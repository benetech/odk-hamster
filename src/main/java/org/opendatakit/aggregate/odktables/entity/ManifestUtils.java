package org.opendatakit.aggregate.odktables.entity;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class ManifestUtils {

  public static String fixInternalUrl(Environment env, URI uri)
      throws URISyntaxException, MalformedURLException {
    URI fixedUri = uri;
    Log log = LogFactory.getLog(ManifestUtils.class);

    if (env != null) {
      String externalUrlString = env.getProperty("external.root.url");
      log.error("externalUrlString " + externalUrlString);

      URI externalUri = new URI(externalUrlString);
      if (StringUtils.isNotBlank(externalUrlString) && !equivalentBaseURI(uri, externalUri)) {
        fixedUri = targetURI(externalUri, uri);
      }
    }
    else {
      log.error("Environment is null!");
    }
    return fixedUri.toURL().toExternalForm();
  }
  
  
  public static String fixInternalUrl(String externalUrlString, String uri) throws URISyntaxException, MalformedURLException  {
    return fixInternalUrl(externalUrlString, new URI(uri));
  }
 
  public static String fixInternalUrl(String externalUrlString, URI uri)
      throws URISyntaxException, MalformedURLException {
    URI fixedUri = uri;
    Log log = LogFactory.getLog(ManifestUtils.class);

    if (StringUtils.isNotEmpty(externalUrlString)) {
      URI externalUri = new URI(externalUrlString);

      if (StringUtils.isNotBlank(externalUrlString) && !equivalentBaseURI(uri, externalUri)) {
        fixedUri = targetURI(externalUri, uri);
      }
      log.error("updating " + uri.toString() + " with externalUrlString " + externalUrlString + " to " + fixedUri.toString());

    }
    else {
      log.error("externalUrlString is empty or null!");
    }
    return fixedUri.toURL().toExternalForm();
  }

  private static URI targetURI(URI targetBase, URI targetPath) throws URISyntaxException {
    return new URI(targetBase.getScheme(), targetPath.getUserInfo(), targetBase.getHost(),
        targetBase.getPort(), targetPath.getPath(), targetPath.getQuery(),
        targetPath.getFragment());
  }

  private static boolean equivalentBaseURI(URI uri1, URI uri2) {
    uri1 = uri1.normalize();
    uri2 = uri2.normalize();
    return StringUtils.equals(uri1.getScheme(), uri2.getScheme())
        && StringUtils.equals(uri1.getHost(), uri2.getHost()) && uri1.getPort() == uri2.getPort();
  }

}
