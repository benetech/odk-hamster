package org.opendatakit.api.offices;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.configuration.annotations.BasicWebServiceUnitTestConfig;
import org.opendatakit.test.util.ConstantsUtils;
import org.opendatakit.test.util.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@BasicWebServiceUnitTestConfig
public class OfficeServiceTest {

  @Autowired
  private EmbeddedWebApplicationContext server;
  
  private static Log logger = LogFactory.getLog(OfficeServiceTest.class);
  final PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();
  private static List<RegionalOffice> testOfficeList;
  private static RegionalOffice testOffice1;
  private static RegionalOffice testOffice2;
  private static RegionalOffice testOffice3;
  private static RestTemplate restTemplate;

  @BeforeClass
  public static void setup() {
    RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    restTemplate = restTemplateBuilder
        .basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD)
        .build();

    testOffice1 = new RegionalOffice("Madison, Wisconsin", "madison");
    testOffice2 = new RegionalOffice("Oostburg, Wisconsin", "oostburg");
    testOffice3 = new RegionalOffice("Sheboygan, Wisconsin", "sheboygan");
    testOfficeList = new ArrayList<RegionalOffice>();
    testOfficeList.add(testOffice1);
    testOfficeList.add(testOffice2);
    testOfficeList.add(testOffice3);
  }

  @Test
  public void putGetDeleteTest() throws IOException {
    RegionalOffice testOffice = new RegionalOffice("Caaguazú, Caaguazú", "caaguazú");

    String getOfficeUrl = ConstantsUtils.url(server) + "/offices/caaguazú";

    TestUtils.putGetOneOffice(restTemplate, server, testOffice);
    // Delete the new office record
    ResponseEntity<String> deleteReponse =
        restTemplate.exchange(getOfficeUrl, HttpMethod.DELETE, null, String.class);
    assertThat(deleteReponse.getStatusCode(), is(HttpStatus.NO_CONTENT));

    // Make sure it's deleted
    try {
      ResponseEntity<RegionalOffice> getResponse =
          restTemplate.getForEntity(getOfficeUrl, RegionalOffice.class);
    } catch (HttpClientErrorException e) {
      assertThat(e.getStatusCode(), is(HttpStatus.NOT_FOUND));
      logger.info(e.getRawStatusCode());
      logger.info(e.getResponseBodyAsString());
    }
  }
  
  @Test
  public void putGetListTest() throws Exception {
    for (RegionalOffice testOffice : testOfficeList) {
      TestUtils.putGetOneOffice(restTemplate, server, testOffice);
    }
    String getOfficeListUrl = ConstantsUtils.url(server) + "/offices/";

    ResponseEntity<List<RegionalOffice>> getResponse =
        restTemplate.exchange(getOfficeListUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<RegionalOffice>>() {});
  
    assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
    assertThat(getResponse.getBody().size(), is(3));
  }
  




}
