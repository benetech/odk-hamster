/**
 * Copyright (C) 2011 University of Washington
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
package org.opendatakit.persistence;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.configuration.UserServiceConfiguration;
import org.opendatakit.configuration.annotations.DBUnitTestConfig;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.CommonFieldsBase;
import org.opendatakit.persistence.DataField;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.PersistConsts;
import org.opendatakit.persistence.Query;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.exception.ODKEntityPersistException;
import org.opendatakit.persistence.exception.ODKOverQuotaException;
import org.opendatakit.security.User;
import org.opendatakit.security.spring.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests storage of strings up to STR_LENGTH (16384) characters long. Confirms that if you store
 * longer strings, they get truncated, and if you fetch the strings from the persistence layer, they
 * are not altered beyond the truncation of length.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DBUnitTestConfig
public class LargeStringTest {

  static int STR_LENGTH = 16384;
  static int SET_SIZE = 2000;
  
  private static final Log logger = LogFactory.getLog(LargeStringTest.class);


  @Autowired
  CallingContext callingContext;

  @PostConstruct
  public void oneTimeSetUp() throws Exception {

    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    System.out.println("dropping the large string table");
    // drop it, in case prior test was messed up...
    callingContext.getDatastore().dropRelation(rel, callingContext.getCurrentUser());

    rel = MyLargeStringRelation.assertRelation(callingContext);

    System.out.println("writing the large string table");

    // write a lot of data...
    for (DataValue value : values) {
      value.assertInstance(rel, callingContext);
    }

    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    System.out.println("done writing the large string table");
  }

  @PreDestroy
  public void afterClass() throws Exception {

    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    System.out.println("dropping the large string table");
    callingContext.getDatastore().dropRelation(rel, callingContext.getCurrentUser());// drop it, in case prior test was
                                                             // messed up...

  }

  static class DataValue {
    static final char chars[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    String guardChar;
    int length;
    String largeString;
    String alternateString;

    DataValue(String guardChar, int length) {
      this.guardChar = guardChar;
      this.length = length;

      StringBuilder b = new StringBuilder();
      StringBuilder s = new StringBuilder();
      for (int i = 0; i < length; ++i) {
        if (i % 5 == 0) {
          b.append(guardChar);
        } else {
          b.append(chars[i % 10]);
        }
        s.append(chars[i % 10]);
      }
      largeString = b.toString();
      alternateString = s.toString();
    }

    void assertInstance(MyLargeStringRelation rel, CallingContext callingContext)
        throws ODKEntityPersistException, ODKOverQuotaException {
      Datastore ds = callingContext.getDatastore();
      User user = callingContext.getCurrentUser();
      MyLargeStringRelation element = ds.createEntityUsingRelation(rel, user);
      element.setStringField(element.primaryKey, guardChar);
      element.setStringField(MyLargeStringRelation.fieldLargeString, largeString);
      ds.putEntity(element, user);
    }
  }

  static DataValue[] values = {
      /* GuardLetter (also PK), length */
      new DataValue("A", 64), new DataValue("B", 81), new DataValue("C", 249),
      new DataValue("D", 250), new DataValue("E", 255), new DataValue("F", 256),
      new DataValue("G", 257), new DataValue("H", 1024), new DataValue("I", STR_LENGTH),
      new DataValue("J", STR_LENGTH + 1), new DataValue("K", STR_LENGTH + 5),};

  @Ignore
  static class MyLargeStringRelation extends CommonFieldsBase {

    static final DataField fieldLargeString = new DataField("LARGE_STRING_FIELD",
        DataField.DataType.STRING, true, Long.valueOf(STR_LENGTH));

    /**
     * Construct a relation prototype.
     * 
     * @param databaseSchema
     */
    private MyLargeStringRelation(String databaseSchema) {
      super(databaseSchema, "MY_LARGE_STRING_TABLE");
      fieldList.add(fieldLargeString);
    }

    /**
     * Construct an empty entity.
     * 
     * @param ref
     * @param user
     */
    private MyLargeStringRelation(MyLargeStringRelation ref, User user) {
      super(ref, user);
    }

    @Override
    public CommonFieldsBase getEmptyRow(User user) {
      return new MyLargeStringRelation(this, user);
    }

    public void print() {
      System.out.println("PK " + getUri() + " " + fieldLargeString.getName() + " "
          + getStringField(fieldLargeString));
    }

    static synchronized final MyLargeStringRelation assertRelation(CallingContext callingContext)
        throws ODKDatastoreException {
      MyLargeStringRelation relationPrototype;
      Datastore ds = callingContext.getDatastore();
      User user = callingContext.getUserService().getDaemonAccountUser();
      relationPrototype = new MyLargeStringRelation(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      return relationPrototype; // set static variable only upon success...
    }
  }

  /**
   * This class is identical to the MyLargeStringRelation EXCEPT that the field definition does not
   * specify the length of the string field. It is expected that the datastore will supply this
   * value.
   * 
   * @author mitchellsundt@gmail.com
   *
   */
  @Ignore
  static class MyDynamicLargeStringRelation extends CommonFieldsBase {

    static final DataField fieldLargeString =
        new DataField("LARGE_STRING_FIELD", DataField.DataType.STRING, true);

    /**
     * Construct a relation prototype.
     * 
     * @param databaseSchema
     */
    private MyDynamicLargeStringRelation(String databaseSchema) {
      super(databaseSchema, "MY_LARGE_STRING_TABLE");
      fieldList.add(fieldLargeString);
    }

    /**
     * Construct an empty entity.
     * 
     * @param ref
     * @param user
     */
    private MyDynamicLargeStringRelation(MyDynamicLargeStringRelation ref, User user) {
      super(ref, user);
    }

    @Override
    public CommonFieldsBase getEmptyRow(User user) {
      return new MyDynamicLargeStringRelation(this, user);
    }

    public void print() {
      System.out.println("PK " + getUri() + " " + fieldLargeString.getName() + " "
          + getStringField(fieldLargeString));
    }

    static synchronized final MyDynamicLargeStringRelation assertRelation(CallingContext callingContext)
        throws ODKDatastoreException {
      MyDynamicLargeStringRelation relationPrototype;
      Datastore ds = callingContext.getDatastore();
      User user = callingContext.getUserService().getDaemonAccountUser();
      relationPrototype = new MyDynamicLargeStringRelation(ds.getDefaultSchemaName());
      ds.assertRelation(relationPrototype, user); // may throw exception...
      // at this point, the prototype has become fully populated
      return relationPrototype; // set static variable only upon success...
    }
  }

  @Test
  public void verifyRowCount() throws ODKDatastoreException {
    logger.info("verifyRowCount");
    
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    Query query = ds.createQuery(rel, "LargeStringTest.verifyRowCount", user);

    List<?> dbValues =
        query.executeDistinctValueForDataField(MyLargeStringRelation.fieldLargeString);
    assertEquals(values.length, dbValues.size());
  }

  @Test
  public void verifyStrings() throws ODKDatastoreException {
    logger.info("verifyStrings");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    Query query = ds.createQuery(rel, "LargeStringTest.verifyStrings", user);

    List<? extends CommonFieldsBase> dbValues = query.executeQuery();
    for (CommonFieldsBase b : dbValues) {
      MyLargeStringRelation r = (MyLargeStringRelation) b;

      String guardChar = r.getUri();
      for (DataValue v : values) {
        if (v.guardChar.equals(guardChar)) {
          String largeString = r.getStringField(MyLargeStringRelation.fieldLargeString);
          int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();
          String truncatedString = v.largeString;
          if (truncatedLength < v.length) {
            truncatedString = v.largeString.substring(0, truncatedLength);
          }
          assertEquals(largeString, truncatedString);
        }
      }
    }
  }

  @Test
  public void verifySetStringFieldReturns() throws ODKDatastoreException {
    logger.info("verifySetStringFieldReturns");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();
    MyLargeStringRelation r = ds.createEntityUsingRelation(rel, user);
    for (DataValue v : values) {
      assertEquals("setStringField outcome for: " + v.largeString,
          r.setStringField(MyLargeStringRelation.fieldLargeString, v.largeString),
          v.length <= truncatedLength);
    }
  }

  @Test
  public void verifyPutGetPutGetEffects() throws ODKDatastoreException {
    logger.info("verifyPutGetPutGetEffects");
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyLargeStringRelation rel = MyLargeStringRelation.assertRelation(callingContext);

    int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();

    // update records with alternate long string
    for (DataValue v : values) {
      MyLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.setStringField(MyLargeStringRelation.fieldLargeString, v.alternateString),
          v.length <= truncatedLength);
      ds.putEntity(r, user);
    }
    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    // verify that alternate strings were saved; update with null string.
    for (DataValue v : values) {
      String truncatedAltString = v.alternateString;
      if (truncatedLength < v.length) {
        truncatedAltString = v.alternateString.substring(0, truncatedLength);
      }
      MyLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyLargeStringRelation.fieldLargeString), truncatedAltString);
      r.setStringField(MyLargeStringRelation.fieldLargeString, null);
      ds.putEntity(r, user);
    }

    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    // verify that null strings were saved; update with large string value.
    for (DataValue v : values) {
      MyLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyLargeStringRelation.fieldLargeString), null);
      assertEquals(r.setStringField(MyLargeStringRelation.fieldLargeString, v.largeString),
          v.length <= truncatedLength);
      ds.putEntity(r, user);
    }

    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // verify that large string is what is present
    for (DataValue v : values) {
      String truncatedString = v.largeString;
      if (truncatedLength < v.length) {
        truncatedString = v.largeString.substring(0, truncatedLength);
      }
      MyLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyLargeStringRelation.fieldLargeString), truncatedString);
    }
  }

  @Test
  public void verifyDynamicRowCount() throws ODKDatastoreException {
    logger.info("verifyDynamicRowCount");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyDynamicLargeStringRelation rel = MyDynamicLargeStringRelation.assertRelation(callingContext);

    Query query = ds.createQuery(rel, "LargeStringTest.verifyDynamicRowCount", user);

    List<?> dbValues =
        query.executeDistinctValueForDataField(MyDynamicLargeStringRelation.fieldLargeString);
    assertEquals(values.length, dbValues.size());
  }

  @Test
  public void verifyDynamicStrings() throws ODKDatastoreException {
    logger.info("verifyDynamicStrings");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();

    int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();
    MyDynamicLargeStringRelation rel = MyDynamicLargeStringRelation.assertRelation(callingContext);

    Query query = ds.createQuery(rel, "LargeStringTest.verifyDynamicStrings", user);

    List<? extends CommonFieldsBase> dbValues = query.executeQuery();
    for (CommonFieldsBase b : dbValues) {
      MyDynamicLargeStringRelation r = (MyDynamicLargeStringRelation) b;

      String guardChar = r.getUri();
      for (DataValue v : values) {
        if (v.guardChar.equals(guardChar)) {
          String largeString = r.getStringField(MyDynamicLargeStringRelation.fieldLargeString);
          String truncatedString = v.largeString;
          if (truncatedLength < v.length) {
            truncatedString = v.largeString.substring(0, truncatedLength);
          }
          assertEquals(largeString, truncatedString);
        }
      }
    }
  }

  @Test
  public void verifyDynamicSetStringFieldReturns() throws ODKDatastoreException {
    logger.info("verifyDynamicSetStringFieldReturns");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyDynamicLargeStringRelation rel = MyDynamicLargeStringRelation.assertRelation(callingContext);

    int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();
    MyDynamicLargeStringRelation r = ds.createEntityUsingRelation(rel, user);

    for (DataValue v : values) {
      assertEquals("setStringField outcome for: " + v.largeString,
          r.setStringField(MyDynamicLargeStringRelation.fieldLargeString, v.largeString),
          v.length <= truncatedLength);
    }
  }

  @Test
  public void verifyDynamicPutGetPutGetEffects() throws ODKDatastoreException {
    logger.info("verifyDynamicPutGetPutGetEffects");

    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    MyDynamicLargeStringRelation rel = MyDynamicLargeStringRelation.assertRelation(callingContext);

    int truncatedLength = (int) (long) MyLargeStringRelation.fieldLargeString.getMaxCharLen();

    // update records with alternate long string
    for (DataValue v : values) {
      MyDynamicLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(
          r.setStringField(MyDynamicLargeStringRelation.fieldLargeString, v.alternateString),
          v.length <= truncatedLength);
      ds.putEntity(r, user);
    }
    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    // verify that alternate strings were saved; update with null string.
    for (DataValue v : values) {
      String truncatedAltString = v.alternateString;
      if (truncatedLength < v.length) {
        truncatedAltString = v.alternateString.substring(0, truncatedLength);
      }
      MyDynamicLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyDynamicLargeStringRelation.fieldLargeString),
          truncatedAltString);
      r.setStringField(MyDynamicLargeStringRelation.fieldLargeString, null);
      ds.putEntity(r, user);
    }

    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }
    // verify that null strings were saved; update with large string value.
    for (DataValue v : values) {
      MyDynamicLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyDynamicLargeStringRelation.fieldLargeString), null);
      assertEquals(r.setStringField(MyDynamicLargeStringRelation.fieldLargeString, v.largeString),
          v.length <= truncatedLength);
      ds.putEntity(r, user);
    }

    // wait for puts to happen
    try {
      Thread.sleep(PersistConsts.MAX_SETTLE_MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // verify that large string is what is present
    for (DataValue v : values) {
      String truncatedString = v.largeString;
      if (truncatedLength < v.length) {
        truncatedString = v.largeString.substring(0, truncatedLength);
      }
      MyDynamicLargeStringRelation r;
      r = ds.getEntity(rel, v.guardChar, user);
      assertEquals(r.getStringField(MyDynamicLargeStringRelation.fieldLargeString),
          truncatedString);
    }
  }
}
