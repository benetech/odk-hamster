package org.opendatakit.api.forms;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.api.forms.entity.FormUploadResult;
import org.opendatakit.configuration.annotations.BasicWebServiceIntTestConfig;
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
@BasicWebServiceIntTestConfig
public class FormServiceTest {

  private static Log logger = LogFactory.getLog(FormServiceTest.class);
  final PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();

  private File sampleForm;

  @Autowired
  private EmbeddedWebApplicationContext server;

  @Before
  public void init() throws IOException {
    sampleForm = pmrpr.getResource("classpath:forms/hamsterform.zip").getFile();
  }

  @Test
  // http://stackoverflow.com/questions/4118670/sending-multipart-file-as-post-parameters-with-resttemplate-requests
  public void testUploadFormDefinition() throws IOException {
    FormHttpMessageConverter formConverter = new FormHttpMessageConverter();
    formConverter.setCharset(Charset.forName("UTF8"));

    RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
    RestTemplate restTemplate = restTemplateBuilder
        .additionalMessageConverters(formConverter, new MappingJackson2HttpMessageConverter())
        .basicAuthorization(ConstantsUtils.TEST_ADMIN_USERNAME, ConstantsUtils.TEST_ADMIN_PASSWORD)
        .build();


    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<String, Object>();
    parts.add(WebConsts.ZIP_FILE, new FileSystemResource(sampleForm));
    parts.add(WebConsts.OFFICE_ID, "madison");
    String target = "http://localhost:" + server.getEmbeddedServletContainer().getPort() + "/forms/"
        + ConstantsUtils.APP_ID_PARAMETER + "/" + ConstantsUtils.API_VERSION_PARAMETER;

    logger.info("Sending to URL: " + target);
    try {
      ResponseEntity<FormUploadResult> entity =
          restTemplate.postForEntity(target, parts, FormUploadResult.class);
      logger.info(entity.toString());
      logger.info(entity.getBody());
    } catch (HttpClientErrorException e) {
      logger.info(e.getRawStatusCode());
      logger.info(e.getResponseBodyAsString());
    }
  }



  @Test
  public void testProcessZipInputStream() throws IOException {
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sampleForm));

    Map<String, byte[]> result = FormService.processZipInputStream(zipInputStream);

    assertThat(result.keySet(), containsInAnyOrder("tables/hamsterform/forms/hamsterform/formDef.json","tables/hamsterform/properties.csv","tables/hamsterform/definition.csv"));
  }

  @Test
  public void testGetTableIdAndDefinitionFromFiles() throws IOException {
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sampleForm));

    Map<String, byte[]> files = FormService.processZipInputStream(zipInputStream);
    String definition = FormService.getDefinitionFromFiles(files);
    String tableId = FormService.getTableIdFromFiles(files);
    assertThat(definition.length(), is(408));
    assertThat(tableId, is("hamsterform"));
  }
  
  @Test
  public void testParseColumnsFromCsv() throws IOException {
    ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sampleForm));

    Map<String, byte[]> files = FormService.processZipInputStream(zipInputStream);
    String definition = FormService.getDefinitionFromFiles(files);
    List<Column> columns = FormService.parseColumnsFromCsv(definition);
    assertThat(columns.size(), is(7));

  }
 


}
