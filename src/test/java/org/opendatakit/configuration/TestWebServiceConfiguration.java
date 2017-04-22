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
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.opendatakit.test.db.SetupTeardown;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("unittest")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TestWebServiceConfiguration {

  @Autowired
  TestDataConfiguration testDataConfiguration;

  @Bean
  public EmbeddedServletContainerFactory servletContainer() throws SQLException, PropertyVetoException {
    // Reset the database before we set up the container
    // This isn't where we should put this but @PostConstruct and @BeforeClass annotations,
    // HIGHEST_PRECEDENCE ordered TestExecutionListeners have all failed to run this code
    // before the Tomcat server is started, causing much sadness.

    String username = testDataConfiguration.dbAdminProperties().getProperty("username");
    String schemaName = testDataConfiguration.dbAdminProperties().getProperty("schemaName");
    SetupTeardown.teardownDatabase(testDataConfiguration.dataSource(), username, schemaName);
    SetupTeardown.setupEmptyDatabase(testDataConfiguration.dataSource(), username, schemaName);

    TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
    factory.setPort(25001);
    factory.setSessionTimeout(10, TimeUnit.MINUTES);
    return factory;
  }
}
