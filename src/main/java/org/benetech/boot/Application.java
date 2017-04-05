package org.benetech.boot;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableAutoConfiguration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit","org.benetech"})
public class Application {
  private static Log logger = LogFactory.getLog(Application.class);


  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
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
