package org.opendatakit.api.odktables;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.constants.WebConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.datamodel.BinaryContent;
import org.opendatakit.datamodel.BinaryContentManipulator;
import org.opendatakit.datamodel.BinaryContentRefBlob;
import org.opendatakit.datamodel.RefBlob;
import org.opendatakit.odktables.ODKTablesExportHelper;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.relation.DbTableInstanceFiles;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKTaskLockException;

import io.swagger.annotations.Api;
import io.swagger.annotations.Authorization;

@Api(description = "Export ODK Form Data", authorizations = {@Authorization(value = "basicAuth")})
public class ExportService {
  private static final Log logger = LogFactory.getLog(ExportService.class);

  // Used to create the names of tables storing blobs within the database
  private static final String TABLE_PREFIX = "___";
  private static final String TABLE_REF_SUFFIX = "_att_ref";
  private static final String TABLE_BLB_SUFFIX = "_att_blb";

  String appId;
  String tableId;
  String exportFormat;
  CallingContext callingContext;
  HttpServletResponse res;

  public ExportService(HttpServletResponse res, String appId, String tableId, String exportFormat,
      CallingContext callingContext) {
    super();
    this.appId = appId;
    this.tableId = tableId;
    this.exportFormat = exportFormat;
    this.callingContext = callingContext;
    this.res = res;
  }

  @GET
  @Produces({"application/zip"})
  @Path("showDeleted/{showDeleted}")
  public void exportDataShowDeleted(@PathParam("showDeleted") boolean showDeleted)
      throws ODKDatastoreException, PermissionDeniedException, IOException {

    ODKTablesExportHelper odkTablesExportHelper = null;

    try {
      odkTablesExportHelper = new ODKTablesExportHelper(tableId, callingContext);
    } catch (ODKDatastoreException e) {
      logger.error(e);
    } catch (PermissionDeniedException e) {
      logger.error(e);
    } catch (ODKTaskLockException e) {
      logger.error(e);
    }

    // Load all submitted instances from a specific ODK Table
    byte[] tableEntries = null;

    if (exportFormat.equals(WebConsts.JSON)) {
      logger.info("Exporting table: " + tableId + " entries to JSON file");
      tableEntries = odkTablesExportHelper.exportData(WebConsts.JSON, showDeleted);
    }
    if (exportFormat.equals(WebConsts.CSV)) {
      logger.info("Exporting table: " + tableId + " entries to CSV file");
      tableEntries = odkTablesExportHelper.exportData(WebConsts.CSV, showDeleted);
    }

    // List of files (blobs) associated with a certain table
    List<BinaryContent> contents = null;
    try {
      DbTableInstanceFiles blobStore = new DbTableInstanceFiles(tableId, callingContext);
      contents = blobStore.getAllBinaryContents(callingContext);
    } catch (ODKDatastoreException e) {
      logger.error(e);
    }

    // Prepare response containing a ZIP file
    ServletOutputStream servletOutputStream = res.getOutputStream();
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);
    try {
      if (contents == null && tableEntries == null) {
        logger.error("Unrecognised file format or failed file preparation");
      } else {
        logger.info("Compressing table: " + tableId + " data to the archive file");

        // Prepare timestamp
        String timeStamp =
            new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        // Zip blob files associated with the table
        zipBlobFiles(zipOutputStream, contents, callingContext, tableId);

        // Create name for a CSV/JSON file
        String fileName = null;
        if (exportFormat.equals(WebConsts.JSON)) {
          fileName = tableId + "_" + timeStamp + ".json";
        }
        if (exportFormat.equals(WebConsts.CSV)) {
          fileName = tableId + "_" + timeStamp + ".csv";
        }

        // Zip CSV/JSON file containing all instances of a particular survey from an ODK Table
        zipFile(zipOutputStream, fileName, tableEntries);

        zipOutputStream.finish();
        byteArrayOutputStream.flush();

        res.setContentType("application/zip");
        res.setHeader("Content-Disposition",
            "attachment; filename=" + tableId + "_" + timeStamp + ".zip");

        servletOutputStream.write(byteArrayOutputStream.toByteArray());
        servletOutputStream.flush();
      }
    } catch (Exception e) {
      logger.error(e);
    } finally {
      try {
        zipOutputStream.close();
        byteArrayOutputStream.close();
        servletOutputStream.close();
      } catch (IOException e) {
        logger.error(e);
      }
    }
  }

  @GET
  @Produces({"application/zip"})
  public void exportData() throws ODKDatastoreException, PermissionDeniedException, IOException {
    exportDataShowDeleted(false);
  }

  private void zipBlobFiles(ZipOutputStream zos, List<BinaryContent> contents,
      CallingContext callingContext, String tableId) throws IOException, ODKDatastoreException {

    try {
      String schemaName = callingContext.getDatastore().getDefaultSchemaName();
      String tableName = tableId.toLowerCase();

      for (BinaryContent binaryContent : contents) {

        // Create BlobManipulator to read the blob from the database
        BinaryContentManipulator.BlobManipulator blobManipulator =
            new BinaryContentManipulator.BlobManipulator(binaryContent.getUri(),
                new BinaryContentRefBlob(schemaName, TABLE_PREFIX + tableName + TABLE_REF_SUFFIX),
                new RefBlob(schemaName, TABLE_PREFIX + tableName + TABLE_BLB_SUFFIX),
                callingContext);

        // Create file with the original file name and
        zipFile(zos, binaryContent.getUnrootedFilePath(), blobManipulator.getBlob());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void zipFile(ZipOutputStream zos, String fileName, byte[] fileContent)
      throws IOException {
    logger.info("Compressing file: " + fileName);
    zos.putNextEntry(new ZipEntry(fileName));
    zos.write(fileContent);
    zos.closeEntry();
  }
}
