/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.persistence.engine.pgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.opendatakit.persistence.CommonFieldsBase;
import org.opendatakit.persistence.DataField;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.EntityKey;
import org.opendatakit.persistence.ITaskLockType;
import org.opendatakit.persistence.TaskLock;
import org.opendatakit.persistence.engine.DatastoreAccessMetrics;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.persistence.exception.ODKTaskLockException;
import org.opendatakit.security.User;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class TaskLockImpl implements TaskLock {

  private static final String PERSISTENCE_LAYER_PROBLEM = "Persistence layer failure";

  final DatastoreAccessMetrics dam;
  final DatastoreImpl datastore;
  final User user;

  TaskLockImpl(DatastoreImpl datastore, DatastoreAccessMetrics dam, User user) {
    this.datastore = datastore;
    this.dam = dam;
    this.user = user;
  }

  private static final String K_BQ = "\"";

  private TaskLockTable doTransaction(TaskLockTable entity, long l)
      throws ODKEntityNotFoundException, ODKTaskLockException {
    boolean first;

    final List<String> stmts = new ArrayList<String>();
    
    String uri = entity.getUri();
    
    StringBuilder stringBuilder = new StringBuilder();
    String tableName = K_BQ + datastore.getDefaultSchemaName() + K_BQ + "." + K_BQ
        + TaskLockTable.TABLE_NAME + K_BQ;

    stringBuilder.append("'").append(user.getUriUser().replaceAll("'", "''")).append("'");
    String uriUserInline = stringBuilder.toString();
    stringBuilder.setLength(0);
    stringBuilder.append("'").append(uri.replaceAll("'", "''")).append("'");
    String uriLockInline = stringBuilder.toString();
    stringBuilder.setLength(0);
    stringBuilder.append("'").append(entity.getFormId().replaceAll("'", "''")).append("'");
    String formIdInline = stringBuilder.toString();
    stringBuilder.setLength(0);
    stringBuilder.append("'").append(entity.getTaskType().replaceAll("'", "''")).append("'");
    String taskTypeInline = stringBuilder.toString();
    stringBuilder.setLength(0);
    stringBuilder.append("interval '").append(l).append(" milliseconds'");
    String lifetimeIntervalMilliseconds = stringBuilder.toString();
    stringBuilder.setLength(0);

    stringBuilder.append("LOCK TABLE ").append(tableName).append(" IN ACCESS EXCLUSIVE MODE");
    stmts.add(stringBuilder.toString());
    stringBuilder.setLength(0);

    dam.recordPutUsage(TaskLockTable.TABLE_NAME);
    if (!entity.isFromDatabase()) {
      // insert a new record (prospective lock)
      stringBuilder.append("INSERT INTO ");
      stringBuilder.append(tableName);
      stringBuilder.append(" (");
      first = true;
      for (DataField dataField : entity.getFieldList()) {
        if (!first) {
          stringBuilder.append(",");
        }
        first = false;
        stringBuilder.append(K_BQ);
        stringBuilder.append(dataField.getName());
        stringBuilder.append(K_BQ);
      }
      first = true;
      stringBuilder.append(") VALUES ( ");
      for (DataField dataField : entity.getFieldList()) {
        if (!first) {
          stringBuilder.append(",");
        }
        first = false;
        if (dataField.equals(entity.creationDate) || dataField.equals(entity.lastUpdateDate)) {
          stringBuilder.append("NOW()");
        } else if (dataField.equals(entity.creatorUriUser) || dataField.equals(entity.lastUpdateUriUser)) {
          stringBuilder.append(uriUserInline);
        } else if (dataField.equals(entity.formId)) {
          stringBuilder.append(formIdInline);
        } else if (dataField.equals(entity.taskType)) {
          stringBuilder.append(taskTypeInline);
        } else if (dataField.equals(entity.primaryKey)) {
          stringBuilder.append(uriLockInline);
        } else if (dataField.equals(entity.expirationDateTime)) {
          stringBuilder.append(" NOW() + ");
          stringBuilder.append(lifetimeIntervalMilliseconds);
        } else {
          throw new IllegalStateException("unexpected case " + dataField.getName());
        }
      }
      stringBuilder.append(")");
      stmts.add(stringBuilder.toString());
      stringBuilder.setLength(0);
    } else {
      // update existing record (prospective lock)
      stringBuilder.append("UPDATE ");
      stringBuilder.append(tableName);
      stringBuilder.append(" SET ");
      first = true;
      for (DataField f : entity.getFieldList()) {
        if (f == entity.primaryKey)
          continue;
        if (!first) {
          stringBuilder.append(",");
        }
        first = false;
        stringBuilder.append(K_BQ);
        stringBuilder.append(f.getName());
        stringBuilder.append(K_BQ);
        stringBuilder.append(" = ");
        if (f.equals(entity.creationDate) || f.equals(entity.lastUpdateDate)) {
          stringBuilder.append("NOW()");
        } else if (f.equals(entity.creatorUriUser) || f.equals(entity.lastUpdateUriUser)) {
          stringBuilder.append(uriUserInline);
        } else if (f.equals(entity.formId)) {
          stringBuilder.append(formIdInline);
        } else if (f.equals(entity.taskType)) {
          stringBuilder.append(taskTypeInline);
        } else if (f.equals(entity.primaryKey)) {
          stringBuilder.append(uriLockInline);
        } else if (f.equals(entity.expirationDateTime)) {
          stringBuilder.append(" NOW() + ");
          stringBuilder.append(lifetimeIntervalMilliseconds);
        } else {
          throw new IllegalStateException("unexpected case " + f.getName());
        }
      }
      stringBuilder.append(" WHERE ");
      stringBuilder.append(K_BQ);
      stringBuilder.append(entity.primaryKey.getName());
      stringBuilder.append(K_BQ);
      stringBuilder.append(" = ");
      stringBuilder.append(uriLockInline);
      stmts.add(stringBuilder.toString());
      stringBuilder.setLength(0);
    }
    // delete stale locks (don't care who's)
    dam.recordDeleteUsage(TaskLockTable.TABLE_NAME);
    stringBuilder.append("DELETE FROM ").append(tableName).append(" WHERE ");
    stringBuilder.append(K_BQ).append(entity.expirationDateTime.getName()).append(K_BQ).append(" <= NOW()");
    stmts.add(stringBuilder.toString());
    stringBuilder.setLength(0);
    // delete prospective locks which are not the oldest for that resource and
    // task type
    dam.recordDeleteUsage(TaskLockTable.TABLE_NAME);
    stringBuilder.append("DELETE FROM ").append(tableName).append(" WHERE ");
    stringBuilder.append(K_BQ).append(entity.formId.getName()).append(K_BQ).append(" = ")
        .append(formIdInline).append(" AND ");
    stringBuilder.append(K_BQ).append(entity.taskType.getName()).append(K_BQ).append(" = ")
        .append(taskTypeInline).append(" AND ");
    stringBuilder.append(K_BQ).append(entity.expirationDateTime.getName()).append(K_BQ);
    stringBuilder.append(" > (SELECT MIN(t3.").append(K_BQ).append(entity.expirationDateTime.getName())
        .append(K_BQ);
    stringBuilder.append(") FROM ").append(tableName).append(" AS t3 WHERE t3.");
    stringBuilder.append(K_BQ).append(entity.formId.getName()).append(K_BQ).append(" = ").append(formIdInline)
        .append(" AND t3.");
    stringBuilder.append(K_BQ).append(entity.taskType.getName()).append(K_BQ).append(" = ")
        .append(taskTypeInline).append(")");
    stmts.add(stringBuilder.toString());
    stringBuilder.setLength(0);
    // delete our entry if it collides with another entry with exactly 
    // this time.
    stringBuilder.append("DELETE FROM ").append(tableName).append(" WHERE ");
    stringBuilder.append(K_BQ).append(entity.formId.getName()).append(K_BQ).append(" = ")
        .append(formIdInline).append(" AND ");
    stringBuilder.append(K_BQ).append(entity.taskType.getName()).append(K_BQ).append(" = ")
        .append(taskTypeInline).append(" AND ");
    stringBuilder.append(K_BQ).append(entity.primaryKey.getName()).append(K_BQ).append(" = ")
        .append(uriLockInline).append(" AND ");
    stringBuilder.append("1 < (SELECT COUNT(t3.").append(K_BQ).append(entity.expirationDateTime.getName())
        .append(K_BQ);
    stringBuilder.append(") FROM ").append(tableName).append(" AS t3 WHERE t3.");
    stringBuilder.append(K_BQ).append(entity.formId.getName()).append(K_BQ).append(" = ").append(formIdInline)
        .append(" AND t3.");
    stringBuilder.append(K_BQ).append(entity.taskType.getName()).append(K_BQ).append(" = ")
        .append(taskTypeInline).append(")");
    stmts.add(stringBuilder.toString());
    stringBuilder.setLength(0);
    // assert: only the lock that holds the resource for that task type appears
    // in the task lock table
    TaskLockTable relation;
    try {

      JdbcTemplate jdbc = datastore.getJdbcConnection();
      jdbc.execute(new ConnectionCallback<Object>() {

        @Override
        public Object doInConnection(Connection conn) throws SQLException, DataAccessException {
          boolean oldAutoCommitValue = conn.getAutoCommit();
          int oldTransactionValue = conn.getTransactionIsolation();
          try {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            Statement stmt = conn.createStatement();
            for (String s : stmts) {
              // for debugging: LogFactory.getLog(TaskLockImpl.class).info(s);
              stmt.execute(s);
            }
            conn.commit();
          } catch (PSQLException e) {
            e.printStackTrace();
            conn.rollback();
          }catch (Exception e) {
            e.printStackTrace();
            conn.rollback();
          }
          conn.setTransactionIsolation(oldTransactionValue);
          conn.setAutoCommit(oldAutoCommitValue);
          return null;
        }

      });

      relation = TaskLockTable.assertRelation(datastore, user);
    } catch (Exception e) {
      throw new ODKTaskLockException(PERSISTENCE_LAYER_PROBLEM, e);
    }
    return (TaskLockTable) datastore.getEntity(relation, entity.getUri(), user);
  }

  @Override
  public boolean obtainLock(String lockId, String formId, ITaskLockType taskType) {
    boolean result = false;
    try {
      TaskLockTable relation = TaskLockTable.assertRelation(datastore, user);
      TaskLockTable entity = datastore.createEntityUsingRelation(relation, user);
      entity.setStringField(entity.primaryKey, lockId);
      entity.setFormId(formId);
      entity.setTaskType(taskType.getName());
      entity = doTransaction(entity, taskType.getLockExpirationTimeout());
      result = true;
    } catch (ODKEntityNotFoundException e) {
      // didn't gain the lock...
    } catch (ODKTaskLockException e) {
      // unexpected failure...
      e.printStackTrace();
    } catch (ODKDatastoreException e) {
      // unexpected failure...
      e.printStackTrace();
    } 
    return result;
  }

  @Override
  public boolean renewLock(String lockId, String formId, ITaskLockType taskType) {
    boolean result = false;
    try {
      TaskLockTable relation = TaskLockTable.assertRelation(datastore, user);
      TaskLockTable entity = datastore.getEntity(relation, lockId, user);
      if (!(entity.getFormId().equals(formId) && entity.getTaskType().equals(taskType.getName()))) {
        throw new IllegalArgumentException("formId or taskType don't match datastore values");
      }
      entity = doTransaction(entity, taskType.getLockExpirationTimeout());
      result = true;
    } catch (IllegalArgumentException e) {
      // unexpected failure...
      e.printStackTrace();
    } catch (ODKEntityNotFoundException e) {
      // unexpected failure...
      e.printStackTrace();
    } catch (ODKDatastoreException e) {
      // unexpected failure...
      e.printStackTrace();
    } catch (ODKTaskLockException e) {
      // unexpected failure...
      e.printStackTrace();
    }
    return result;
  }

  @Override
  public boolean releaseLock(String lockId, String formId, ITaskLockType taskType) {
    boolean result = false;
    try {
      TaskLockTable relation = TaskLockTable.assertRelation(datastore, user);
      // we don't have the record that we want to delete; construct
      // the entity key from the relation and the URI for the record.
      datastore.deleteEntity(new EntityKey(relation, lockId), user);
      result = true;
    } catch (ODKDatastoreException e) {
      // if we see a lot of these, we are running too long between renewals
      LogFactory.getLog(TaskLockImpl.class).info("delete of taskLock threw exception!");
      e.printStackTrace();
    }
    return result;
  }

  public static class TaskLockTable extends CommonFieldsBase {
    static final String TABLE_NAME = "_task_lock";

    private static final DataField FORM_ID = new DataField("FORM_ID", DataField.DataType.STRING,
        false, 4096L);
    private static final DataField TASK_TYPE = new DataField("TASK_TYPE",
        DataField.DataType.STRING, false, 80L);
    private static final DataField EXPIRATION_DATETIME = new DataField("EXPIRATION_DATETIME",
        DataField.DataType.DATETIME, true);

    DataField formId;
    DataField taskType;
    DataField expirationDateTime;

    TaskLockTable(String schema) {
      super(schema, TABLE_NAME);
      fieldList.add(formId = new DataField(FORM_ID));
      fieldList.add(taskType = new DataField(TASK_TYPE));
      fieldList.add(expirationDateTime = new DataField(EXPIRATION_DATETIME));
    }

    TaskLockTable(TaskLockTable ref, User user) {
      super(ref, user);
      formId = ref.formId;
      taskType = ref.taskType;
      expirationDateTime = ref.expirationDateTime;
    }

    String getFormId() {
      return getStringField(formId);
    }

    void setFormId(String value) {
      if (!setStringField(formId, value)) {
        throw new IllegalStateException("overflow formId");
      }
    }

    String getTaskType() {
      return getStringField(taskType);
    }

    void setTaskType(String value) {
      if (!setStringField(taskType, value)) {
        throw new IllegalStateException("overflow taskType");
      }
    }

    Date getExpirationDateTime() {
      return getDateField(expirationDateTime);
    }

    void setExpirationDateTime(Date value) {
      setDateField(expirationDateTime, value);
    }

    @Override
    public CommonFieldsBase getEmptyRow(User user) {
      return new TaskLockTable(this, user);
    }

    static TaskLockTable relation = null;
    
    public static void resetSingletonReference() {
      relation = null;
    }

    public static synchronized final TaskLockTable assertRelation(Datastore datastore, User user)
        throws ODKDatastoreException {
      if (relation == null) {
        TaskLockTable relationPrototype;
        relationPrototype = new TaskLockTable(datastore.getDefaultSchemaName());
        datastore.assertRelation(relationPrototype, user);
        relation = relationPrototype;
      }
      return relation;
    }
  }
}
