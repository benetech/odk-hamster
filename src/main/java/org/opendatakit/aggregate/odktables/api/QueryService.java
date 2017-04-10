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

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.opendatakit.aggregate.odktables.DataManager;
import org.opendatakit.aggregate.odktables.DataManager.WebsafeRows;
import org.opendatakit.aggregate.odktables.exception.BadColumnNameException;
import org.opendatakit.aggregate.odktables.exception.InconsistentStateException;
import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.relation.DbLogTable;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowResource;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissions;
import org.opendatakit.common.persistence.QueryResumePoint;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.utils.WebUtils;
import org.opendatakit.common.web.CallingContext;

public class QueryService {

  public static final String QUERY_DATA_ETAG = "data_etag";
  public static final String QUERY_START_TIME = "startTime";
  public static final String QUERY_END_TIME = "endTime";
  public static final String CURSOR_PARAMETER = "cursor";
  public static final String FETCH_LIMIT = "fetchLimit";

  private final String schemaETag;
  private final DataManager dm;
  private final UriInfo info;

  public QueryService(String appId, String tableId, String schemaETag, UriInfo info,
      TablesUserPermissions userPermissions, CallingContext cc)
      throws ODKEntityNotFoundException, ODKDatastoreException {
    this.schemaETag = schemaETag;
    this.dm = new DataManager(appId, tableId, userPermissions, cc);
    this.info = info;
  }


  /**
   *
   * @param startTime - timestamp in format yyyy-MM-ddTHH:mm:ss.SSSSSSSSS
   * @param endTime - timestamp in format yyyy-MM-ddTHH:mm:ss.SSSSSSSSS
   * @param cursor - null or a websafeCursor value from the RowResourceList of a previous call
   * @param fetchLimit - null or the number of rows to fetch. If null, server will choose the limit.
   * @return {@link RowResourceList} of row changes since the dataETag value
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws InconsistentStateException
   * @throws ODKTaskLockException
   * @throws BadColumnNameException
   */
  @GET
  @Path("lastUpdateDate")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})

  public Response getRowsInTimeRangeBasedOnLastUpdateDate(
      @QueryParam(QUERY_START_TIME) String startTime, @QueryParam(QUERY_END_TIME) String endTime,
      @QueryParam(CURSOR_PARAMETER) String cursor, @QueryParam(FETCH_LIMIT) String fetchLimit)
      throws ODKDatastoreException, PermissionDeniedException, InconsistentStateException,
      ODKTaskLockException, BadColumnNameException, ParseException {
    int limit =
        (fetchLimit == null || fetchLimit.length() == 0) ? 2000 : Integer.valueOf(fetchLimit);
    WebsafeRows websafeResult = dm.getRowsInTimeRange(DbLogTable.LAST_UPDATE_DATE_COLUMN_NAME,
        startTime, endTime, QueryResumePoint.fromWebsafeCursor(WebUtils.safeDecode(cursor)), limit);
    RowResourceList rowResourceList =
        new RowResourceList(getResources(websafeResult.rows), websafeResult.dataETag, getTableUri(),
            WebUtils.safeEncode(websafeResult.websafeRefetchCursor),
            WebUtils.safeEncode(websafeResult.websafeBackwardCursor),
            WebUtils.safeEncode(websafeResult.websafeResumeCursor), websafeResult.hasMore,
            websafeResult.hasPrior);
    return Response.ok(rowResourceList).build();
  }

  /**
   *
   * @param startTime - timestamp in format yyyy-MM-ddTHH:mm:ss.SSSSSSSSS
   * @param endTime - timestamp in format yyyy-MM-ddTHH:mm:ss.SSSSSSSSS
   * @param cursor - null or a websafeCursor value from the RowResourceList of a previous call
   * @param fetchLimit - null or the number of rows to fetch. If null, server will choose the limit.
   * @return {@link RowResourceList} of row changes since the dataETag value
   * @throws ODKDatastoreException
   * @throws PermissionDeniedException
   * @throws InconsistentStateException
   * @throws ODKTaskLockException
   * @throws BadColumnNameException
   */
  @GET
  @Path("savepointTimestamp")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getRowsInTimeRangeBasedOnSavepointTimestamp(
      @QueryParam(QUERY_START_TIME) String startTime, @QueryParam(QUERY_END_TIME) String endTime,
      @QueryParam(CURSOR_PARAMETER) String cursor, @QueryParam(FETCH_LIMIT) String fetchLimit)
      throws ODKDatastoreException, PermissionDeniedException, InconsistentStateException,
      ODKTaskLockException, BadColumnNameException, ParseException {
    int limit =
        (fetchLimit == null || fetchLimit.length() == 0) ? 2000 : Integer.valueOf(fetchLimit);
    WebsafeRows websafeResult = dm.getRowsInTimeRange(DbLogTable.SAVEPOINT_TIMESTAMP.getName(),
        startTime, endTime, QueryResumePoint.fromWebsafeCursor(WebUtils.safeDecode(cursor)), limit);
    RowResourceList rowResourceList =
        new RowResourceList(getResources(websafeResult.rows), websafeResult.dataETag, getTableUri(),
            WebUtils.safeEncode(websafeResult.websafeRefetchCursor),
            WebUtils.safeEncode(websafeResult.websafeBackwardCursor),
            WebUtils.safeEncode(websafeResult.websafeResumeCursor), websafeResult.hasMore,
            websafeResult.hasPrior);
    return Response.ok(rowResourceList).build();
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
}
