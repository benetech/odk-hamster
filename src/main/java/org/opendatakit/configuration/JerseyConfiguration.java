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
package org.opendatakit.configuration;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.glassfish.jersey.servlet.ServletProperties;
import org.opendatakit.api.RootRedirect;
import org.opendatakit.api.filter.GzipReaderInterceptor;
import org.opendatakit.api.filter.MultipartFormDataToMixedInterceptor;
import org.opendatakit.api.forms.FormService;
import org.opendatakit.api.odktables.DataService;
import org.opendatakit.api.odktables.DiffService;
import org.opendatakit.api.odktables.FileManifestService;
import org.opendatakit.api.odktables.FileService;
import org.opendatakit.api.odktables.InstanceFileService;
import org.opendatakit.api.odktables.OdkTables;
import org.opendatakit.api.odktables.QueryService;
import org.opendatakit.api.odktables.RealizedTableService;
import org.opendatakit.api.odktables.TableAclService;
import org.opendatakit.api.odktables.TableService;
import org.opendatakit.api.odktables.mapper.IOExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.IOExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.IOExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionTextXmlMapper;
import org.opendatakit.api.users.RoleService;
import org.opendatakit.api.users.UserService;
import org.opendatakit.odktables.entity.serialization.SimpleHTMLMessageWriter;
import org.opendatakit.odktables.entity.serialization.SimpleJSONMessageReaderWriter;
import org.opendatakit.odktables.entity.serialization.SimpleXMLMessageReaderWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletConfigAware;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import io.swagger.models.auth.BasicAuthDefinition;

@Component
public class JerseyConfiguration extends ResourceConfig implements ServletConfigAware {
	private static Log logger = LogFactory.getLog(JerseyConfiguration.class);

	@Value("${spring.jersey.application-path:/}")
	private String apiPath;

	private ServletConfig servletConfig;
	
	public JerseyConfiguration() {
		registerEndpoints();
		this.configureSwagger();
	}

	@PostConstruct
	public void init() {
		// this.configureSwagger();
	}

	private void registerEndpoints() {
		logger.info("Registering Jersey classes.");

		// Uncomment to handle proxied URLs
		// register(ProxyUrlSetFilter.class);
		register(GzipReaderInterceptor.class);
		register(MultipartFormDataToMixedInterceptor.class);

		register(MultiPartFeature.class);

		// ODK Tables Synchronization 2.0 API
		register(OdkTables.class);
		register(DataService.class);
		register(DiffService.class);
		register(FileManifestService.class);
		register(FileService.class);
		register(InstanceFileService.class);
		register(QueryService.class);
		register(RealizedTableService.class);
		register(TableAclService.class);
		register(TableService.class);

		// Legacy User/Roles ODK 1.0 API
		register(RoleService.class);
		register(UserService.class);

		// Form management
		register(FormService.class);

		// Mapper classes
		register(SimpleHTMLMessageWriter.class);
		register(SimpleJSONMessageReaderWriter.class);
		register(SimpleXMLMessageReaderWriter.class);
		register(ODKDatastoreExceptionJsonMapper.class);
		register(ODKDatastoreExceptionTextXmlMapper.class);
		register(ODKDatastoreExceptionApplicationXmlMapper.class);
		register(ODKTablesExceptionJsonMapper.class);
		register(ODKTablesExceptionTextXmlMapper.class);
		register(ODKTablesExceptionApplicationXmlMapper.class);
		register(ODKTaskLockExceptionJsonMapper.class);
		register(ODKTaskLockExceptionTextXmlMapper.class);
		register(ODKTaskLockExceptionApplicationXmlMapper.class);
		register(IOExceptionJsonMapper.class);
		register(IOExceptionTextXmlMapper.class);
		register(IOExceptionApplicationXmlMapper.class);
		register(RuntimeExceptionJsonMapper.class);
		register(RuntimeExceptionTextXmlMapper.class);
		register(RuntimeExceptionApplicationXmlMapper.class);

		// Redirect the root to something user friendly, or for security through
		// obfuscation (good luck with the latter.)
		register(RootRedirect.class);

		// Generate Jersey WADL at /<Jersey's servlet path>/application.wadl
		register(WadlResource.class);

		// Forward when not found, lets us get to static content like swagger
		property(ServletProperties.FILTER_STATIC_CONTENT_REGEX, "((/swagger/.*)|(.*\\.(html|ico)))");

	}

	/**
	 * Configure the Swagger documentation for this API.
	 * 
	 * Very useful setup instructions:
	 * http://tech.asimio.net/2016/04/05/Microservices-using-Spring-Boot-Jersey-Swagger-and-Docker.html
	 */
	private void configureSwagger() {
		// Creates file at localhost:port/swagger.json
		this.register(ApiListingResource.class);
		this.register(SwaggerSerializers.class);

		BeanConfig config = new BeanConfig();
		config.setConfigId("odk-tables-sync-hamster");
		config.setTitle("OpenDataKit 2.0 Tables Sync + Hamster API");
		config.setVersion("2");
		config.setContact("Caden Howell <cadenh@benetech.org>");
		config.setSchemes(new String[] { "http", "https" });
		config.setResourcePackage("org.opendatakit.api");
		config.setBasePath(this.apiPath);
		config.setPrettyPrint(true);
		config.setScan(true);
		
		Swagger swagger = new Swagger();
		swagger.securityDefinition("basicAuth", new BasicAuthDefinition());
		new SwaggerContextService().withServletConfig(servletConfig).updateSwagger(swagger);

	}

	@Override
	public void setServletConfig(ServletConfig servletConfig) {
		logger.info("Setting ServletConfig");
		this.servletConfig = servletConfig;
	}
}
