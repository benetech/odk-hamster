package org.opendatakit.configuration;

import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.security.Realm;
import org.opendatakit.common.security.UserService;
import org.opendatakit.common.security.spring.UserServiceImpl;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.context.TestCallingContextImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("unittest")
@ComponentScan(basePackages = {"org.opendatakit", "org.benetech"})
public class TestUserServiceConfiguration {

  @Autowired
  @Qualifier("datastore")
  Datastore datastore;

  @Value("${security.server.realm.realmString:opendatakit.org RC1 Aggregate 1.0 realm}")
  private String realmString;

  @Value("${security.server.hostname:localhost}")
  private String hostname;

  @Value("${security.server.port:8888}")
  private int port;

  @Value("${security.server.securePort:8443}")
  private int securePort;

  @Value("${security.server.channelType:ANY_CHANNEL}")
  private String channelType;

  @Value("${security.server.secureChannelType:ANY_CHANNEL}")
  private String secureChannelType;

  @Value("${external.root.url:http://localhost:8888}")
  private String changePasswordUrl;

  @Value("${security.server.superUser:mailto:cadenh@benetech.org}")
  private String superUserEmail;

  @Value("${security.server.superUserUsername:aggregate}")
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
  public UserService userService() {
    UserServiceImpl userServiceImpl = new UserServiceImpl();
    userServiceImpl.setDatastore(datastore);
    userServiceImpl.setRealm(realm());
    userServiceImpl.setSuperUserEmail(superUserEmail);
    userServiceImpl.setSuperUserUsername(superUserUsername);
    return userServiceImpl;
  }

  @Bean
  public CallingContext callingContext() {
    TestCallingContextImpl callingContextImpl = new TestCallingContextImpl();
    callingContextImpl.setUserService(userService());
    callingContextImpl.setDatastore(datastore);
    callingContextImpl.setServerUrl("http://" + hostname + ":" + port + webApplicationBase);
    callingContextImpl
        .setSecureServerUrl("https://" + hostname + ":" + securePort + webApplicationBase);
    callingContextImpl.setWebApplicationBase(webApplicationBase);
    callingContextImpl.setAsDaemon(true);
    return callingContextImpl;
  }

}
