/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.persistence.table;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.CommonFieldsBase;
import org.opendatakit.persistence.DataField;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.Query;
import org.opendatakit.persistence.DataField.DataType;
import org.opendatakit.persistence.Query.Direction;
import org.opendatakit.persistence.Query.FilterOperation;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKEntityPersistException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.security.User;
import org.opendatakit.utils.WebUtils;

public class ServerPreferencesPropertiesTable extends CommonFieldsBase {

  private static final String TABLE_NAME = "_server_preferences_properties";

  private static final DataField KEY = new DataField("KEY", DataField.DataType.STRING, true, 128L);

  private static final DataField VALUE = new DataField("VALUE", DataField.DataType.STRING, true,
      20480L);

  // these values are set in the ServiceAccountPrivateKeyUploadServlet
  // and used everywhere else when requesting access
  private static final String GOOGLE_API_CLIENT_ID = "GOOGLE_CLIENT_ID";


  private static final String GOOGLE_SIMPLE_API_KEY = "GOOG_SIMPLE_API_KEY"; // supplied
                                                                             // to
                                                                             // Google
                                                                             // Maps
                                                                             // only

  private static final String ENKETO_API_URL = "ENKETO_API_URL";
  private static final String ENKETO_API_TOKEN = "ENKETO_API_TOKEN";

  // other keys...
  private static final String SITE_KEY = "SITE_KEY";
  private static final String LAST_KNOWN_REALM_STRING = "LAST_KNOWN_REALM_STRING";


  private static final String ODK_TABLES_ENABLED = "ODK_TABLES_ENABLED";
  private static final String FASTER_WATCHDOG_CYCLE_ENABLED = "FASTER_WATCHDOG_CYCLE_ENABLED";
  private static final String FASTER_BACKGROUND_ACTIONS_DISABLED = "FASTER_BACKGROUND_ACTIONS_DISABLED";
  private static final String SKIP_MALFORMED_SUBMISSIONS = "SKIP_MALFORMED_SUBMISSIONS";

  private static final String ODK_TABLES_SEQUENCER_BASE = "ODK_TABLES_SEQUENCER_BASE";
  // there can be only one APP_ID per ODK Aggregate. Store the app name here.
  // The main impact on this is validity checking on sync when the appId is
  // checked.
  private static final String ODK_TABLES_APP_ID = "ODK_TABLES_APP_ID";

  /**
   * Construct a relation prototype.
   *
   * @param databaseSchema
   * @param tableName
   */
  private ServerPreferencesPropertiesTable(String schemaName) {
    super(schemaName, TABLE_NAME);
    fieldList.add(KEY);
    fieldList.add(VALUE);
  }

  /**
   * Construct an empty entity. Only called via {@link #getEmptyRow(User)}
   *
   * @param ref
   * @param user
   */
  private ServerPreferencesPropertiesTable(ServerPreferencesPropertiesTable ref, User user) {
    super(ref, user);
  }

  @Override
  public ServerPreferencesPropertiesTable getEmptyRow(User user) {
    return new ServerPreferencesPropertiesTable(this, user);
  }

