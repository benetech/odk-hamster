package org.opendatakit.test.util;

import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;

public class ConstantsUtils {
  public static final String API_VERSION_PARAMETER = "2";
  public static final String APP_ID_PARAMETER = "default";
  
  public static final String TEST_ADMIN_USERNAME = "admin";
  public static final String TEST_ADMIN_PASSWORD = "aggregate";
  
  public static final String url( EmbeddedWebApplicationContext server) {
	  return  "http://localhost:" + server.getEmbeddedServletContainer().getPort();
  }
}
