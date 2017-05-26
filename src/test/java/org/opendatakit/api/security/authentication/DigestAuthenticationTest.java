package org.opendatakit.api.security.authentication;


import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.benetech.security.client.digest.DigestRestTemplateFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.configuration.annotations.DigestWebServiceIntTestConfig;
import org.opendatakit.context.CallingContext;
import org.opendatakit.test.util.ConstantsUtils;
import org.opendatakit.test.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@DigestWebServiceIntTestConfig
public class DigestAuthenticationTest {

  @Autowired
  private EmbeddedWebApplicationContext server;

  @Autowired
  CallingContext callingContext;


  private static Log logger = LogFactory.getLog(DigestAuthenticationTest.class);
  private static UserEntity testUser1;
  private static RegionalOffice testOffice1;
  private static RestTemplate restTemplate;
  private static String realmName;

  // Using PostConstruct instead of @BeforeClass because it allows non-static method
  @PostConstruct
  public void setup() {
    
    realmName = callingContext.getUserService().getCurrentRealm().getRealmString();
    RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    restTemplate = restTemplateBuilder
        .basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD)
        .build();

    testUser1 = new UserEntity("username:antonio", "Antonio Ruíz", "capiatá", "ROLE_DATA_VIEWER",
        "ROLE_USER");

    testOffice1 = new RegionalOffice("Capiatá, Central", "capiatá");

    TestUtils.putOffice(restTemplate, server, testOffice1);
    TestUtils.putGetOneUser(restTemplate, server, testUser1);
    TestUtils.setPlaintextPassword(restTemplate, server, testUser1, "mypass");
  }


  @Test
  public void digestAuthTest() throws IOException {
    RestTemplate digestRestTemplate = DigestRestTemplateFactory.getRestTemplate("localhost",
        server.getEmbeddedServletContainer().getPort(), "http", realmName, "antonio", "mypass");

    logger.info("Retrieve list of current user roles");
    String getRolesGrantedUrl = ConstantsUtils.url(server) + "/roles/granted/";
    ResponseEntity<List<String>> getResponse = digestRestTemplate.exchange(getRolesGrantedUrl,
        HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {});

    logger.info("List of current user roles");
    for (String role : getResponse.getBody()) {
      logger.info(role);
    }

    logger.info("Status code: " + getResponse.getStatusCode());

  }
}
