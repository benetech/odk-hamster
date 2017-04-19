package org.opendatakit.configuration;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.sql.DataSource;

import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.engine.pgres.DatastoreImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@Profile("unittest")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TestDataConfiguration {

  @Value("${jdbc.driverClassName:org.postgresql.Driver}")
  String driverClassName;

  @Value("${jdbc.url}")
  String url;

  @Value("${jdbc.username}")
  String username;

  @Value("${jdbc.password}")
  String password;

  @Value("${jdbc.schema}")
  String schemaName;

  @Value("10")
  int maxIdle;
  
  @Value("3600")  // Seconds
  int maxIdleTime;
  
  @Value("60")  // Seconds
  int maxIdleTimeExcessConnections;

  @Value("5")
  int minPoolSize;

  @Value("20")
  int maxPoolSize;
  
  @Value("5")
  int acquireIncrement;
  
  @Value("3")
  int acquireRetryAttempts;
  
  @Value("4500") // Milliseconds
  int acquireRetryDelay;

  @Value("select 1")
  String validationQuery;

  /**
   * This is only for functions at the "meta" level that tests have to do i.e. Drop/create database,
   * schema, user. There is no equivalent for production. Also, properties can't be exposed for
   * Spring dependency injection unless you wrap them in a bean, so here you go.
   */
  @Bean(name = "dbAdminProperties")
  public Properties dbAdminProperties() {
    Properties properties = new Properties();
    properties.setProperty("username", username);
    properties.setProperty("schemaName", schemaName);
    return properties;
  }

  @Bean
  public DataSource dataSource() throws PropertyVetoException {
    ComboPooledDataSource comboPooledDataSource = new ComboPooledDataSource();
    comboPooledDataSource.setUser(username);
    comboPooledDataSource.setPassword(password);
    comboPooledDataSource.setJdbcUrl(url);
    comboPooledDataSource.setDriverClass(driverClassName);
    comboPooledDataSource.setMinPoolSize(minPoolSize);
    comboPooledDataSource.setAcquireIncrement(acquireIncrement);
    comboPooledDataSource.setAcquireRetryAttempts(acquireRetryAttempts);
    comboPooledDataSource.setMaxPoolSize(maxPoolSize);
    comboPooledDataSource.setMaxIdleTime(maxIdle);
    comboPooledDataSource.setMaxIdleTimeExcessConnections(maxIdleTimeExcessConnections);
    comboPooledDataSource.setPreferredTestQuery(validationQuery);
    return comboPooledDataSource;
  }

  @Bean(name = "datastore")
  public Datastore datastore() throws ODKDatastoreException, PropertyVetoException {
    DatastoreImpl datastoreImpl = new DatastoreImpl();
    datastoreImpl.setDataSource(dataSource());
    datastoreImpl.setSchemaName(schemaName);
    return datastoreImpl;
  }

}