  public static String getSiteKey(CallingContext cc) throws ODKEntityNotFoundException,
      ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, SITE_KEY);
    if (value == null) {
      // synthesize a new one...
      value = CommonFieldsBase.newUri();
      setServerPreferencesProperty(cc, SITE_KEY, value);
    }
    return value;
  }

  public static void setSiteKey(CallingContext cc, String siteKey)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, SITE_KEY, siteKey);
  }

  public static String getLastKnownRealmString(CallingContext cc) throws ODKEntityNotFoundException,
      ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, LAST_KNOWN_REALM_STRING);
    return value;
  }

  public static void setLastKnownRealmString(CallingContext cc, String lastKnownRealmString)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, LAST_KNOWN_REALM_STRING, lastKnownRealmString);
  }



  public static Boolean getOdkTablesEnabled(CallingContext cc) throws ODKEntityNotFoundException,
      ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, ODK_TABLES_ENABLED);
    if (value != null) {
      return Boolean.valueOf(value);
    }
    // null value should be treated as false
    return false;
  }

  public static void setOdkTablesEnabled(CallingContext cc, Boolean enabled)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, ODK_TABLES_ENABLED, enabled.toString());
  }

  public static String getOdkTablesAppId(CallingContext cc) throws ODKEntityNotFoundException,
      ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, ODK_TABLES_APP_ID);
    if ( value == null || value.length() == 0 ) {
      value = "default";
    }
    return value;
  }

  public static void setOdkTablesAppId(CallingContext cc, String appId)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, ODK_TABLES_APP_ID, appId);
  }

  public static String unsafeIncOdkTablesSequencerBase(CallingContext cc)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, ODK_TABLES_SEQUENCER_BASE);
    String newValue = WebUtils.iso8601Date(new Date());
    if (value != null && value.compareTo(newValue) >= 0) {
      // the saved value String-compares greater
      // than the current time string.

      // parse the current time string...
      Date d = WebUtils.parseDate(value);
      if (d == null) {
        throw new IllegalStateException(
            "The saved ODK_TABLES_SEQUENCER_BASE value could not be parsed!");
      }
      // add 1 millisecond and retry...
      newValue = WebUtils.iso8601Date(new Date(d.getTime() + 1));
      if (value.compareTo(newValue) >= 0) {
        throw new IllegalStateException(
            "The new ODK_TABLES_SEQUENCER_BASE value was not greater than the saved value.");
      }
    }
    setServerPreferencesProperty(cc, ODK_TABLES_SEQUENCER_BASE, newValue);
    return newValue;
  }

  public static Boolean getFasterWatchdogCycleEnabled(CallingContext cc)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, FASTER_WATCHDOG_CYCLE_ENABLED);
    if (value != null) {
      return Boolean.valueOf(value);
    }
    // null value should be treated as false
    return false;
  }

  public static void setFasterWatchdogCycleEnabled(CallingContext cc, Boolean enabled)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, FASTER_WATCHDOG_CYCLE_ENABLED, enabled.toString());
  }

  public static Boolean getFasterBackgroundActionsDisabled(CallingContext cc)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, FASTER_BACKGROUND_ACTIONS_DISABLED);
    if (value != null) {
      return Boolean.valueOf(value);
    }
    // null value should be treated as false
    return false;
  }

  public static void setFasterBackgroundActionsDisabled(CallingContext cc, Boolean disabled)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, FASTER_BACKGROUND_ACTIONS_DISABLED, disabled.toString());
  }

  public static Boolean getSkipMalformedSubmissions(CallingContext cc)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    String value = getServerPreferencesProperty(cc, SKIP_MALFORMED_SUBMISSIONS);
    if (value != null) {
      return Boolean.valueOf(value);
    }
    // null value should be treated as false
    return false;
  }

  public static void setSkipMalformedSubmissions(CallingContext cc, Boolean skipMalformedSubmissions)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    setServerPreferencesProperty(cc, SKIP_MALFORMED_SUBMISSIONS, skipMalformedSubmissions.toString());
  }

  public void persist(CallingContext cc) throws ODKEntityPersistException, ODKOverQuotaException {
    Datastore ds = cc.getDatastore();
    User user = cc.getCurrentUser();

    ds.putEntity(this, user);
  }

  private static ServerPreferencesPropertiesTable relation = null;

  public static synchronized final ServerPreferencesPropertiesTable assertRelation(CallingContext cc)
      throws ODKDatastoreException {
    if (relation == null) {
      ServerPreferencesPropertiesTable relationPrototype;
      Datastore ds = cc.getDatastore();
      User user = cc.getUserService().getDaemonAccountUser();
      relationPrototype = new ServerPreferencesPropertiesTable(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      relation = relationPrototype; // set static variable only upon success...
    }
    return relation;
  }

  public static final String getServerPreferencesProperty(CallingContext cc, String keyName)
      throws ODKEntityNotFoundException, ODKOverQuotaException {
    try {
      ServerPreferencesPropertiesTable relation = assertRelation(cc);
      Query query = cc.getDatastore().createQuery(relation,
          "ServerPreferences.getServerPreferences", cc.getCurrentUser());
      query.addFilter(KEY, Query.FilterOperation.EQUAL, keyName);
      // don't care about duplicate entries because we always access the most
      // recent first
      query.addSort(relation.lastUpdateDate, Query.Direction.DESCENDING);
      List<? extends CommonFieldsBase> results = query.executeQuery();
      if (!results.isEmpty()) {
        if (results.get(0) instanceof ServerPreferencesPropertiesTable) {
          ServerPreferencesPropertiesTable preferences = (ServerPreferencesPropertiesTable) results.get(0);
          return preferences.getStringField(VALUE);
        }
      }
      return null;
    } catch (ODKOverQuotaException e) {
      throw e;
    } catch (ODKDatastoreException e) {
      throw new ODKEntityNotFoundException(e);
    }
  }

  public static final void setServerPreferencesProperty(CallingContext cc, String keyName,
      String value) throws ODKEntityNotFoundException, ODKOverQuotaException {
    Log logger = LogFactory.getLog(ServerPreferencesPropertiesTable.class);

    try {
      ServerPreferencesPropertiesTable relation = assertRelation(cc);
      Query query = cc.getDatastore().createQuery(relation,
          "ServerPreferences.getServerPreferences", cc.getCurrentUser());
      query.addFilter(KEY, Query.FilterOperation.EQUAL, keyName);
      List<? extends CommonFieldsBase> results = query.executeQuery();
      if (!results.isEmpty()) {
        if (results.get(0) instanceof ServerPreferencesPropertiesTable) {
          ServerPreferencesPropertiesTable preferences = (ServerPreferencesPropertiesTable) results.get(0);
          if (!preferences.setStringField(VALUE, value)) {
            throw new IllegalStateException("Unexpected truncation of ServerPreferencesProperty: "
                + keyName + " value");
          }
          preferences.persist(cc);
          return;
        }
        throw new IllegalStateException("Expected ServerPreferencesProperties entity");
      }
      // nothing there -- put the value...
      ServerPreferencesPropertiesTable preferences = cc.getDatastore().createEntityUsingRelation(
          relation, cc.getCurrentUser());
      logger.info("Setting Server Preference " + keyName + " to " + value);
      if (!preferences.setStringField(KEY, keyName)) {
        throw new IllegalStateException("Unexpected truncation of ServerPreferencesProperty: "
            + keyName + " keyName");
      }
      if (!preferences.setStringField(VALUE, value)) {
        throw new IllegalStateException("Unexpected truncation of ServerPreferencesProperty: "
            + keyName + " value");
      }
      preferences.persist(cc);
      return;
    } catch (ODKOverQuotaException e) {
      throw e;
    } catch (ODKDatastoreException e) {
      throw new ODKEntityNotFoundException(e);
    }
  }
}
