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

package org.opendatakit.aggregate.odktables.api;

import org.opendatakit.aggregate.odktables.DataManager;
import org.opendatakit.aggregate.odktables.DataManager.WebsafeRows;
import org.opendatakit.aggregate.odktables.exception.BadColumnNameException;
import org.opendatakit.aggregate.odktables.exception.InconsistentStateException;
import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.exception.TableDataETagMismatchException;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowList;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissions;
import org.opendatakit.common.persistence.QueryResumePoint;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.utils.WebUtils;
import org.opendatakit.common.web.CallingContext;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class DataService {

  public static final String QUERY_ROW_ETAG = "row_etag";
  public static final String CURSOR_PARAMETER = "cursor";
  public static final String FETCH_LIMIT = "fetchLimit";
  public static final String DEVICE_ID = "deviceId";
  public static final String OFFICE_ID = "officeId";
  public static final String SORT_COLUMN = "sortColumn";
  public static final String ASCENDING = "ascending";

  private final String schemaETag;
  private final DataManager dm;
  private final UriInfo info;

  public DataService(String appId, String tableId, String schemaETag, UriInfo info,
      TablesUserPermissions userPermissions, CallingContext cc)
      throws ODKEntityNotFoundException, ODKDatastoreException {
    this.schemaETag = schemaETag;
    this.dm = new DataManager(appId, tableId, userPermissions, cc);
    this.info = info;
  }

  /**
   * Get all data rows.
   * 
   * @param cursor - null or a websafeCursor value from the RowResourceList of a previous call
   * @param fetchLimit - null or the number of rows to fetch. If null, server will choose the limit.
   * @return {@link RowResourceList} containing the rows being returned.
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws InconsistentStateException
   * @throws ODKTaskLockException
   * @throws BadColumnNameException
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* RowResourceList */ getRows(@QueryParam(CURSOR_PARAMETER) String cursor,
      @QueryParam(FETCH_LIMIT) String fetchLimit, @QueryParam(SORT_COLUMN) String sortColumn,
      @QueryParam(ASCENDING) Boolean ascending, @QueryParam(DEVICE_ID) String deviceId,
      @QueryParam(OFFICE_ID) String officeId)
      throws ODKDatastoreException, PermissionDeniedException, InconsistentStateException,
      ODKTaskLockException, BadColumnNameException {
    int limit =
        (fetchLimit == null || fetchLimit.length() == 0) ? 2000 : Integer.parseInt(fetchLimit);
    boolean asc = ascending == null ? false : ascending;
    WebsafeRows websafeResult =
        dm.getRows(QueryResumePoint.fromWebsafeCursor(WebUtils.safeDecode(cursor)), limit,
            sortColumn, asc, deviceId, officeId);
    RowResourceList rowResourceList =
        new RowResourceList(getResources(websafeResult.rows), websafeResult.dataETag, getTableUri(),
            WebUtils.safeEncode(websafeResult.websafeRefetchCursor),
            WebUtils.safeEncode(websafeResult.websafeBackwardCursor),
            WebUtils.safeEncode(websafeResult.websafeResumeCursor), websafeResult.hasMore,
            websafeResult.hasPrior);
    return Response.ok(rowResourceList)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  /**
   * API for creating, updating or deleting rows.
   * 
   * This API will return 409 (Conflict) if the RowList dataETag does not match the current dataETag
   * for this table.
   * 
   * @param rows
   * @return {@link RowOutcomeList} of the newly added/modified/deleted rows.
   * @throws ODKTaskLockException
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws BadColumnNameException
   * @throws InconsistentStateException
   * @throws TableDataETagMismatchException
   */
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* RowOutcomeList */ alterRows(RowList rows)
      throws ODKTaskLockException, ODKDatastoreException, PermissionDeniedException,
      BadColumnNameException, InconsistentStateException, TableDataETagMismatchException {

    RowOutcomeList outcomes = dm.insertOrUpdateRows(rows);
    updateTableUri(outcomes);
    return Response.ok(outcomes)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  /**
   * Get the current values for a single rowId
   * 
   * @param rowId
   * @return {@link RowResource} of the row
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws InconsistentStateException
   * @throws ODKTaskLockException
   * @throws BadColumnNameException
   */
  @GET
  @Path("{rowId}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response /* RowResource */ getRow(@PathParam("rowId") String rowId)
      throws ODKDatastoreException, PermissionDeniedException, InconsistentStateException,
      ODKTaskLockException, BadColumnNameException {
    Row row = dm.getRow(rowId);
    RowResource resource = getResource(row);
    return Response.ok(resource)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  private String getTableUri() {
    String appId = dm.getAppId();
    String tableId = dm.getTableId();

    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(OdkTables.class, "getTablesService");
    URI table = ub.clone().build(appId, tableId);
    try {
      return table.toURL().toExternalForm();
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("unable to convert URL ");
    }
  }

  private RowResource getResource(Row row) {
    String appId = dm.getAppId();
    String tableId = dm.getTableId();
    String rowId = row.getRowId();

    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(OdkTables.class, "getTablesService");
    URI self = ub.clone().path(TableService.class, "getRealizedTable")
        .path(RealizedTableService.class, "getData").path(DataService.class, "getRow")
        .build(appId, tableId, schemaETag, rowId);
    RowResource resource = new RowResource(row);
    try {
      resource.setSelfUri(self.toURL().toExternalForm());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("unable to convert URL ");
    }
    return resource;
  }

  private ArrayList<RowResource> getResources(List<Row> rows) {
    ArrayList<RowResource> resources = new ArrayList<RowResource>();
    for (Row row : rows) {
      resources.add(getResource(row));
    }
    return resources;
  }

  private void updateTableUri(RowOutcomeList outcomeList) {
    String appId = dm.getAppId();
    String tableId = dm.getTableId();
    // for bandwidth efficiency, do not provide selfUri in response array

    UriBuilder ub = info.getBaseUriBuilder();
    ub.path(OdkTables.class, "getTablesService");
    URI table = ub.clone().build(appId, tableId);
    try {
      outcomeList.setTableUri(table.toURL().toExternalForm());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      throw new IllegalArgumentException("unable to convert URL ");
    }
  }
}
