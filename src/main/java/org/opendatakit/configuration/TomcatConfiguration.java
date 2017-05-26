package org.opendatakit.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.catalina.Context;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The purpose of this configuration class is to get static files to be served and have that
 * working within a Docker container.
 * 
 * The only things that require these static files are the Swagger UI, the friendly 
 * front page, and the favicon.  If none of these things work, the web service still functions.
 * 
 * To do this without installing Spring MVC we're creating a temporary static file directory
 * and configuring the embedded Tomcat instance to use it.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
@Configuration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TomcatConfiguration {

  private static Log logger = LogFactory.getLog(TomcatConfiguration.class);
  private static String STATIC_DIR = "static";

  @Bean
  public EmbeddedServletContainerFactory servletContainer() {
    TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();

    TomcatContextCustomizer staticContentCustomizer = new TomcatContextCustomizer() {
      @Override
      public void customize(Context context) {

        URL staticDirUrl =
            EmbeddedServletContainerFactory.class.getClassLoader().getResource(STATIC_DIR);
        if ("jar".equals(staticDirUrl.getProtocol())) {
          try {
            String staticDirPath = Files.createTempDirectory(STATIC_DIR).toAbsolutePath().toString();
            logger.info("Setting new static file directory path " + staticDirPath);
            copyStaticFiles(staticDirPath);
            context.setDocBase(staticDirPath);

          } catch (IOException e) {
            logger.error(
                "Unable to create temp directory for static files.  That's ok, they're non-essential for web service operation.");
            logger.error(
                "Swagger UI will not be available since it relies on statically served files.");
            logger.error(e);
          }
        }
      }
    };
    tomcat.addContextCustomizers(staticContentCustomizer);
    return tomcat;
  }

  private void copyStaticFiles(String targetPath) throws IOException {
    PathMatchingResourcePatternResolver pathResolver = new PathMatchingResourcePatternResolver();
    Resource[] resources;

    resources = pathResolver.getResources("classpath:static/**/*");
    logger.info("Copying static resources to /tmp directory.");
    for (Resource resource : resources) {

      logger.debug(resource.getURI().toString());
      String uri = resource.getURI().toString();
      if (!uri.endsWith("/")) {
        String newPath =
            targetPath + uri.substring(uri.lastIndexOf(STATIC_DIR) + STATIC_DIR.length());
        File staticFile = new File(newPath);
        // Create directories on demand.
        staticFile.getParentFile().mkdirs();
        logger.info("Copying " + newPath);
        IOUtils.copy(resource.getInputStream(), new FileOutputStream(staticFile));
      }
    }
  }
}
