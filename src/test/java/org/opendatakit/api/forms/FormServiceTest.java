package org.opendatakit.api.forms;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.api.forms.entity.FormUploadResult;
import org.opendatakit.configuration.annotations.WebServiceUnitTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@WebServiceUnitTestConfig
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
    RestTemplate restTemplate =  restTemplateBuilder.additionalMessageConverters(formConverter,
        new MappingJackson2HttpMessageConverter())
    //restTemplateBuilder.requestFactory(new HttpComponentsClientHttpRequestFactory());
    .basicAuthorization("admin", "aggregate")
    .build();


    final File sampleForm = pmrpr.getResource("classpath:forms/hamsterform.zip").getFile();

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
    parts.add("form_zip", new FileSystemResource(sampleForm));
    parts.add("officeId", "madison");
    String target = "http://localhost:" + server.getEmbeddedServletContainer().getPort() + "/form";
    logger.info("Sending to URL: " + target);
    try {
    ResponseEntity<FormUploadResult> entity =
        restTemplate.postForEntity(target, parts, FormUploadResult.class);
        logger.info(entity.toString());
        logger.info(entity.getBody());
    }
    catch (HttpClientErrorException e) {
      logger.info(e.getRawStatusCode());
      logger.info(e.getResponseBodyAsString());
    }

    // assertThat(entity.getBody()).isEqualTo("[\"default\"]");
    // assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

}
