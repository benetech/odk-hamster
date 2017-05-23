package org.opendatakit.configuration;

import java.io.File;
import java.net.URL;

import org.apache.catalina.Context;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The entire purpose of this configuration class is to get static files to be served 
 * and have that working within a Docker container. 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
@Configuration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TomcatConfiguration {

  private static Log logger = LogFactory.getLog(TomcatConfiguration.class);


  @Bean
  public EmbeddedServletContainerFactory servletContainer() {
    TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
    logger.info("Getting the servlet configuration; sniffing out path locations");
    logger.info("Current document root " + tomcat.getDocumentRoot());
    logger.info(
        "jar location " + this.getClass().getProtectionDomain().getCodeSource().getLocation());
    logger.info("user.dir " + System.getProperty("user.dir"));
    logger.info("Context path " + tomcat.getContextPath());

    TomcatContextCustomizer staticContentCustomizer = new TomcatContextCustomizer() {
      @Override
      public void customize(Context context) {
 
        URL staticDirUrl =
            EmbeddedServletContainerFactory.class.getClassLoader().getResource("static");
        logger.info("static resource folder " + staticDirUrl.toString());
        String docBase = new File(staticDirUrl.getPath()).getPath();
        logger.info("path to static resource folder " + docBase);
        context.setDocBase("/tmp/static");
      }
    };
    tomcat.addContextCustomizers(staticContentCustomizer);
    return tomcat;
  }


}
