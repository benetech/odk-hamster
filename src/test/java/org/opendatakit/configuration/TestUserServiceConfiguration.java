/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.configuration;

import java.beans.PropertyVetoException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;

@Configuration
@Profile({"unittest", "integrationtest"})
public class TestUserServiceConfiguration {
 
  @Autowired
  TestDataConfiguration testDataConfiguration;

  @Value("/")
  private String servletPath;
  
  @Value("${security.server.realm.realmString:opendatakit.org ODK 2.0 Server}")
  private String realmString;

  @Value("${security.server.hostname:localhost}")
  private String hostname;

  @Value("${security.server.superUserUsername:admin}")
  private String superUserUsername;

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
  public RoleHierarchy hierarchicalRoleRelationships()
      throws ODKDatastoreException, PropertyVetoException {
    Log logger = LogFactory.getLog(TestUserServiceConfiguration.class);
    logger.info("Setting up hierarchicalRoleRelationships");
    RoleHierarchyImpl roleHierarchyImpl = new RoleHierarchyImpl();
    roleHierarchyImpl.setDatastore(testDataConfiguration.datastore());
    roleHierarchyImpl.setUserService(userService());
    return roleHierarchyImpl;
  }
  
  
  @Bean
  public CallingContext callingContext() throws ODKDatastoreException, PropertyVetoException {
    CallingContextImpl callingContextImpl = new CallingContextImpl(testDataConfiguration.datastore(),
        userService(), hierarchicalRoleRelationships(),
        servletPath, false);
    return callingContextImpl;
  }
  

}
