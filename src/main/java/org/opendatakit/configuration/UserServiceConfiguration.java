package org.opendatakit.configuration;

import java.beans.PropertyVetoException;

import org.opendatakit.context.CallingContext;
import org.opendatakit.context.CallingContextImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.security.Realm;
import org.opendatakit.security.UserService;
import org.opendatakit.security.spring.RoleHierarchyImpl;
import org.opendatakit.security.spring.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

@Configuration
@Profile("default")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class UserServiceConfiguration {

  @Autowired
  DataConfiguration dataConfiguration;

  @Value("/")
  private String servletPath;

  @Value("${security.server.realm.realmString:opendatakit.org ODK Aggregate}")
  private String realmString;

  @Value("${security.server.hostname:localhost}")
  private String hostname;

  @Value("${security.server.port:8080}")
  private int port;

  @Value("${security.server.securePort:8443}")
  private int securePort;

  @Value("${security.server.channelType:ANY_CHANNEL}")
  private String channelType;

  @Value("${security.server.secureChannelType:ANY_CHANNEL}")
  private String secureChannelType;

  @Value("${external.root.url:http://localhost:8080}")
  private String changePasswordUrl;

  @Value("${security.server.superUserUsername:admin}")
  private String superUserUsername;

  @Value("")
  private String webApplicationBase;

  @Bean
  public Realm realm() {
    Realm realm = new Realm();
    realm.setRealmString(realmString);
    realm.setHostname(hostname);
    realm.setPort(port);
    realm.setSecurePort(securePort);
    realm.setChannelType(channelType);
    realm.setSecureChannelType(secureChannelType);
    realm.setChangePasswordURL(changePasswordUrl);
    return realm;
  }

  @Bean
  public UserService userService() throws ODKDatastoreException, PropertyVetoException {
    UserServiceImpl userServiceImpl = new UserServiceImpl();
    userServiceImpl.setRealm(realm());
    userServiceImpl.setDatastore(dataConfiguration.datastore());
    userServiceImpl.setSuperUserUsername(superUserUsername);
    userServiceImpl
        .setMessageDigestPasswordEncoder(basicAuthenticationMessageDigestPasswordEncoder());
    return userServiceImpl;
  }


  // The Basic Authentication passwords are sha1-encoded with a salt
  @Bean
  public MessageDigestPasswordEncoder basicAuthenticationMessageDigestPasswordEncoder() {
    return new ShaPasswordEncoder();
  }

  @Bean
  public RoleHierarchy hierarchicalRoleRelationships()
      throws ODKDatastoreException, PropertyVetoException {
    RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
    roleHierarchyImpl.setDatastore(dataConfiguration.datastore());
    roleHierarchyImpl.setUserService(userService());
    roleHierarchyImpl.setPasswordEncoder(basicAuthenticationMessageDigestPasswordEncoder());
    return roleHierarchyImpl;
  }

  @Bean
  CallingContext callingContext() throws ODKDatastoreException, PropertyVetoException {
    CallingContextImpl callingContextImpl = new CallingContextImpl(dataConfiguration.datastore(),
        userService(), hierarchicalRoleRelationships(),
        basicAuthenticationMessageDigestPasswordEncoder(), servletPath, false);
    return callingContextImpl;
  }

}
