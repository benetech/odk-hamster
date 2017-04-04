package org.opendatakit.configuration;

import java.beans.PropertyVetoException;

import javax.sql.DataSource;

import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.engine.pgres.DatastoreImpl;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit.common","org.benetech"})
public class DataConfiguration {
  
  @Value("${jdbc.driverClassName:'org.postgresql.Driver'}")
  String driverClassName;
  
  @Value("${jdbc.url}")
  String url;
  
  @Value("${jdbc.username}")
  String username;
  
  @Value("${jdbc.password}")
  String password;
  
  @Value("${jdbc.schema}")
  public String schemaName;
  

  @Value("10")
  int maxIdle;
  
  @Value("3600")  // Seconds
  int maxIdleTime;
  
  @Value("60")  // Seconds
  int maxIdleTimeExcessConnections;

  @Value("5")
  int minPoolSize;

  @Value("100")
  int maxPoolSize;
  
  @Value("5")
  int acquireIncrement;
  
  @Value("3")
  int acquireRetryAttempts;
  
  @Value("4500") // Milliseconds
  int acquireRetryDelay;

  @Value("select 1")
  String validationQuery;
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

  @Bean
  public Datastore datastore() throws ODKDatastoreException, PropertyVetoException {
    DatastoreImpl datastoreImpl = new DatastoreImpl();
    datastoreImpl.setDataSource(dataSource());
    datastoreImpl.setSchemaName(schemaName);
    return datastoreImpl;
  }

}
