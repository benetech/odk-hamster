package org.opendatakit.persistence.table;




import java.util.List;

import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.CommonFieldsBase;
import org.opendatakit.persistence.DataField;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.Query;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityPersistException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.security.User;

/**
 * Created by user on 13.01.17.
 */
public class OdkRegionalOfficeTable extends CommonFieldsBase {

    private static final String TABLE_NAME = "_odktables_regional_offices";

    private static final DataField REGIONAL_OFFICE_ID = new DataField("REGIONAL_OFFICE_ID",
            DataField.DataType.STRING, true);

    private static final DataField REGIONAL_OFFICE_NAME = new DataField("REGIONAL_OFFICE_NAME",
            DataField.DataType.STRING, true);


    private OdkRegionalOfficeTable(String schemaName) {
        super(schemaName, TABLE_NAME);
        fieldList.add(REGIONAL_OFFICE_ID);
        fieldList.add(REGIONAL_OFFICE_NAME);
    }


    public OdkRegionalOfficeTable(OdkRegionalOfficeTable ref, User user) {
        super(ref, user);
    }

    @Override
    public CommonFieldsBase getEmptyRow(User user) {
        return new OdkRegionalOfficeTable(this, user);
    }

    public void persist(CallingContext cc) throws ODKEntityPersistException, ODKOverQuotaException {
        Datastore ds = cc.getDatastore();
        User user = cc.getCurrentUser();

        ds.putEntity(this, user);
    }

    private static OdkRegionalOfficeTable relation = null;

    public static synchronized final OdkRegionalOfficeTable assertRelation(CallingContext cc)
            throws ODKDatastoreException {
        if (relation == null) {
            OdkRegionalOfficeTable relationPrototype;
            Datastore ds = cc.getDatastore();
            User user = cc.getUserService().getDaemonAccountUser();
            relationPrototype = new OdkRegionalOfficeTable(ds.getDefaultSchemaName());
            ds.assertRelation(relationPrototype, user); // may throw exception...
            // at this point, the prototype has become fully populatedgit a
            relation = relationPrototype; // set static variable only upon success...
        }
        return relation;
    }

    public String getRegionalOfficeId() {
        return getStringField(REGIONAL_OFFICE_ID);
    }

    public String getRegionalOfficeName() {
        return getStringField(REGIONAL_OFFICE_NAME);
    }

    /**
     * Set the Regional Office ID
     *
     * @ throws IllegalArgumentException if the value cannot be set
     */
    public void setRegionalOfficeId(String regionalOfficeID) {
        if (!setStringField(REGIONAL_OFFICE_ID, regionalOfficeID)) {
            throw new IllegalArgumentException("overflow regionalOfficeID");
        }
    }

    /**
     * Set the Regional Office Name
     *
     * @ throws IllegalArgumentException if the value cannot be set
     */
    public void setRegionalOfficeName(String regionalOfficeName) {
        if (!setStringField(REGIONAL_OFFICE_NAME, regionalOfficeName)) {
            throw new IllegalArgumentException("overflow regionalOfficeName");
        }
    }

    public static final OdkRegionalOfficeTable getRecordFromDatabase(String officeId, CallingContext cc) throws DatastoreFailureException
    {
        Datastore ds = cc.getDatastore();
        try {
            OdkRegionalOfficeTable prototype = OdkRegionalOfficeTable.assertRelation(cc);

            Query q = ds.createQuery(prototype,
                    "OdkRegionalOfficeTable.getAllOffices", cc.getCurrentUser());

            q.addFilter(REGIONAL_OFFICE_ID, Query.FilterOperation.EQUAL, officeId);
            List<? extends CommonFieldsBase> results = q.executeQuery();

            if (results.size() == 1) {
                return (OdkRegionalOfficeTable) results.get(0);
            }

        } catch (ODKDatastoreException e) {
            e.printStackTrace();
            throw new DatastoreFailureException(e);
        }
        return null;
    }


}
