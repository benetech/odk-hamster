/**
 * This file added by Benetech.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.configuration.UserServiceConfiguration;
import org.postgresql.util.PSQLException;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * 
 * 
 * How this class evolved: 
 * 1. A set of sql files was called directly by Maven in the original ODK
 * aggregate which set up and tore down the database before and after tests. 
 * 2. This being one of
 * our first exposures to Java Configuration files in Spring, we really liked the advantages of
 * having configuration in real code as much as possible, and adding SQL stub files became less
 * attractive. 
 * 3. We had the goal of moving control of database resets to the test classes, similar
 * to how we do in the Bookshare project. 
 * 4. The first attempt of using \@BeforeClass with JUnit
 * failed because JUnit requires BeforeClass methods to be static, and Spring can't do dependency
 * injection into static methods.
 * http://stackoverflow.com/questions/1052577/why-must-junits-fixturesetup-be-static 
 * 5. We tried
 * using PostConstruct and PreDestroy annotations instead of BeforeClass and AfterClass, but they
 * did not execute in the order that we expected when we used them in parent classes and extended
 * those classes. http://www.mkyong.com/spring/spring-postconstruct-and-predestroy-example/ 
 * 6. We
 * moved on to the Spring solution of implementing a TestExecutionListener interface. This was
 * initally extremely frustrating, because for some reason, injecting data into the
 * TestExecutionListener itself does not seem to work, and you need to access beans via the
 * TestContext.
 * http://stackoverflow.com/questions/42204840/spring-dependency-injection-into-spring-testexecutionlisteners-not-working
 * 7. We did discover DbUnitTestExecutionListener intended to run SQL setup and teardown scripts but
 * at this point we had lost some faith in shiny new Spring features and wanted to get on with
 * development. This may be something to explore in the future. 
 * 8. It turns out that you cannot do
 * parameterized queries to create databases, schemas, or database users, so to our disgust we had
 * to use string manipulation to add these. 
 * 9. It turns out that you cannot use JDBC to add a schema
 * to a newly created database that you are not connected to, so we had to add an entire data source
 * definition just to set up a new schema.
 * 10. C3P0 threw up a ton of errors if we deleted a database associated with a defined data source
 * even if that data source was not being used.  So we are now resetting the schema instead of the 
 * entire database.
 * 
 * It will be no surprise to me at all if this is torn down in the future and replaced with .sql
 * stub scripts. In the meantime, it is nice not to have to worry about where to put SQL files in
 * the classpath.
 */
@ContextConfiguration(classes = {TestDataConfiguration.class},initializers = ConfigFileApplicationContextInitializer.class)
@ActiveProfiles("unittest")
public class SetupTeardown extends AbstractTestExecutionListener {

  private static Log logger = LogFactory.getLog(SetupTeardown.class);

  /**
   * Data sources.
   */

  private DataSource dataSource;

  /**
   * Values for the database we're resetting.
   */
  private String schemaName;
  private String username;

  // Drops connections to a database in Postgres.
  // Prevents "Can't drop because people are connected" errors.
  private static final String KILL_CONN_SQL = "SELECT pg_terminate_backend(pg_stat_activity.pid) "
      + "FROM pg_stat_activity WHERE pg_stat_activity.datname = ?";
  private static final String DROP_SCHEMA_SQL = "DROP SCHEMA IF EXISTS ";
  private static final String CASCADE_SQL = " CASCADE";
  private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA ";
  private static final String GRANT_USER_SCHEMA_SQL = "GRANT ALL PRIVILEGES ON SCHEMA ";
  private static final String DBL_QT = "\"";
  private static final String SGL_QT = "'";
  private static final String TO = " TO ";


  @Override
  public void beforeTestClass(TestContext testContext) throws Exception {
    dataSource = testContext.getApplicationContext().getBean("dataSource", DataSource.class);
    getProperties(testContext);
    teardownDatabase();
    setupEmptyDatabase();
  }

  @Override
  public void afterTestClass(TestContext testContext) throws Exception {
    getProperties(testContext);
    dataSource = testContext.getApplicationContext().getBean("dataSource", DataSource.class);

    // teardownDatabase(conn);
  }

  private void getProperties(TestContext testContext) {
    Properties properties =
        testContext.getApplicationContext().getBean("dbAdminProperties", Properties.class);
    username = properties.getProperty("username");
    schemaName = properties.getProperty("schemaName");
  }

  private void teardownDatabase() throws SQLException {
    Connection conn = dataSource.getConnection();
    
    logger.info("Dropping schema " + schemaName);
    StringBuilder sql = new StringBuilder(DROP_SCHEMA_SQL);
    sql.append(DBL_QT).append(schemaName).append(DBL_QT).append(CASCADE_SQL);
    executeStatementAndContinue(conn, sql.toString());
  }

  private void setupEmptyDatabase() throws SQLException {
    // Databases, schemata, tables and usernames cannot be set in parameterized queries
    // Risk of SQL Injection is nil because the parameters are not coming from outside input.
    // ...And now you know why most people inject a (native) SQL script for setup and teardown.

    Connection conn = dataSource.getConnection();

    logger.info("Creating schema " + schemaName);
    StringBuilder sql = new StringBuilder(CREATE_SCHEMA_SQL);
    sql.append(DBL_QT).append(schemaName).append(DBL_QT);
    executeStatementOrDie(conn, sql.toString());

    logger.info("Granting user " + username + " permission on schema " + schemaName);
    sql = new StringBuilder(GRANT_USER_SCHEMA_SQL);
    sql.append(DBL_QT).append(schemaName).append(DBL_QT).append(TO).append(DBL_QT).append(username)
        .append(DBL_QT);
    executeStatementOrDie(conn, sql.toString());
  }

  private void executeStatementOrDie(Connection conn, String sql)
      throws SQLException, PSQLException {
    logger.info("Executing: " + sql);
    Statement s = conn.createStatement();
    s.executeUpdate(sql);
    s.close();
  }

  private void executeStatementAndContinue(Connection conn, String sql) {
    try {
      executeStatementOrDie(conn, sql.toString());
    } catch (PSQLException e) {
      logger.info("Failed: " + e.getMessage());
    } catch (SQLException e) {
      logger.info("Failed: " + e.getMessage());
    }
  }

}
