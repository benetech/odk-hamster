package org.opendatakit.test.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.postgresql.util.PSQLException;

public class SetupTeardownUtil {
  
  private static final String DROP_SCHEMA_SQL = "DROP SCHEMA IF EXISTS ";
  private static final String CASCADE_SQL = " CASCADE";
  private static final String CREATE_SCHEMA_SQL = "CREATE SCHEMA IF NOT EXISTS ";
  private static final String GRANT_USER_SCHEMA_SQL = "GRANT ALL PRIVILEGES ON SCHEMA ";
  private static final String DBL_QT = "\"";
  private static final String TO = " TO ";
  
  public static void teardownDatabase(DataSource dataSource, String schemaName) throws SQLException {
    Connection conn = dataSource.getConnection();
    Log logger = LogFactory.getLog(SetupTeardownUtil.class);
    logger.info("Dropping schema " + schemaName);
    StringBuilder sql = new StringBuilder(DROP_SCHEMA_SQL);
    sql.append(DBL_QT).append(schemaName).append(DBL_QT).append(CASCADE_SQL);
    executeStatementAndContinue(conn, sql.toString());
  }

  public static void setupEmptyDatabase(DataSource dataSource, String username, String schemaName) throws SQLException {
    // Databases, schemata, tables and postgres usernames cannot be set in parameterized queries
    // Risk of SQL Injection is nil because the parameters are not coming from outside input.
    Connection conn = dataSource.getConnection();
    Log logger = LogFactory.getLog(SetupTeardownUtil.class);
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

  private static void executeStatementOrDie(Connection conn, String sql)
      throws SQLException, PSQLException {
    Log logger = LogFactory.getLog(SetupTeardownUtil.class);
    logger.info("Executing: " + sql);
    Statement s = conn.createStatement();
    s.executeUpdate(sql);
    s.close();
  }

  private static void executeStatementAndContinue(Connection conn, String sql) {
    Log logger = LogFactory.getLog(SetupTeardownUtil.class);
    try {
      executeStatementOrDie(conn, sql.toString());
    } catch (PSQLException e) {
      logger.info("Failed: " + e.getMessage());
    } catch (SQLException e) {
      logger.info("Failed: " + e.getMessage());
    }
  }
}
