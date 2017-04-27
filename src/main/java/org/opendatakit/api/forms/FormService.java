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
package org.opendatakit.api.forms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.ContextUtils;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.api.forms.entity.FormUploadResult;
import org.opendatakit.api.odktables.FileManifestService;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.constants.MimeTypes;
import org.opendatakit.constants.WebConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.odktables.ConfigFileChangeDetail;
import org.opendatakit.odktables.FileContentInfo;
import org.opendatakit.odktables.FileManager;
import org.opendatakit.odktables.FileManifestManager;
import org.opendatakit.odktables.TableManager;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.exception.TableAlreadyExistsException;
import org.opendatakit.odktables.security.TablesUserPermissions;
import org.opendatakit.odktables.util.ServiceUtils;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.springframework.beans.factory.annotation.Autowired;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "/forms", description = "ODK Form Definition API",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("/forms")
public class FormService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(FormService.class);

  @GET
  public Response doGet(@Context HttpServletRequest req, @Context HttpServletResponse resp)
      throws IOException {
    return Response.ok("Hello forms.").build();
  }

  @POST
  @ApiOperation(
      value = "This API operation is currently being written. Upload a zipped form definition.",
      response = FormUploadResult.class)
  @Consumes({MediaType.MULTIPART_FORM_DATA})
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @Path("{appId}/{odkClientVersion}")
  public Response doPost(@Context HttpServletRequest req, @Context HttpServletResponse resp,
      @PathParam("odkClientVersion") String odkClientVersion, @PathParam("appId") String appId,
      @Context UriInfo info) throws IOException {
    ServiceUtils.examineRequest(req.getServletContext(), req);

    req.getContentLength();
    if (!ServletFileUpload.isMultipartContent(req)) {
      throw new WebApplicationException(ErrorConsts.NO_MULTI_PART_CONTENT,
          HttpServletResponse.SC_BAD_REQUEST);
    }

    try {
      TablesUserPermissions userPermissions = ContextUtils.getTablesUserPermissions(callingContext);
      List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
      Map<String, byte[]> files = new HashMap<>();
      String definition = null;
      String tableId = null;
      List<String> regionalOffices = new ArrayList<>();

      // unzipping files

      for (FileItem item : items) {

        // Retrieve all Regional Office IDs to which a form definition
        // is going to be assigned to
        if (item.getFieldName().equals(WebConsts.OFFICE_ID)) {
          regionalOffices.add(item.getString());
          logger.info("Form definition would be assigned to office with ID: " + item.getString());
        }

        String fieldName = item.getFieldName();
        String fileName = FilenameUtils.getName(item.getName());

        if (fieldName.equals(WebConsts.ZIP_FILE)) {

          if (!(fileName.endsWith(".zip"))) {
            throw new WebApplicationException(ErrorConsts.NO_ZIP_FILE,
                HttpServletResponse.SC_BAD_REQUEST);
          }

          InputStream fileStream = item.getInputStream();
          ZipInputStream zipStream = new ZipInputStream(fileStream);

          int c;

          byte buffer[] = new byte[2084];
          ByteArrayOutputStream tempBAOS;
          ZipEntry zipEntry;
          while ((zipEntry = zipStream.getNextEntry()) != null) {
            if (!(zipEntry.isDirectory())) {
              tempBAOS = new ByteArrayOutputStream();
              while ((c = zipStream.read(buffer, 0, 2048)) > -1) {
                tempBAOS.write(buffer, 0, c);
              }
              files.put("tables" + BasicConsts.FORWARDSLASH + zipEntry.getName(),
                  tempBAOS.toByteArray());
              if (zipEntry.getName().endsWith("definition.csv")) {
                tableId = FileManager.getTableIdForFilePath(
                    "tables" + BasicConsts.FORWARDSLASH + zipEntry.getName());
                definition = new String(tempBAOS.toByteArray());
              }
            }
          }
        }
      }

      if (definition == null || tableId == null || regionalOffices.isEmpty()) {
        throw new WebApplicationException(ErrorConsts.NO_DEFINITION_FILE,
            HttpServletResponse.SC_BAD_REQUEST);
      }

      List<String> notUploadedFiles = new ArrayList<>();
      List<String> uploadedFiles = new ArrayList<>();

      // adding tables
      List<Column> columns = parseColumnsFromCsv(definition);
      TableManager tm = new TableManager(appId, userPermissions, callingContext);
      tm.createTable(tableId, columns, regionalOffices);

      // uploading files
      for (Map.Entry<String, byte[]> entry : files.entrySet()) {
        String contentType =
            MimeTypes.MIME_TYPES.get(entry.getKey().substring(entry.getKey().lastIndexOf(".") + 1));
        if (contentType == null) {
          contentType = "application/octet-stream";
        }

        FileManager fm = new FileManager(appId, callingContext);
        FileContentInfo fi = new FileContentInfo(entry.getKey(), contentType,
            Long.valueOf(entry.getValue().length), null, entry.getValue());

        ConfigFileChangeDetail outcome =
            fm.putFile(odkClientVersion, tableId,
                fi, userPermissions);

        if (outcome == ConfigFileChangeDetail.FILE_NOT_CHANGED) {
          notUploadedFiles.add(entry.getKey());
        } else {
          uploadedFiles.add(entry.getKey());
        }
      }

      FileManifestManager manifestManager = new FileManifestManager(appId,
          odkClientVersion, callingContext);
      OdkTablesFileManifest manifest = manifestManager.getManifestForTable(tableId);
      FileManifestService.fixDownloadUrls(info, appId, odkClientVersion, manifest);

      FormUploadResult formUploadResult = new FormUploadResult();
      formUploadResult.setNotProcessedFiles(notUploadedFiles);
      formUploadResult.setManifest(manifest);
      String eTag = Integer.toHexString(manifest.hashCode()); // Is this
                                                              // right?


      return Response.status(Status.CREATED).entity(formUploadResult).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();

    } catch (FileUploadException | ODKDatastoreException | ODKTaskLockException
        | PermissionDeniedException | TableAlreadyExistsException e) {
      logger.error("error uploading zip", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    }
  }

  private List<Column> parseColumnsFromCsv(String definition) {
    List<Column> outcome = new ArrayList<>();
    String columnsStrings[] = definition.split("\\s+");
    Column temp;
    for (String column : columnsStrings) {
      if (column.startsWith("_")) {
        continue;
      }
      String fields[] = column.split(",", 4);
      temp = new Column(fields[0], fields[1], fields[2],
          fields[3].replaceAll("\"\"", "\"").replaceAll("\\]\"", "\\]").replaceAll("\"\\[", "\\["));
      outcome.add(temp);
    }

    return outcome;
  }

}
