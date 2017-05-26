/*
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
package org.opendatakit.configuration;

import java.beans.PropertyVetoException;
import java.util.Properties;

import javax.sql.DataSource;

import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.EphemeralTestDatastoreImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@Profile({"unittest", "integrationtest"})
@ComponentScan(basePackages = {"org.opendatakit"})
public class TestDataConfiguration {

  @Value("${spring.datasource.driverClassName}")
  String driverClassName;

  @Value("${spring.datasource.url}")
  String url;

  @Value("${spring.datasource.username}")
  String username;

  @Value("${spring.datasource.password}")
  String password;
  
  @Value("${use.embedded.postgres}")
  boolean useEmbeddedPostgres;

  @Value("10")
  int maxIdle;

  @Value("3600") // Seconds
  int maxIdleTime;

  @Value("60") // Seconds
  int maxIdleTimeExcessConnections;

  @Value("5")
  int minPoolSize;

  @Value("20")
  int initialPoolSize;

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





  /**
   * This is only for functions at the "meta" level that tests have to do i.e. Drop/create database,
   * schema, user. There is no equivalent for production. Also, properties can't be exposed for
   * Spring dependency injection unless you wrap them in a bean, so here you go.
   */
  @Bean(name = "dbAdminProperties")
  public Properties dbAdminProperties() {
    Properties properties = new Properties();
    properties.setProperty("username", username);
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
    comboPooledDataSource.setInitialPoolSize(initialPoolSize);
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
    EphemeralTestDatastoreImpl datastoreImpl = new EphemeralTestDatastoreImpl();
    datastoreImpl.setUseEmbeddedPostgres(useEmbeddedPostgres);
    datastoreImpl.setUsername(username);
    datastoreImpl.setDataSource(dataSource());
    return datastoreImpl;
  }

  @Bean(name = "transactionManager")
  public DataSourceTransactionManager transactionManager() throws PropertyVetoException {
    DataSourceTransactionManager txManager = new DataSourceTransactionManager();
    txManager.setDataSource(dataSource());
    return txManager;
  }
}
