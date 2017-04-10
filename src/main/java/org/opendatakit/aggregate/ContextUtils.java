package org.opendatakit.aggregate;

import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissions;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissionsImpl;
import org.opendatakit.common.persistence.ServerPreferencesProperties;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.context.CallingContextImpl;

public class ContextUtils {


  public static CallingContext duplicateContext(CallingContext context) {
    return new CallingContextImpl(context);
  }

  public static TablesUserPermissions getTablesUserPermissions(CallingContext cc) throws PermissionDeniedException, ODKDatastoreException, ODKTaskLockException {
    return new TablesUserPermissionsImpl(cc);
 }

 public static String getOdkTablesAppId(CallingContext cc) throws ODKEntityNotFoundException, ODKOverQuotaException {
    return ServerPreferencesProperties.getOdkTablesAppId(cc);
 }
}
