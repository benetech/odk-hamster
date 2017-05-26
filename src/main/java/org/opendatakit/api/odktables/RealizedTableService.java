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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinition;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinitionResource;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.WebConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.datamodel.BinaryContent;
import org.opendatakit.odktables.FileContentInfo;
import org.opendatakit.odktables.InstanceFileManager;
import org.opendatakit.odktables.TableManager;
import org.opendatakit.odktables.InstanceFileManager.FetchBlobHandler;
import org.opendatakit.odktables.InstanceFileManager.FileContentHandler;
import org.opendatakit.odktables.exception.AppNameMismatchException;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.exception.SchemaETagMismatchException;
import org.opendatakit.odktables.exception.TableNotFoundException;
import org.opendatakit.odktables.relation.DbTableInstanceFiles;
import org.opendatakit.odktables.relation.DbTableInstanceManifestETags;
import org.opendatakit.odktables.relation.DbTableInstanceManifestETags.DbTableInstanceManifestETagEntity;
import org.opendatakit.odktables.security.TablesUserPermissions;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.opendatakit.security.common.GrantedAuthorityName;
import org.opendatakit.security.server.SecurityServiceUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;

@Api(authorizations = {@Authorization(value = "basicAuth")})
public class RealizedTableService {
  private static final Log logger = LogFactory.getLog(RealizedTableService.class);

  private final ServletContext sc;
  private final HttpServletRequest req;
  private final HttpHeaders headers;
  private final UriInfo info;
  private final String appId;
  private final String tableId;
  private final String schemaETag;
  private final boolean notActiveSchema;
  private final TablesUserPermissions userPermissions;
  private final TableManager tm;
  private final CallingContext cc;

  public RealizedTableService(ServletContext sc, HttpServletRequest req, HttpHeaders headers,
      UriInfo info, String appId, String tableId, String schemaETag, boolean notActiveSchema,
      TablesUserPermissions userPermissions, TableManager tm, CallingContext cc)
      throws ODKEntityNotFoundException, ODKDatastoreException {
    this.sc = sc;
    this.req = req;
    this.headers = headers;
    this.info = info;
    this.appId = appId;
    this.tableId = tableId;
    this.schemaETag = schemaETag;
    this.notActiveSchema = notActiveSchema;
    this.userPermissions = userPermissions;
    this.tm = tm;
    this.cc = cc;
  }

