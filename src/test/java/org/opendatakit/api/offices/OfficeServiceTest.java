package org.opendatakit.api.offices;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import org.opendatakit.configuration.annotations.WebServiceUnitTestConfig;
import org.opendatakit.test.ConstantsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@WebServiceUnitTestConfig
public class OfficeServiceTest {

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
				.basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD).build();

		testOffice1 = new RegionalOffice("Madison, Wisconsin", "madison");
		testOffice2 = new RegionalOffice("Oostburg, Wisconsin", "oostburg");
		testOffice3 = new RegionalOffice("Sheboygan, Wisconsin", "sheboygan");
		testOfficeList = new ArrayList<RegionalOffice>();
		testOfficeList.add(testOffice1);
		testOfficeList.add(testOffice2);
		testOfficeList.add(testOffice3);
	}

	@Autowired
	private EmbeddedWebApplicationContext server;

	@Test
	public void putGet() throws IOException {
		RegionalOffice testOffice = new RegionalOffice("Caaguazú, Caaguazú", "caaguazú");

		String putOfficeUrl = ConstantsUtils.url(server) + "/offices/";

		HttpEntity<RegionalOffice> putOfficeEntity = new HttpEntity<>(testOffice);

		// Submit a new office
		ResponseEntity<RegionalOffice> postResponse = restTemplate.exchange(putOfficeUrl, HttpMethod.POST,
				putOfficeEntity, RegionalOffice.class);
		assertThat(postResponse.getStatusCode(), is(HttpStatus.CREATED));

		// Retrieve the new office record
		String getOfficeUrl = ConstantsUtils.url(server) + "/offices/caaguazú";

		ResponseEntity<RegionalOffice> getResponse = restTemplate.getForEntity(getOfficeUrl, RegionalOffice.class);
		assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
		assertThat(getResponse.getBody().getName(), equalTo(testOffice.getName()));
		assertThat(getResponse.getBody().getOfficeID(), equalTo(testOffice.getOfficeID()));
		assertThat(getResponse.getBody().getUri(), notNullValue());

		// Delete the new office record
		ResponseEntity<String> deleteReponse = restTemplate.exchange(getOfficeUrl, HttpMethod.DELETE, null,
				String.class);
		assertThat(deleteReponse.getStatusCode(), is(HttpStatus.NO_CONTENT));

		// Make sure it's deleted
		try {
			getResponse = restTemplate.getForEntity(getOfficeUrl, RegionalOffice.class);
		} catch (HttpClientErrorException e) {
			assertThat(e.getStatusCode(), is(HttpStatus.NOT_FOUND));
			logger.info(e.getRawStatusCode());
			logger.info(e.getResponseBodyAsString());
		}
	}
}
