/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.engine.pgres.DatastoreImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.mchange.v2.c3p0.ComboPooledDataSource;

@Configuration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit","org.benetech"})
public class DataConfiguration {
  
  private static final Log logger = LogFactory.getLog(DataConfiguration.class);

  @Value("${jdbc.driverClassName:org.postgresql.Driver}")
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

  @Primary
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
    DatastoreImpl datastoreImpl = new DatastoreImpl();
    datastoreImpl.setDataSource(dataSource());
    datastoreImpl.setSchemaName(schemaName);
    return datastoreImpl;
  }
  
  @Bean(name = "transactionManager")
  public DataSourceTransactionManager transactionManager() throws PropertyVetoException {
    DataSourceTransactionManager txManager = new DataSourceTransactionManager();
    txManager.setDataSource(dataSource());
    return txManager;
  }
  

}
