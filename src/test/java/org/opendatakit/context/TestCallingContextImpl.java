package org.opendatakit.context;

import javax.servlet.ServletContext;

import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.UserService;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

public class TestCallingContextImpl implements CallingContext {
  String serverUrl;
  String secureServerUrl;
  String webApplicationBase;
  ServletContext servletContext;
  Datastore datastore;
  UserService userService;
  boolean asDaemon = false;

  @Override
  public Object getBean(String beanName) {
    return null;
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
    return asDaemon ? userService.getDaemonAccountUser() : userService.getCurrentUser();
  }

  @Override
  public ServletContext getServletContext() {
    return servletContext;
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

  public void setServletContext(ServletContext servletContext) {
    this.servletContext = servletContext;
  }

  public void setDatastore(Datastore datastore) {
    this.datastore = datastore;
  }

  public void setUserService(UserService userService) {
    this.userService = userService;
  }
  
  

}
