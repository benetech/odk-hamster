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
package org.opendatakit.ermodel;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.configuration.annotations.DBUnitTestConfig;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Simple test case for the AbstractBlobRelationSet class.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DBUnitTestConfig
public class BlobRelationSetTest {

  @Autowired
  CallingContext callingContext;

  @Before
  public void setUp() throws Exception {

    // ensure that blobset tables are not present...
    MyBlobRelationSet set = new MyBlobRelationSet(callingContext);
    set.dropBlobRelationSet(callingContext);
  }

  static class MyBlobRelationSet extends AbstractBlobRelationSet {

    MyBlobRelationSet(CallingContext callingContext) throws ODKDatastoreException {
      super("TEST_BLOB", callingContext);
    }
  }

  @Test
  public void testCase1() throws ODKDatastoreException {

    MyBlobRelationSet rel = new MyBlobRelationSet(callingContext);

    BlobEntitySet instance = rel.newBlobEntitySet(callingContext);
    assertEquals(0, instance.getAttachmentCount(callingContext));

    String s = "this is a string";
    instance.addBlob(s.getBytes(), "text/plain", null, false, callingContext);
    String t = "another string";
    instance.addBlob(t.getBytes(), "text/xml", "different", false, callingContext);
    instance.persist(callingContext);
    BlobEntitySet alt = rel.getBlobEntitySet(instance.getUri(), callingContext);
    assertEquals(2, alt.getAttachmentCount(callingContext));
    String name = alt.getUnrootedFilename(1, callingContext);
    String otherType = null;
    if (name == null) {
      assertEquals("text/plain", alt.getContentType(1, callingContext));
      assertEquals(s.getBytes().length, alt.getContentLength(1, callingContext).intValue());
      otherType = "text/xml";
    } else {
      assertEquals("text/xml", alt.getContentType(1, callingContext));
      assertEquals(t.getBytes().length, alt.getContentLength(1, callingContext).intValue());
      otherType = "text/plain";
    }
    assertEquals(otherType, alt.getContentType(2, callingContext));

    rel.dropBlobRelationSet(callingContext);
  }
}
