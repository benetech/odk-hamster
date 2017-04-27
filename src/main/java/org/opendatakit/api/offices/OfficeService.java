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
package org.opendatakit.api.offices;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.exception.TableAlreadyExistsException;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.opendatakit.persistence.table.OdkRegionalOfficeTable;
import org.opendatakit.security.User;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;

@Api(value = "/offices", description = "ODK Office API",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("offices")
public class OfficeService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(OfficeService.class);


  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String USER_ID = "user_id";
  private static final String FULL_NAME = "full_name";
  private static final String ROLES = "roles";


  @GET
  @Path("list")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getList() throws IOException {

    // Need to set host header? original has
    // resp.addHeader(HttpHeaders.HOST, cc.getServerURL());

    return Response.ok(mapper.writeValueAsString("hewwo")).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  @GET
  @Path("{officeId}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getOffice(@PathParam("officeId") String officeId) {
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    RegionalOffice office = null;
    try {
      OdkRegionalOfficeTable.assertRelation(callingContext);
      OdkRegionalOfficeTable record = 
          OdkRegionalOfficeTable.getRecordFromDatabase(officeId, callingContext);
      office = new RegionalOffice(record.getUri(), record.getRegionalOfficeName(), record.getRegionalOfficeId());
    } catch (ODKDatastoreException | DatastoreFailureException e) {
      logger.error("Error retrieving ", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (NullPointerException e) {
      logger.error("Error retrieving ", e);
      throw new WebApplicationException("Office ID not found\n" + e.toString(),
          HttpServletResponse.SC_NOT_FOUND);
    }

    return Response.ok().entity(office).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  /**
   * Add or update regional office to database.
   *
   * @throws DatastoreFailureException
   */
  @POST
  @Path("")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response putOffice(RegionalOffice office)
      throws AccessDeniedException, DatastoreFailureException {
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    OdkRegionalOfficeTable prototype;
    try {
      prototype = OdkRegionalOfficeTable.assertRelation(callingContext);

      OdkRegionalOfficeTable record = null;

      try {
        // when office is already exists in database it is just edited
        record = ds.getEntity(prototype, office.getUri(), user);
        record.setRegionalOfficeId(office.getOfficeID());
        record.setRegionalOfficeName(office.getName());
      } catch (ODKEntityNotFoundException e) {
        // when office is not exists we create a new record in database
        record = ds.createEntityUsingRelation(prototype, user);
        record.setRegionalOfficeId(office.getOfficeID());
        record.setRegionalOfficeName(office.getName());
      }

      record.persist(callingContext);
      String eTag = Integer.toHexString(record.hashCode());

      return Response.status(Status.CREATED).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();


    } catch (ODKDatastoreException e) {
      logger.error("error uploading zip", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    }

  }

  @DELETE
  @Path("{officeId}")
  public Response deleteOffice(@PathParam("officeId") String officeId)
      throws IOException, DatastoreFailureException {
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    try {
      OdkRegionalOfficeTable recordToDelete =
          OdkRegionalOfficeTable.getRecordFromDatabase(officeId, callingContext);
      if (recordToDelete != null) {
        ds.deleteEntity(recordToDelete.getEntityKey(), user);
      }
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
    return Response.status(Status.NO_CONTENT)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }
}
