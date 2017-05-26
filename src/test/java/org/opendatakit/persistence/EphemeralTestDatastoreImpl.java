package org.opendatakit.persistence;

import static java.util.Collections.emptyList;
import static ru.yandex.qatools.embed.postgresql.EmbeddedPostgres.cachedRuntimeConfig;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.persistence.engine.pgres.DatastoreImpl;
import org.opendatakit.persistence.engine.pgres.TaskLockImpl.TaskLockTable;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.table.GrantedAuthorityHierarchyTable;
import org.opendatakit.persistence.table.OdkRegionalOfficeTable;
import org.opendatakit.persistence.table.OdkTablesUserInfoTable;
import org.opendatakit.persistence.table.RegisteredUsersTable;
import org.opendatakit.persistence.table.SecurityRevisionsTable;
import org.opendatakit.persistence.table.ServerPreferencesPropertiesTable;
import org.opendatakit.persistence.table.UserGrantedAuthority;
import org.opendatakit.test.db.SetupTeardownUtil;
import org.springframework.beans.factory.DisposableBean;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.flapdoodle.embed.process.config.IRuntimeConfig;
import ru.yandex.qatools.embed.postgresql.EmbeddedPostgres;

/**
 * Creates a unique schema so that each test can be isolated while sharing the same Postgres
 * database.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
public class EphemeralTestDatastoreImpl extends DatastoreImpl implements DisposableBean {

  private String ephemeralSchemaName = null;
  private DataSource dataSource;
  private String username;
  private boolean useEmbeddedPostgres;
  private EmbeddedPostgres postgres;


  public EphemeralTestDatastoreImpl() throws ODKDatastoreException {
    super();
  }

  private String getEphemeralSchemaName() {
    if (ephemeralSchemaName == null) {
      String uuid = UUID.randomUUID().toString();
      uuid = uuid.substring(uuid.length() - 6);
      ephemeralSchemaName = "hamster_unit_" + uuid;
      Log logger = LogFactory.getLog(TestDataConfiguration.class);
      logger.info("Setting schema name to " + ephemeralSchemaName);
    }
    return ephemeralSchemaName;
  }


  public void setUsername(String username) {
    this.username = username;
  }


  @Override
  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
    super.setDataSource(dataSource);
  }

  @Override
  public void afterPropertiesSet() throws Exception {

    if (useEmbeddedPostgres) {
      try {
        postgres = new EmbeddedPostgres();
        IRuntimeConfig runtimeConfig =
            cachedRuntimeConfig(Paths.get(System.getProperty("java.io.tmpdir"), "pgembed"));
        String url = postgres.start(runtimeConfig, "localhost", 15433, "hamster_unit", username,
            "not the real password", emptyList());
        ((ComboPooledDataSource) dataSource).setJdbcUrl(url);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Even with @DirtiesContext, the Spring holds on to these static references because they are
    // not managed by Spring.
    // These objects should be refactored to be managed by Spring.
    GrantedAuthorityHierarchyTable.resetSingletonReference();
    OdkRegionalOfficeTable.resetSingletonReference();
    OdkTablesUserInfoTable.resetSingletonReference();
    RegisteredUsersTable.resetSingletonReference();
    SecurityRevisionsTable.resetSingletonReference();
    ServerPreferencesPropertiesTable.resetSingletonReference();
    UserGrantedAuthority.resetSingletonReference();
    TaskLockTable.resetSingletonReference();

    super.setSchemaName(getEphemeralSchemaName());
    SetupTeardownUtil.setupEmptyDatabase(this.dataSource, this.username, getEphemeralSchemaName());
    super.afterPropertiesSet();
  }

  @Override
  public void destroy() throws Exception {
    SetupTeardownUtil.teardownDatabase(this.dataSource, getEphemeralSchemaName());

  }


  public void setUseEmbeddedPostgres(boolean useEmbeddedPostgres) {
    this.useEmbeddedPostgres = useEmbeddedPostgres;
  }




}
