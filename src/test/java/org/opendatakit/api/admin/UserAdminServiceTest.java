/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.api.admin;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.configuration.annotations.WebServiceUnitTestConfig;
import org.opendatakit.constants.SecurityConsts;
import org.opendatakit.test.ConstantsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@WebServiceUnitTestConfig
public class UserAdminServiceTest {

  @Autowired
  private EmbeddedWebApplicationContext server;

  private static Log logger = LogFactory.getLog(UserAdminServiceTest.class);
  private static List<UserEntity> testUserList;
  private static UserEntity testUser1;
  private static UserEntity testUser2;
  private static UserEntity testUser3;
  private RestTemplate restTemplate;


  @BeforeClass
  public static void setupStatic() {
    testUser1 = new UserEntity("username:manu", "Manu Benítez", "capiatá", "ROLE_USER");
    testUser2 = new UserEntity("username:antonio", "Antonio Ruíz", "capiatá", "ROLE_DATA_VIEWER",
        "ROLE_USER");
    testUser3 = new UserEntity("username:javier", "Javier Cáceres", "capiatá",
        "ROLE_SYNCHRONIZE_TABLES", "ROLE_DATA_VIEWER", "ROLE_USER");
    testUserList = new ArrayList<UserEntity>();
    testUserList.add(testUser1);
    testUserList.add(testUser2);
    testUserList.add(testUser3);
  }

  @Before
  public void setup() {
    RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    restTemplate = restTemplateBuilder
        .basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD)
        .build();

    RegionalOffice testOffice = new RegionalOffice("Capiatá, Central", "capiatá");
    putOffice(testOffice);
  }


  @Test
  public void putGetDeleteOneUserTest() {
    logger.info("Create one user " + testUser1.getUserId());
    putGetOneUser(testUser1);
    logger.info("Delete one user " + testUser1.getUserId());
    deleteGetOneUser(testUser1);
  }

  @Test
  public void putGetDeleteListTest() {
    
    // Get initial number of users
    // (Admin and anonymous are automatically created)
    String getUserListUrl = ConstantsUtils.url(server) + "/admin/users/";
    ResponseEntity<List<UserEntity>> initialUserList = restTemplate.exchange(getUserListUrl,
        HttpMethod.GET, null, new ParameterizedTypeReference<List<UserEntity>>() {});
    
    int initialNumberUsers = initialUserList.getBody().size();
    logger.info(initialNumberUsers + " users already exist in the database");

    logger.info("List of all users");
    for (UserEntity entity: initialUserList.getBody()) {
      logger.info(entity);
    }
    
    logger.info("Create multiple test users");
    for (UserEntity userEntity : testUserList) {
      putGetOneUser(userEntity);
    }

    logger.info("Retrieve list of all users");
    getUserListUrl = ConstantsUtils.url(server) + "/admin/users/";
    ResponseEntity<List<UserEntity>> getResponse = restTemplate.exchange(getUserListUrl,
        HttpMethod.GET, null, new ParameterizedTypeReference<List<UserEntity>>() {});
    
    logger.info("List of all users");
    for (UserEntity entity: getResponse.getBody()) {
      logger.info(entity);
    }
    
    logger.info("Check that " + testUserList.size() + " users were added.");
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    logger.info("" + getResponse.getBody().size() + " - " + initialNumberUsers + " = " + testUserList.size());
    assertThat(getResponse.getBody().size() - initialNumberUsers, is(testUserList.size()));

    logger.info("Delete the new users");
    for (UserEntity userEntity : testUserList) {
      deleteGetOneUser(userEntity);
    }
  }

  @Test
  public void testChangePlaintextPassword() {
    putGetOneUser(testUser3);
    String username = testUser3.getUserId().substring(SecurityConsts.USERNAME_COLON.length());
    String password = "mypass";

    String changePasswordUrl =
        ConstantsUtils.url(server) + "/admin/users/" + testUser3.getUserId() + "/password";

    logger.info("Updating password for " + username + " using " + changePasswordUrl);

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    headers.add("Content-Type", "application/json");
    HttpEntity<?> request = new HttpEntity<>(password, headers);
    ResponseEntity<String> postResponse =
        restTemplate.postForEntity(changePasswordUrl, request, String.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.OK));

    logger.info("Retrieving data using " + username + "'s new password");

    RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    RestTemplate userRestTemplate =
        restTemplateBuilder.basicAuthorization(username, password).build();
    String getUserUrl = ConstantsUtils.url(server) + "/admin/users/" + testUser3.getUserId();
    ResponseEntity<UserEntity> getResponse =
        userRestTemplate.exchange(getUserUrl, HttpMethod.GET, null, UserEntity.class);
    UserEntity body = getResponse.getBody();
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    assertThat(body.getUserId(), equalTo(testUser3.getUserId()));
    
    logger.info("Cleanup: deleting " + username);
    deleteGetOneUser(testUser3);
  }

  private void putGetOneUser(UserEntity testUser) {
    String postUserUrl = ConstantsUtils.url(server) + "/admin/users";

    HttpEntity<UserEntity> postUserEntity = new HttpEntity<>(testUser);

    // Submit a new user
    ResponseEntity<UserEntity> postResponse =
        restTemplate.exchange(postUserUrl, HttpMethod.POST, postUserEntity, UserEntity.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.CREATED));

    // Retrieve the new user record
    String getUserUrl = ConstantsUtils.url(server) + "/admin/users/" + testUser.getUserId();

    ResponseEntity<UserEntity> getResponse =
        restTemplate.exchange(getUserUrl, HttpMethod.GET, null, UserEntity.class);
    UserEntity body = getResponse.getBody();
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    assertThat(body.getFullName(), equalTo(testUser.getFullName()));
    assertThat(body.getOfficeId(), equalTo(testUser.getOfficeId()));
    assertThat(body.getUserId(), equalTo(testUser.getUserId()));

    // Why wrap the second list?
    // http://stackoverflow.com/questions/21624592/hamcrest-compare-collections#answer-21628859
    assertThat(body.getRoles(),
        containsInAnyOrder(testUser.getRoles().toArray(new String[testUser.getRoles().size()])));
  }



  private void deleteGetOneUser(UserEntity testUser) {
    String getUserUrl = ConstantsUtils.url(server) + "/admin/users/" + testUser.getUserId();

    ResponseEntity<UserEntity> deleteResponse =
        restTemplate.exchange(getUserUrl, HttpMethod.DELETE, null, UserEntity.class);
    assertThat(deleteResponse.getStatusCode(), is(HttpStatus.NO_CONTENT));

    try {
      // Verify that delete was successful
      ResponseEntity<UserEntity> getResponse =
          restTemplate.exchange(getUserUrl, HttpMethod.GET, null, UserEntity.class);
    } catch (HttpClientErrorException e) {
      assertThat(e.getStatusCode(), is(HttpStatus.NOT_FOUND));
      logger.info(e.getRawStatusCode());
    }
  }

  private void putOffice(RegionalOffice office) {
    String putOfficeUrl = ConstantsUtils.url(server) + "/offices/";

    HttpEntity<RegionalOffice> putOfficeEntity = new HttpEntity<>(office);

    // Submit a new office
    ResponseEntity<RegionalOffice> postResponse =
        restTemplate.exchange(putOfficeUrl, HttpMethod.POST, putOfficeEntity, RegionalOffice.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.CREATED));
  }
}
