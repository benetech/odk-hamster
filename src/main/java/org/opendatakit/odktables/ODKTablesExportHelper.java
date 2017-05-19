package org.opendatakit.odktables;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.lang3.CharEncoding;
import org.opendatakit.aggregate.odktables.rest.RFC4180CsvWriter;
import org.opendatakit.aggregate.odktables.rest.entity.DataKeyValue;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.TableEntry;
import org.opendatakit.constants.WebConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.odktables.constants.ODKDefaultColumnNames;
import org.opendatakit.odktables.exception.BadColumnNameException;
import org.opendatakit.odktables.exception.InconsistentStateException;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.relation.DbColumnDefinitions;
import org.opendatakit.odktables.security.TablesUserPermissions;
import org.opendatakit.odktables.security.TablesUserPermissionsImpl;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.opendatakit.persistence.table.ServerPreferencesPropertiesTable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Helper class for exporting data from ODK Tables to CSV and JSON files. It is responsible for
 * loading data from the database and holding credentials of the user which wants to export the data.
 */

public class ODKTablesExportHelper {

    private String appId;
    private String tableId;
    private CallingContext callingContext;
    private TablesUserPermissions tablesUserPermissions;
    private TableManager tableManager;
    private DataManager dataManager;
    private DataManager.WebsafeRows websafeRows;
    private ArrayList<String> userDefinedColumnNames;

    public ODKTablesExportHelper(String tableId, CallingContext callingContext) throws ODKEntityNotFoundException,
            ODKOverQuotaException, ODKDatastoreException, PermissionDeniedException, ODKTaskLockException {
        this.tableId = tableId;
        this.callingContext = callingContext;
        this.appId = ServerPreferencesPropertiesTable.getOdkTablesAppId(callingContext);
        this.tablesUserPermissions = new TablesUserPermissionsImpl(callingContext);
        this.tableManager = new TableManager(appId, tablesUserPermissions, callingContext);
        this.dataManager = new DataManager(appId, tableId, tablesUserPermissions, callingContext);
    }

    /**
     * Loads user defined column names from the database.
     *
     * @return boolean if the load was performed successfully
     * @throws ODKDatastoreException
     * @throws PermissionDeniedException
     */
    public void loadUserDefinedColumnNames() throws ODKDatastoreException, PermissionDeniedException {
        TableEntry entry = tableManager.getTable(tableId);
        if ( entry == null || entry.getSchemaETag() == null ) {
            throw new ODKEntityNotFoundException();
        }
        this.userDefinedColumnNames = DbColumnDefinitions.queryForDbColumnNames(tableId,
                entry.getSchemaETag(), callingContext);
    }

    /**
     * Loads data rows from a particular table from the database.
     *
     * @return boolean if the data load was performed successfully
     * @throws ODKDatastoreException
     * @throws PermissionDeniedException
     * @throws ODKTaskLockException
     * @throws InconsistentStateException
     * @throws BadColumnNameException
     */
    public void loadWebsafeRows(boolean showDeleted) throws ODKDatastoreException, PermissionDeniedException, ODKTaskLockException,
            InconsistentStateException, BadColumnNameException {
        // null - fetch data from the beginning
        // 0 - unlimited number of rows
        this.websafeRows = this.dataManager.getRows(null, 0, null, false, null, null, showDeleted);
    }

