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
package org.opendatakit.api.healthcheck;

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.context.CallingContext;
import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "/healthcheck", description = "Health check for services like AWS",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("/healthcheck")
public class HealthCheckService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(HealthCheckService.class);

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @ApiOperation(value = "Simple health check.", response = String.class)
  public Response getList() throws IOException {

    return Response.ok().entity("Health Check OK").encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

}
