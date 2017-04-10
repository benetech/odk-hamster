package org.benetech.boot;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

//@Configuration
//@EnableAutoConfiguration
@Profile("default")
//@ComponentScan(basePackages = {"org.opendatakit","org.benetech"})

@SpringBootApplication(scanBasePackages = {"org.opendatakit","org.benetech"})
public class Application extends SpringBootServletInitializer{
  private static Log logger = LogFactory.getLog(Application.class);


  public static void main(String[] args) {
    //SpringApplication.run(Application.class, args);
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
