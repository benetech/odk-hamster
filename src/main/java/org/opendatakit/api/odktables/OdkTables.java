package org.opendatakit.api.odktables;

import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.ContextUtils;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.AppNameList;
import org.opendatakit.aggregate.odktables.rest.entity.ClientVersionList;
import org.opendatakit.context.CallingContext;
import org.opendatakit.odktables.exception.AppNameMismatchException;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.relation.DbTableFileInfo;
import org.opendatakit.odktables.util.ServiceUtils;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("odktables")
@Component
public class OdkTables {

  public static final String CURSOR_PARAMETER = "cursor";
  public static final String FETCH_LIMIT = "fetchLimit";
  public static final String OFFICE_ID = "officeId";

  @Autowired
  CallingContext callingContext;

  @GET
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getAppNames(@Context ServletContext sc, @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders) throws ODKDatastoreException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    // CallingContext cc = ContextFactory.getCallingContext(sc, req);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    AppNameList appNames = new AppNameList(Collections.singletonList(preferencesAppId));
    return Response.ok(appNames)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  @GET
  @Path("{appId}/clientVersions")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* ClientVersionList */ getOdkClientVersions(@Context ServletContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders, @Context UriInfo info,
      @PathParam("appId") String appId) throws AppNameMismatchException, PermissionDeniedException,
      ODKDatastoreException, ODKTaskLockException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    // CallingContext cc = ContextFactory.getCallingContext(sc, req);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    if (!preferencesAppId.equals(appId)) {
      throw new AppNameMismatchException("AppName (" + appId + ") differs");
    }

    List<String> distinctOdkClientVersions = null;
    String eTagOdkClientVersions = null;

    // retrieve the incoming if-none-match eTag...
    List<String> eTags = httpHeaders.getRequestHeader(HttpHeaders.IF_NONE_MATCH);
    String eTag = (eTags == null || eTags.isEmpty()) ? null : eTags.get(0);
    try {
      distinctOdkClientVersions = DbTableFileInfo.queryForAllOdkClientVersions(callingContext);
      eTagOdkClientVersions = Integer.toHexString(
          (distinctOdkClientVersions == null) ? -1 : distinctOdkClientVersions.hashCode());

      if (eTag != null && distinctOdkClientVersions != null && eTag.equals(eTagOdkClientVersions)) {
        return Response.status(Status.NOT_MODIFIED).header(HttpHeaders.ETAG, eTag)
            .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true").build();
      }
    } catch (ODKDatastoreException e) {
      Log log = LogFactory.getLog(FileManifestService.class);
      log.error("Datastore exception in getting the file manifest");
      e.printStackTrace();
    }

    if (distinctOdkClientVersions == null) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity("Unable to retrieve odkClientVersions.")
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      UriBuilder ub = info.getBaseUriBuilder();
      ub.path(OdkTables.class);
      ub.path(OdkTables.class, "getOdkClientVersions");

      ClientVersionList clientVersions = new ClientVersionList(distinctOdkClientVersions);
      return Response.ok(clientVersions).header(HttpHeaders.ETAG, eTagOdkClientVersions)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    }
  }

  @Path("{appId}/manifest")
  public FileManifestService getFileManifestService(@Context ServletContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders, @Context UriInfo info,
      @PathParam("appId") String appId) throws AppNameMismatchException, PermissionDeniedException,
      ODKDatastoreException, ODKTaskLockException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    // CallingContext cc = ContextFactory.getCallingContext(sc, req);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    if (!preferencesAppId.equals(appId)) {
      throw new AppNameMismatchException("AppName (" + appId + ") differs");
    }

    return new FileManifestService(info, appId, callingContext);
  }

  @Path("{appId}/files")
  public FileService getFilesService(@Context ServletContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders, @Context UriInfo info,
      @PathParam("appId") String appId) throws AppNameMismatchException, PermissionDeniedException,
      ODKDatastoreException, ODKTaskLockException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    if (!preferencesAppId.equals(appId)) {
      throw new AppNameMismatchException("AppName (" + appId + ") differs");
    }

    return new FileService(sc, req, httpHeaders, info, appId, callingContext);
  }

  @GET
  @Path("{appId}/tables")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* TableResourceList */ getTables(@Context ServletContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders, @Context UriInfo info,
      @PathParam("appId") String appId, @QueryParam(CURSOR_PARAMETER) String cursor,
      @QueryParam(FETCH_LIMIT) String fetchLimit, @QueryParam(OFFICE_ID) String officeId)
      throws AppNameMismatchException, PermissionDeniedException, ODKDatastoreException,
      ODKTaskLockException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    // CallingContext cc = ContextFactory.getCallingContext(sc, req);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    if (!preferencesAppId.equals(appId)) {
      throw new AppNameMismatchException("AppName (" + appId + ") differs");
    }

    TableService ts = new TableService(sc, req, httpHeaders, info, appId, callingContext);
    return ts.getTables(cursor, fetchLimit, officeId);
  }

  @Path("{appId}/tables/{tableId}")
  public TableService getTablesService(@Context ServletContext sc,
      @Context HttpServletRequest req, @Context HttpHeaders httpHeaders, @Context UriInfo info,
      @PathParam("appId") String appId, @PathParam("tableId") String tableId)
      throws AppNameMismatchException, PermissionDeniedException, ODKDatastoreException,
      ODKTaskLockException {

    ServiceUtils.examineRequest(sc, req, httpHeaders);
    // CallingContext cc = ContextFactory.getCallingContext(sc, req);
    String preferencesAppId = ContextUtils.getOdkTablesAppId(callingContext);

    if (!preferencesAppId.equals(appId)) {
      throw new AppNameMismatchException("AppName (" + appId + ") differs");
    }

    return new TableService(sc, req, httpHeaders, info, appId, tableId, callingContext);
  }

}
