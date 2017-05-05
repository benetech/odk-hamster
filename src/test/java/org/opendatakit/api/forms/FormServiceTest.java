package org.opendatakit.api.forms;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.api.forms.entity.FormUploadResult;
import org.opendatakit.configuration.annotations.BasicWebServiceUnitTestConfig;
import org.opendatakit.constants.WebConsts;
import org.opendatakit.test.util.ConstantsUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@BasicWebServiceUnitTestConfig
public class FormServiceTest {

	private static Log logger = LogFactory.getLog(FormServiceTest.class);
	final PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();

	@Autowired
	private EmbeddedWebApplicationContext server;

	@Test
	// http://stackoverflow.com/questions/4118670/sending-multipart-file-as-post-parameters-with-resttemplate-requests
	public void uploadFormDefinition() throws IOException {
		FormHttpMessageConverter formConverter = new FormHttpMessageConverter();
		formConverter.setCharset(Charset.forName("UTF8"));

		RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
		RestTemplate restTemplate = restTemplateBuilder
				.additionalMessageConverters(formConverter, new MappingJackson2HttpMessageConverter())
				.basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD).build();

		final File sampleForm = pmrpr.getResource("classpath:forms/hamsterform.zip").getFile();

		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
		parts.add(WebConsts.ZIP_FILE, new FileSystemResource(sampleForm));
		parts.add(WebConsts.OFFICE_ID, "madison");
		String target = "http://localhost:" + server.getEmbeddedServletContainer().getPort() + "/forms/"
				+ ConstantsUtils.APP_ID_PARAMETER + "/" + ConstantsUtils.API_VERSION_PARAMETER;

		logger.info("Sending to URL: " + target);
		try {
			ResponseEntity<FormUploadResult> entity = restTemplate.postForEntity(target, parts, FormUploadResult.class);
			logger.info(entity.toString());
			logger.info(entity.getBody());
		} catch (HttpClientErrorException e) {
			logger.info(e.getRawStatusCode());
			logger.info(e.getResponseBodyAsString());
		}
	}
	

}
