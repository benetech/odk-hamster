package org.opendatakit.test.util;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.api.admin.UserAdminServiceTest;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.constants.SecurityConsts;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class TestUtils {
  
  public static void putGetOneOffice(RestTemplate restTemplate,
      EmbeddedWebApplicationContext server, RegionalOffice office) {
    String putOfficeUrl = ConstantsUtils.url(server) + "/offices/";

    HttpEntity<RegionalOffice> putOfficeEntity = new HttpEntity<>(office);

    // Submit a new office
    ResponseEntity<RegionalOffice> postResponse =
        restTemplate.exchange(putOfficeUrl, HttpMethod.POST, putOfficeEntity, RegionalOffice.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.CREATED));

    // Retrieve the new office record
    String getOfficeUrl = ConstantsUtils.url(server) + "/offices/" + office.getOfficeId();

    ResponseEntity<RegionalOffice> getResponse =
        restTemplate.getForEntity(getOfficeUrl, RegionalOffice.class);
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    assertThat(getResponse.getBody().getName(), equalTo(office.getName()));
    assertThat(getResponse.getBody().getOfficeId(), equalTo(office.getOfficeId()));
    assertThat(getResponse.getBody().getUri(), notNullValue());
  }

  public static void putGetOneUser(RestTemplate restTemplate, EmbeddedWebApplicationContext server,
      UserEntity testUser) {
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

  public static void setPlaintextPassword(RestTemplate restTemplate,
      EmbeddedWebApplicationContext server, UserEntity userEntity, String password) {
    
    Log logger = LogFactory.getLog(TestUtils.class);

    String username = userEntity.getUserId().substring(SecurityConsts.USERNAME_COLON.length());

    String changePasswordUrl =
        ConstantsUtils.url(server) + "/admin/users/" + userEntity.getUserId() + "/password";

    logger.info("Updating password for " + username + " using " + changePasswordUrl);

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
    headers.add("Content-Type", "application/json");
    HttpEntity<?> request = new HttpEntity<>(password, headers);
    ResponseEntity<String> postResponse =
        restTemplate.postForEntity(changePasswordUrl, request, String.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.OK));
  }


  public static void deleteGetOneUser(RestTemplate restTemplate,
      EmbeddedWebApplicationContext server, UserEntity testUser) {
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
    }
  }

  public static void putOffice(RestTemplate restTemplate, EmbeddedWebApplicationContext server,
      RegionalOffice office) {
    String putOfficeUrl = ConstantsUtils.url(server) + "/offices/";

    HttpEntity<RegionalOffice> putOfficeEntity = new HttpEntity<>(office);

    // Submit a new office
    ResponseEntity<RegionalOffice> postResponse =
        restTemplate.exchange(putOfficeUrl, HttpMethod.POST, putOfficeEntity, RegionalOffice.class);
    assertThat(postResponse.getStatusCode(), is(HttpStatus.CREATED));
  }

}
