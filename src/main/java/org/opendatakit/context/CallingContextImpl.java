package org.opendatakit.context;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.security.Realm;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.UserService;
import org.opendatakit.common.security.spring.UserServiceImpl;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.ServletConsts;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;

public class CallingContextImpl implements CallingContext {
  String serverUrl;
  String secureServerUrl;
  String webApplicationBase;

  Datastore datastore;

  UserService userService;
  RoleHierarchy hierarchicalRoleRelationships;
  MessageDigestPasswordEncoder messageDigestPasswordEncoder;

  boolean asDaemon = false;

  private static final Log logger = LogFactory.getLog(CallingContextImpl.class);


  public CallingContextImpl(Datastore datastore, UserService userService,
      RoleHierarchy roleHierarchy, MessageDigestPasswordEncoder messageDigestPasswordEncoder,
      String path, boolean asDaemon) {

    path = path == null ? "" : path;
    this.datastore = datastore;
    this.userService = userService;
    this.asDaemon = asDaemon;
    this.hierarchicalRoleRelationships = roleHierarchy;

    Realm realm = userService.getCurrentRealm();
    Integer identifiedSecurePort = inferSecurePort(realm);

    Integer identifiedPort = inferPort(realm, identifiedSecurePort);
    String identifiedHostname = inferHostname(realm);
    String identifiedScheme = realm.isSslRequired() ? "https" : "http";


    boolean expectedPort =
        (identifiedScheme.equalsIgnoreCase("http") && identifiedPort == ServletConsts.WEB_PORT)
            || (identifiedScheme.equalsIgnoreCase("https")
                && identifiedPort == ServletConsts.SECURE_WEB_PORT);

    if (!expectedPort) {
      serverUrl = identifiedScheme + "://" + identifiedHostname + BasicConsts.COLON
          + Integer.toString(identifiedPort) + path;
    } else {
      serverUrl = identifiedScheme + "://" + identifiedHostname + path;
    }

    if (realm.isSslRequired() || !realm.isSslAvailable()) {
      secureServerUrl = serverUrl;
    } else {
      if (identifiedSecurePort != null && identifiedSecurePort != 0
          && identifiedSecurePort != ServletConsts.SECURE_WEB_PORT) {
        // explicitly name the port
        secureServerUrl = "https://" + identifiedHostname + BasicConsts.COLON
            + Integer.toString(identifiedSecurePort) + path;
      } else {
        // assume it is the default https port...
        secureServerUrl = "https://" + identifiedHostname + path;
      }
    }
    webApplicationBase = path;
  }


  static int inferPort(Realm realm, Integer securePort) {
    Integer identifiedPort = realm.getPort();

    if (identifiedPort == null || identifiedPort == 0) {
      identifiedPort = ServletConsts.WEB_PORT;
    }

    if (realm.isSslRequired()) {
      identifiedPort = securePort;
    }

    return identifiedPort;
  }


  static int inferSecurePort(Realm realm) {
    Integer identifiedSecurePort = realm.getSecurePort();
    if (identifiedSecurePort == null || identifiedSecurePort == 0) {
      identifiedSecurePort = ServletConsts.SECURE_WEB_PORT;
    }
    return identifiedSecurePort;
  }

  static String inferHostname(Realm realm) {

    String identifiedHostname = realm.getHostname();

    if (identifiedHostname == null || identifiedHostname.length() == 0) {

      try {
        identifiedHostname = InetAddress.getLocalHost().getCanonicalHostName();
      } catch (UnknownHostException e) {
        identifiedHostname = "127.0.0.1";
      }

    }
    return identifiedHostname;
  }

  public CallingContextImpl(CallingContext context) {
    this.serverUrl = context.getServerURL();
    this.secureServerUrl = context.getSecureServerURL();
    this.webApplicationBase = context.getWebApplicationURL();
    this.datastore = context.getDatastore();
    this.userService = context.getUserService();
    this.asDaemon = context.getAsDaemon();
    this.hierarchicalRoleRelationships = context.getHierarchicalRoleRelationships();
  }


  // @Override
  // public Object getBean(String beanName) {
  // return null;
  // }

  @Override
  public Datastore getDatastore() {
    return datastore;
  }

  @Override
  public UserService getUserService() {
    return userService;
  }

  @Override
  public void setAsDaemon(boolean asDaemon) {
    this.asDaemon = asDaemon;
  }

  @Override
  public boolean getAsDaemon() {
    return asDaemon;
  }

  @Override
  public User getCurrentUser() {
    User currentUser = null;
    try {
      currentUser = userService.getCurrentUser();
    } catch (NullPointerException e) {
      logger.info("Can't get current user.  Possibly no security context is set yet, normal at startup.");
    }
    return asDaemon || currentUser == null ? userService.getDaemonAccountUser() : currentUser;
  }


  @Override
  public String getWebApplicationURL() {
    return webApplicationBase;
  }

  @Override
  public String getWebApplicationURL(String servletAddr) {
    return webApplicationBase + BasicConsts.FORWARDSLASH + servletAddr;

  }

  @Override
  public String getServerURL() {
    return serverUrl;
  }

  @Override
  public String getSecureServerURL() {
    return getSecureServerURL();
  }


  @Override
  public RoleHierarchy getHierarchicalRoleRelationships() {
    return hierarchicalRoleRelationships;
  }

  @Override
  public MessageDigestPasswordEncoder getMessageDigestPasswordEncoder() {
    return messageDigestPasswordEncoder;
  }

  public String getServerUrl() {
    return serverUrl;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  public String getSecureServerUrl() {
    return secureServerUrl;
  }

  public void setSecureServerUrl(String secureServerUrl) {
    this.secureServerUrl = secureServerUrl;
  }

  public String getWebApplicationBase() {
    return webApplicationBase;
  }

  public void setWebApplicationBase(String webApplicationBase) {
    this.webApplicationBase = webApplicationBase;
  }

  public void setDatastore(Datastore datastore) {
    this.datastore = datastore;
  }

  public void setUserService(UserService userService) {
    this.userService = userService;
  }



  public void setHierarchicalRoleRelationships(RoleHierarchy roleHierarchy) {
    this.hierarchicalRoleRelationships = roleHierarchy;
  }



}