    public String getAppId() {
        return this.appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * Gets all user defined column names
     *
     * @return ArrayList<String> of user defined column names
     */

    public ArrayList<String> getUserDefinedColumnNames() {
        return this.userDefinedColumnNames;
    }

    /**
     * Gets the wrapper class DataManager.WebsafeRows containing data rows
     *
     * @return DataManager.WebsafeRows which contains data rows
     */
    public DataManager.WebsafeRows getWebsafeRows() {
        return this.websafeRows;
    }


    public byte[] exportData(String fileFormat, boolean showDeleted) throws JsonProcessingException{
        try {
            loadWebsafeRows(showDeleted);
            if(fileFormat.equals(WebConsts.CSV)){
                loadUserDefinedColumnNames();
            }
        } catch (ODKDatastoreException e) {
            e.printStackTrace();
        } catch (PermissionDeniedException e) {
            e.printStackTrace();
        } catch (ODKTaskLockException e) {
            e.printStackTrace();
        } catch (InconsistentStateException e) {
            e.printStackTrace();
        } catch (BadColumnNameException e) {
            e.printStackTrace();
        }
        if(fileFormat.equals(WebConsts.CSV))
            return prepareCSV();
        else if(fileFormat.equals(WebConsts.JSON))
            return prepareJSON();
        else
            return new byte[0];

    }

    private byte[] prepareCSV(){
        // Begin creation of the CSV file
        StringWriter buffer = new StringWriter();

        // Writes one row at a time with data separated with tab key
        RFC4180CsvWriter writer = new RFC4180CsvWriter(buffer);

        // Write column headings
        ArrayList<String> columnHeadings = new ArrayList<String>();
        columnHeadings.add(ODKDefaultColumnNames.DELETE_ROW_HEADING);
        for ( String columnName : userDefinedColumnNames) {
            columnHeadings.add(columnName);
        }
        columnHeadings.add(ODKDefaultColumnNames.SAVEPOINT_TYPE);
        columnHeadings.add(ODKDefaultColumnNames.FORM_ID);
        columnHeadings.add(ODKDefaultColumnNames.LOCALE);
        columnHeadings.add(ODKDefaultColumnNames.SAVEPOINT_TIMESTAMP);
        columnHeadings.add(ODKDefaultColumnNames.SAVEPOINT_CREATOR);
        columnHeadings.add(ODKDefaultColumnNames.ROW_ID);
        columnHeadings.add(ODKDefaultColumnNames.ROW_ETAG);
        columnHeadings.add(ODKDefaultColumnNames.FILTER_TYPE);
        columnHeadings.add(ODKDefaultColumnNames.FILTER_VALUE);
        columnHeadings.add(ODKDefaultColumnNames.LAST_UPDATE_USER);
        columnHeadings.add(ODKDefaultColumnNames.CREATED_BY_USER);
        columnHeadings.add(ODKDefaultColumnNames.DATA_ETAG_AT_MODIFICATION);

        try {
            writer.writeNext(columnHeadings.toArray(new String[0]));

            //Write all the data to the specific columns
            ArrayList<String> dataRow = new ArrayList<String>();
            for (Row row: websafeRows.rows) {
                // Is row deleted
                dataRow.add(String.valueOf(row.isDeleted()));

                // User defined column values
                for (DataKeyValue keyValue : row.getValues()) {
                    dataRow.add(keyValue.value);
                }

                // Default columns
                dataRow.add(row.getSavepointType());
                dataRow.add(row.getFormId());
                dataRow.add(row.getLocale());
                dataRow.add(row.getSavepointTimestamp());
                dataRow.add(row.getSavepointCreator());
                dataRow.add(row.getRowId());
                dataRow.add(row.getRowETag());
                dataRow.add(row.getRowFilterScope().getType().name());
                dataRow.add(row.getRowFilterScope().getValue());
                dataRow.add(row.getLastUpdateUser());
                dataRow.add(row.getCreateUser());
                dataRow.add(row.getDataETagAtModification());

                writer.writeNext(dataRow.toArray(new String[0]));
                dataRow.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Prepare response with a generated .csv file
        try {
            return buffer.getBuffer().toString().getBytes(CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    private byte[] prepareJSON() throws JsonProcessingException{
        // Convert the data from rows to .json
        
        ObjectMapper mapper = new ObjectMapper();
        String parsedJson = mapper.writeValueAsString(websafeRows) ; 

        FileWriter writer = null;
        try {
            writer = new FileWriter(tableId + ".json", false);
            writer.write(parsedJson);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            return parsedJson.getBytes(CharEncoding.UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }


}
