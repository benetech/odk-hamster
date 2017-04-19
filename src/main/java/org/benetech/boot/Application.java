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
package org.benetech.boot;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Profile("default")
@SpringBootApplication(scanBasePackages = {"org.opendatakit","org.benetech"})
public class Application extends SpringBootServletInitializer{
  private static Log logger = LogFactory.getLog(Application.class);


  public static void main(String[] args) {
    new Application().configure(new SpringApplicationBuilder(Application.class)).run(args);
  }

  @Bean
  protected ServletContextListener listener() {
    return new ServletContextListener() {

      @Override
      public void contextDestroyed(ServletContextEvent arg0) {
        logger.info("ServletContext destroyed");

      }

      @Override
      public void contextInitialized(ServletContextEvent arg0) {
        logger.info("ServletContext initialized");

      }

    };
  }


}
