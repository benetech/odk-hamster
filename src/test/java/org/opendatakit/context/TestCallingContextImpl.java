package org.opendatakit.context;

import org.opendatakit.constants.BasicConsts;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.security.User;
import org.opendatakit.security.UserService;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;

public class TestCallingContextImpl implements CallingContext {
  String serverUrl;
  String secureServerUrl;
  String webApplicationBase;
  Datastore datastore;
  UserService userService;
  RoleHierarchy hierarchicalRoleRelationships;

  boolean asDaemon = false;


  @Override
  public Datastore getDatastore() {
    return datastore;
  }

  @Override
  public UserService getUserService() {
    return userService;
  }

  @Override
  public RoleHierarchy getHierarchicalRoleRelationships() {
    return hierarchicalRoleRelationships;
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
    return asDaemon ? userService.getDaemonAccountUser() : userService.getCurrentUser();
  }

  @Override
  public String getWebApplicationURL() {
    return webApplicationBase;
  }

  @Override
  public String getWebApplicationURL(String servletAddr) {
    return webApplicationBase + BasicConsts.FORWARDSLASH + servletAddr;

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

  public void setHierarchicalRoleRelationships(RoleHierarchy hierarchicalRoleRelationships) {
    this.hierarchicalRoleRelationships = hierarchicalRoleRelationships;
  }

}
