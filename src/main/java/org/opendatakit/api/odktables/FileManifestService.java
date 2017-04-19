/*
 * Copyright (C) 2012-2013 University of Washington
 *
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
package org.opendatakit.api.odktables;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.context.CallingContext;
import org.opendatakit.odktables.FileManifestManager;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.relation.DbManifestETags;
import org.opendatakit.odktables.relation.DbManifestETags.DbManifestETagEntity;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.persistence.exception.ODKTaskLockException;

/**
 * Servlet for downloading a manifest of files to the phone for the correct app and the correct
 * table.
 *
 * @author sudar.sam@gmail.com
 */
public class FileManifestService {

  private final CallingContext cc;
  private final String appId;
  private final UriInfo info;

  public FileManifestService(UriInfo info, String appId, CallingContext cc)
      throws ODKEntityNotFoundException, ODKDatastoreException, PermissionDeniedException,
      ODKTaskLockException {
    this.cc = cc;
    this.appId = appId;
    this.info = info;
  }

  public static String getAppLevelManifestETag(CallingContext cc) throws ODKDatastoreException {
    DbManifestETagEntity eTagEntity =
        DbManifestETags.getTableIdEntry(DbManifestETags.APP_LEVEL, cc);
    return eTagEntity.getManifestETag();
  }

  public static String getTableLevelManifestETag(String tableId, CallingContext cc)
      throws ODKDatastoreException {
    DbManifestETagEntity eTagEntity = DbManifestETags.getTableIdEntry(tableId, cc);
    return eTagEntity.getManifestETag();
  }

