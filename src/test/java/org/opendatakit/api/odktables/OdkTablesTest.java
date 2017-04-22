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
package org.opendatakit.api.odktables;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.configuration.annotations.WebServiceUnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

/**
 *
 * Test the root entry point for Jersey web service calls.
 * Configured to run with authentication disabled.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 */
@RunWith(SpringRunner.class)
@WebServiceUnitTestConfig
public class OdkTablesTest{
  
  private static Log logger = LogFactory.getLog(OdkTablesTest.class);

  @Autowired
  private TestRestTemplate restTemplate;
  
  @Autowired
  private EmbeddedWebApplicationContext server;

  @Test
  public void contextLoads() {
      ResponseEntity<String> entity = this.restTemplate.getForEntity("http://localhost:"
          + server.getEmbeddedServletContainer().getPort() + "/odktables",
              String.class);
      logger.info(entity.toString());
      logger.info(entity.getBody());
      assertThat(entity.getBody()).isEqualTo("[\"default\"]");
      assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }
}
