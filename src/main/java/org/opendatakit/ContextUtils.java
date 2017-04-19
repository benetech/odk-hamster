package org.opendatakit;

import org.opendatakit.context.CallingContext;
import org.opendatakit.context.CallingContextImpl;
import org.opendatakit.odktables.exception.PermissionDeniedException;
import org.opendatakit.odktables.security.TablesUserPermissions;
import org.opendatakit.odktables.security.TablesUserPermissionsImpl;
import org.opendatakit.persistence.ServerPreferencesProperties;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.persistence.exception.ODKTaskLockException;

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