  /**
   * Delete a realized tableId and all its data (supplied in implementation constructor)
   *
   * @return successful status code if successful.
   * @throws ODKDatastoreException
   * @throws ODKTaskLockException
   * @throws PermissionDeniedException
   */
  @DELETE
  public Response deleteTable()
      throws ODKDatastoreException, ODKTaskLockException, PermissionDeniedException {

    TreeSet<GrantedAuthorityName> ui = SecurityServiceUtil.getCurrentUserSecurityInfo(cc);
    if (!ui.contains(GrantedAuthorityName.ROLE_ADMINISTER_TABLES)) {
      throw new PermissionDeniedException("User does not belong to the 'Administer Tables' group");
    }

    tm.deleteTable(tableId);

    return Response.ok()
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  /**
   * Data row subresource for a realized tableId (supplied in implementation constructor)
   *
   * @return {@link DataService} for manipulating row data in this table.
   * @throws ODKDatastoreException
   * @throws SchemaETagMismatchException
   * @throws PermissionDeniedException
   * @throws AppNameMismatchException
   * @throws ODKTaskLockException
   * @throws TableNotFoundException
   */
  @Path("rows")
  public DataService getData()
      throws ODKDatastoreException, PermissionDeniedException, SchemaETagMismatchException,
      AppNameMismatchException, ODKTaskLockException, TableNotFoundException {

    if (notActiveSchema) {
      throw new TableNotFoundException(TableService.ERROR_TABLE_NOT_FOUND + "\n" + tableId);
    }
    DataService service = new DataService(appId, tableId, schemaETag, info, userPermissions, cc);
    return service;
  }

  /**
   * Differences subresource for a realized tableId (supplied in implementation constructor)
   *
   * @return {@link DiffService} for the row-changes on this table.
   * @throws ODKDatastoreException
   * @throws SchemaETagMismatchException
   * @throws PermissionDeniedException
   * @throws AppNameMismatchException
   * @throws ODKTaskLockException
   * @throws TableNotFoundException
   */
  @Path("diff")
  public DiffService getDiff()
      throws ODKDatastoreException, PermissionDeniedException, SchemaETagMismatchException,
      AppNameMismatchException, ODKTaskLockException, TableNotFoundException {

    if (notActiveSchema) {
      throw new TableNotFoundException(TableService.ERROR_TABLE_NOT_FOUND + "\n" + tableId);
    }
    DiffService service = new DiffService(appId, tableId, schemaETag, info, userPermissions, cc);
    return service;
  }

  /**
   * Differences subresource for a realized tableId (supplied in implementation constructor)
   *
   * @return {@link QueryService} for the row-changes on this table.
   * @throws ODKDatastoreException
   * @throws SchemaETagMismatchException
   * @throws PermissionDeniedException
   * @throws AppNameMismatchException
   * @throws ODKTaskLockException
   * @throws TableNotFoundException
   */
  @Path("query")
  public QueryService getQuery()
      throws ODKDatastoreException, PermissionDeniedException, SchemaETagMismatchException,
      AppNameMismatchException, ODKTaskLockException, TableNotFoundException {

    if (notActiveSchema) {
      throw new TableNotFoundException(TableService.ERROR_TABLE_NOT_FOUND + "\n" + tableId);
    }
    QueryService service = new QueryService(appId, tableId, schemaETag, info, userPermissions, cc);
    return service;
  }

  /**
   * Exposed only to provide the attachments URL in the TableResource
   *
   * @return throws PermissionDeniedException if called.
   * @throws PermissionDeniedException
   */
  @Path("attachments")
  public InstanceFileService getInstanceFileService() throws PermissionDeniedException {
    throw new PermissionDeniedException("rowId is required");
  }


  /**
   * There is already a row-by-row manifest at attachments/{rowId}/manifest
   * This shows the list of attachments for a table, similar to what you see in the aggregate interface
   * This was ported from Open Data Kit Aggregate's ServerDataServiceImpl.getInstanceFileInfoContents
   * @param httpHeaders
   * @return
   * @throws IOException
   * @throws ODKTaskLockException
   * @throws PermissionDeniedException
   * @throws ODKDatastoreException 
   */
  @GET
  @Path("attachments/manifest")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getManifest(@Context HttpHeaders httpHeaders)
      throws IOException, ODKTaskLockException, PermissionDeniedException, ODKDatastoreException {
    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(OdkTables.class);
    ub.path(OdkTables.class, "getTablesService");


    DbTableInstanceFiles blobStore = new DbTableInstanceFiles(tableId, cc);
    List<BinaryContent> contents = blobStore.getAllBinaryContents(cc);

    ArrayList<OdkTablesFileManifestEntry> completedSummaries = new ArrayList<OdkTablesFileManifestEntry>();
    for (BinaryContent entry : contents) {
      if (entry.getUnrootedFilePath() == null) {
        continue;
      }
      String rowId = entry.getTopLevelAuri();
      UriBuilder tmp = ub.clone().path(TableService.class, "getRealizedTable")
          .path(RealizedTableService.class, "getInstanceFiles")
          .path(InstanceFileService.class, "getFile");
      URI getFile = tmp.build(appId, tableId, schemaETag, rowId, entry.getUnrootedFilePath());
      String downloadUrl = getFile.toASCIIString() + "?" + FileService.PARAM_AS_ATTACHMENT
          + "=true";
      OdkTablesFileManifestEntry manifestEntry = new OdkTablesFileManifestEntry();
      manifestEntry.downloadUrl = downloadUrl;
      manifestEntry.contentLength = entry.getContentLength();
      manifestEntry.contentType = entry.getContentType();
      manifestEntry.filename = entry.getUnrootedFilePath();
      manifestEntry.md5hash = entry.getContentHash();
      completedSummaries.add(manifestEntry);
      
    }

    OdkTablesFileManifest manifest = new OdkTablesFileManifest(completedSummaries);
    
    ResponseBuilder rBuild = Response.ok(manifest).header(HttpHeaders.ETAG, "test")
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true");
    return rBuild.build();

  }



  /**
   * Instance file subresource for a realized tableId and (supplied in implementation constructor)
   *
   * @param rowId
   * @return {@link InstanceFileService} for file attachments to the rows on this table.
   * @throws ODKDatastoreException
   * @throws SchemaETagMismatchException
   * @throws PermissionDeniedException
   * @throws AppNameMismatchException
   * @throws ODKTaskLockException
   * @throws TableNotFoundException
   */
  @Path("attachments/{rowId}")
  public InstanceFileService getInstanceFiles(@PathParam("rowId") String rowId)
      throws ODKDatastoreException, PermissionDeniedException, SchemaETagMismatchException,
      AppNameMismatchException, ODKTaskLockException, TableNotFoundException {

    if (notActiveSchema) {
      throw new TableNotFoundException(TableService.ERROR_TABLE_NOT_FOUND + "\n" + tableId);
    }
    InstanceFileService service =
        new InstanceFileService(appId, tableId, schemaETag, rowId, info, userPermissions, cc);
    return service;
  }

  /**
   * Get the definition of a realized tableId (supplied in implementation constructor)
   *
   * @return {@link TableDefinitionResource} for the schema of this table.
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws ODKTaskLockException
   * @throws AppNameMismatchException
   * @throws TableNotFoundException
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})

  public Response getDefinition() throws ODKDatastoreException, PermissionDeniedException,
      ODKTaskLockException, AppNameMismatchException, TableNotFoundException {

    if (notActiveSchema) {
      throw new TableNotFoundException(TableService.ERROR_TABLE_NOT_FOUND + "\n" + tableId);
    }
    TableDefinition definition = tm.getTableDefinition(tableId);
    TableDefinitionResource definitionResource = new TableDefinitionResource(definition);
    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(OdkTables.class);
    ub.path(OdkTables.class, "getTablesService");
    URI selfUri =
        ub.clone().path(TableService.class, "getRealizedTable").build(appId, tableId, schemaETag);
    URI tableUri = ub.clone().build(appId, tableId);
    try {
      definitionResource.setSelfUri(selfUri.toURL().toExternalForm());
      definitionResource.setTableUri(tableUri.toURL().toExternalForm());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("Unable to convert to URL");
    }
    return Response.ok(definitionResource)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

}
