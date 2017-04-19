package org.opendatakit.configuration;

import java.beans.PropertyVetoException;

import org.opendatakit.context.CallingContext;
import org.opendatakit.context.CallingContextImpl;
import org.opendatakit.context.TestCallingContextImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.security.Realm;
import org.opendatakit.security.UserService;
import org.opendatakit.security.spring.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

@Configuration
@Profile("unittest")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TestUserServiceConfiguration {
 
  @Autowired
  TestDataConfiguration testDataConfiguration;

  @Value("${security.server.realm.realmString:opendatakit.org ODK Aggregate}")
  private String realmString;

  @Value("${security.server.hostname:localhost}")
  private String hostname;

  @Value("${security.server.superUserUsername:admin}")
  private String superUserUsername;

  @Value("")
  private String webApplicationBase;

  @Bean
  public Realm realm() {
    Realm realm = new Realm();
    realm.setRealmString(realmString);
    realm.setHostname(hostname);
    return realm;
  }

  @Bean
  public UserService userService() throws ODKDatastoreException, PropertyVetoException {
    UserServiceImpl userServiceImpl = new UserServiceImpl();
    userServiceImpl.setRealm(realm());
    userServiceImpl.setDatastore(testDataConfiguration.datastore());
    userServiceImpl.setSuperUserUsername(superUserUsername);
    return userServiceImpl;
  }

  @Bean
  public CallingContext callingContext() throws ODKDatastoreException, PropertyVetoException {
    TestCallingContextImpl callingContextImpl = new TestCallingContextImpl();
    callingContextImpl.setUserService(userService());
    callingContextImpl.setDatastore(testDataConfiguration.datastore());
    callingContextImpl.setWebApplicationBase(webApplicationBase);
    callingContextImpl.setAsDaemon(true);
    return callingContextImpl;
  }
  
  @Bean 
  public MessageDigestPasswordEncoder passwordEncoder() {
    return new ShaPasswordEncoder();
  }

}
