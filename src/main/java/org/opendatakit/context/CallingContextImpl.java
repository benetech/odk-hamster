package org.opendatakit.context;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ServletConsts;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.security.Realm;
import org.opendatakit.security.User;
import org.opendatakit.security.UserService;
import org.opendatakit.security.spring.UserServiceImpl;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CallingContextImpl implements CallingContext {

  private String webApplicationBase;

  private Datastore datastore;

  private UserService userService;
  private RoleHierarchy hierarchicalRoleRelationships;
  private MessageDigestPasswordEncoder messageDigestPasswordEncoder;

  private boolean asDaemon = false;

  private static final Log logger = LogFactory.getLog(CallingContextImpl.class);


  public CallingContextImpl(Datastore datastore, UserService userService,
      RoleHierarchy roleHierarchy, MessageDigestPasswordEncoder messageDigestPasswordEncoder,
      String path, boolean asDaemon) {

    path = path == null ? "" : path;
    this.datastore = datastore;
    this.userService = userService;
    this.asDaemon = asDaemon;
    this.hierarchicalRoleRelationships = roleHierarchy;

    webApplicationBase = path;
  }


  public CallingContextImpl(CallingContext context) {

    this.webApplicationBase = context.getWebApplicationURL();
    this.datastore = context.getDatastore();
    this.userService = context.getUserService();
    this.asDaemon = context.getAsDaemon();
    this.hierarchicalRoleRelationships = context.getHierarchicalRoleRelationships();
  }

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
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
      currentUser = userService.getCurrentUser();
    } else {
      logger.info(
          "Can't get current user from userService.  No security context is set yet, normal at startup.  Using Daemon account user.");
      currentUser = userService.getDaemonAccountUser();
    }
    return currentUser;
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
  public RoleHierarchy getHierarchicalRoleRelationships() {
    return hierarchicalRoleRelationships;
  }

  @Override
  public MessageDigestPasswordEncoder getMessageDigestPasswordEncoder() {
    return messageDigestPasswordEncoder;
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
