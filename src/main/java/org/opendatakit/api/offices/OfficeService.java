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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.api.offices.entity.RegionalOffice;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.CommonFieldsBase;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.Query;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.table.OdkRegionalOfficeTable;
import org.opendatakit.security.User;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "/offices", description = "ODK Office API",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("offices")
public class OfficeService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(OfficeService.class);


  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @ApiOperation(value = "Get list of all offices.", response = RegionalOffice.class,
  responseContainer = "List")
  public Response getList() throws IOException {
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    List<RegionalOffice> regionalOffices = new ArrayList<RegionalOffice>();
    try {
      OdkRegionalOfficeTable.assertRelation(callingContext);
      OdkRegionalOfficeTable regionalOfficeTable =
          OdkRegionalOfficeTable.assertRelation(callingContext);
      Query q = ds.createQuery(regionalOfficeTable, "OdkRegionalOfficeTable.getAllOffices", user);
      List<? extends CommonFieldsBase> l = q.executeQuery();
      for (CommonFieldsBase cb : l) {
        OdkRegionalOfficeTable t = (OdkRegionalOfficeTable) cb;
        RegionalOffice i =
            new RegionalOffice(t.getUri(), t.getRegionalOfficeName(), t.getRegionalOfficeId());
        regionalOffices.add(i);
      }
    } catch (ODKDatastoreException e) {
      logger.error("Error retrieving ", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    return Response.ok().entity(regionalOffices).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  @ApiOperation(value = "Get information about a particular office.", response = RegionalOffice.class)
  @GET
  @Path("{officeId}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getOffice(@PathParam("officeId") String officeId) {
    RegionalOffice office = null;
    try {
      OdkRegionalOfficeTable.assertRelation(callingContext);
      OdkRegionalOfficeTable record =
          OdkRegionalOfficeTable.getRecordFromDatabase(officeId, callingContext);
      if (record == null) {
        return Response.status(Status.NOT_FOUND).entity("Office ID not found.")
            .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true").build();
      }
      office = new RegionalOffice(record.getUri(), record.getRegionalOfficeName(),
          record.getRegionalOfficeId());
    } catch (ODKDatastoreException | DatastoreFailureException e) {
      logger.error("Error retrieving ", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
  @ApiOperation(
      value = "Add or update office to database.  Uses officeId field as unique key which determines if office is created or updated.")
  @POST
  @Path("/")
  @Secured({"ROLE_SITE_ACCESS_ADMIN"})
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

      logger.info("Incoming officeId " + (office != null ?  office.toString() : "null"));

      OdkRegionalOfficeTable record =
          OdkRegionalOfficeTable.getRecordFromDatabase(office.getOfficeId(), callingContext);

      logger.info("Looked up officeId " + (record != null ?  record.toString() : "null"));
      try {
        // when office is already exists in database it is just edited
        record = ds.getEntity(prototype, office.getUri(), user);
        record.setRegionalOfficeId(office.getOfficeId());
        record.setRegionalOfficeName(office.getName());
      } catch (ODKEntityNotFoundException e) {
        // when office is not exists we create a new record in database
        record = ds.createEntityUsingRelation(prototype, user);
        record.setRegionalOfficeId(office.getOfficeId());
        record.setRegionalOfficeName(office.getName());
      }

      record.persist(callingContext);
      String eTag = Integer.toHexString(record.hashCode());

      return Response.status(Status.CREATED).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();

    } catch (ODKDatastoreException e) {
      logger.error("Error adding or updating office", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_BAD_REQUEST);

    }

  }

  @ApiOperation(value = "Delete office.")
  @DELETE
  @Secured({"ROLE_SITE_ACCESS_ADMIN"})
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