  /**
   *
   * @param httpHeaders
   * @param odkClientVersion
   * @return {@link OdkTablesFileManifest} of all the files meeting the filter criteria.
   * @throws ODKOverQuotaException
   * @throws ODKEntityNotFoundException
   * @throws ODKTaskLockException
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   */
  @GET
  @Path("{odkClientVersion}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* OdkTablesFileManifest */ getAppLevelFileManifest(
      @Context HttpHeaders httpHeaders, @PathParam("odkClientVersion") String odkClientVersion)
      throws ODKEntityNotFoundException, ODKOverQuotaException, PermissionDeniedException,
      ODKDatastoreException, ODKTaskLockException {

    FileManifestManager manifestManager = new FileManifestManager(appId, odkClientVersion, cc);
    OdkTablesFileManifest manifest = null;

    // retrieve the incoming if-none-match eTag...
    List<String> eTags = httpHeaders.getRequestHeader(HttpHeaders.IF_NONE_MATCH);
    String eTag = (eTags == null || eTags.isEmpty()) ? null : eTags.get(0);
    DbManifestETagEntity eTagEntity = null;
    try {
      try {
        eTagEntity = DbManifestETags.getTableIdEntry(DbManifestETags.APP_LEVEL, cc);
      } catch (ODKEntityNotFoundException e) {
        // ignore...
      }
      if (eTag != null && eTagEntity != null && eTag.equals(eTagEntity.getManifestETag())) {
        return Response.status(Status.NOT_MODIFIED).header(HttpHeaders.ETAG, eTag)
            .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true").build();
      }
      // we want just the app-level files.
      manifest = manifestManager.getManifestForAppLevelFiles();

    } catch (ODKDatastoreException e) {
      Log log = LogFactory.getLog(FileManifestService.class);
      log.error("Datastore exception in getting the file manifest");
      e.printStackTrace();
    }
    if (manifest == null) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to retrieve manifest.")
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      String newETag = Integer.toHexString(manifest.hashCode());
      // create a new eTagEntity if there isn't one already...
      if (eTagEntity == null) {
        eTagEntity = DbManifestETags.createNewEntity(DbManifestETags.APP_LEVEL, cc);
        eTagEntity.setManifestETag(newETag);
        eTagEntity.put(cc);
      } else if (!newETag.equals(eTagEntity.getManifestETag())) {
        Log log = LogFactory.getLog(FileManifestService.class);
        log.error("App-level Manifest ETag does not match computed value!");
        eTagEntity.setManifestETag(newETag);
        eTagEntity.put(cc);
      }
      // and whatever the eTag is in that entity is the eTag we should return...
      eTag = eTagEntity.getManifestETag();
      UriBuilder uriBuilder = info.getBaseUriBuilder();
      uriBuilder.path(OdkTables.class);
      //UriBuilder uriBuilder = UriBuilder.fromResource(OdkTables.class);
      uriBuilder.path(OdkTables.class, "getFilesService");
      // now supply the downloadUrl...
      for (OdkTablesFileManifestEntry entry : manifest.getFiles()) {
        URI self = uriBuilder.clone().path(FileService.class, "getFile").build(ArrayUtils.toArray(appId, odkClientVersion,
            entry.filename), false);
        try {
          entry.downloadUrl = self.toURL().toExternalForm();
        } catch (MalformedURLException e) {
          e.printStackTrace();
          throw new IllegalArgumentException("Unable to convert to URL");
        }
      }

      return Response.ok(manifest).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    }
  }

  /**
   *
   * @param httpHeaders
   * @param odkClientVersion
   * @param tableId
   * @return {@link OdkTablesFileManifest} of all the files meeting the filter criteria.
   * @throws ODKOverQuotaException
   * @throws ODKEntityNotFoundException
   * @throws ODKTaskLockException
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   */
  @GET
  @Path("{odkClientVersion}/{tableId}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* OdkTablesFileManifest */ getTableIdFileManifest(
      @Context HttpHeaders httpHeaders, @PathParam("odkClientVersion") String odkClientVersion,
      @PathParam("tableId") String tableId)
      throws ODKEntityNotFoundException, ODKOverQuotaException, PermissionDeniedException,
      ODKDatastoreException, ODKTaskLockException {

    FileManifestManager manifestManager = new FileManifestManager(appId, odkClientVersion, cc);
    OdkTablesFileManifest manifest = null;

    // retrieve the incoming if-none-match eTag...
    List<String> eTags = httpHeaders.getRequestHeader(HttpHeaders.IF_NONE_MATCH);
    String eTag = (eTags == null || eTags.isEmpty()) ? null : eTags.get(0);
    DbManifestETagEntity eTagEntity = null;
    try {
      try {
        eTagEntity = DbManifestETags.getTableIdEntry(tableId, cc);
      } catch (ODKEntityNotFoundException e) {
        // ignore...
      }
      if (eTag != null && eTagEntity != null && eTag.equals(eTagEntity.getManifestETag())) {
        return Response.status(Status.NOT_MODIFIED).header(HttpHeaders.ETAG, eTag)
            .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true").build();
      }
      // we want just the files for the table.
      manifest = manifestManager.getManifestForTable(tableId);
    } catch (ODKDatastoreException e) {
      Log log = LogFactory.getLog(FileManifestService.class);
      log.error("Datastore exception in getting the file manifest");
      e.printStackTrace();
    }
    if (manifest == null) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to retrieve manifest.")
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      String newETag = Integer.toHexString(manifest.hashCode());
      // create a new eTagEntity if there isn't one already...
      if (eTagEntity == null) {
        eTagEntity = DbManifestETags.createNewEntity(tableId, cc);
        eTagEntity.setManifestETag(newETag);
        eTagEntity.put(cc);
      } else if (!newETag.equals(eTagEntity.getManifestETag())) {
        Log log = LogFactory.getLog(FileManifestService.class);
        log.error("Table-level (" + tableId + ") Manifest ETag does not match computed value!");
        eTagEntity.setManifestETag(newETag);
        eTagEntity.put(cc);
      }
      // and whatever the eTag is in that entity is the eTag we should return...
      eTag = eTagEntity.getManifestETag();
      UriBuilder ub = info.getBaseUriBuilder();
      ub.path(OdkTables.class);
      ub.path(OdkTables.class, "getFilesService");
      // now supply the downloadUrl...
      for (OdkTablesFileManifestEntry entry : manifest.getFiles()) {
        URI self = ub.clone().path(FileService.class, "getFile").build(ArrayUtils.toArray(appId, odkClientVersion,
            entry.filename),false);
        try {
          entry.downloadUrl = self.toURL().toExternalForm();
        } catch (MalformedURLException e) {
          e.printStackTrace();
          throw new IllegalArgumentException("Unable to convert to URL");
        }
      }

      return Response.ok(manifest).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    }
  }

}
