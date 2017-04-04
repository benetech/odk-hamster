/**
 * Copyright (C) 2012 University of Washington
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
package org.opendatakit.common.persistence;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.configuration.TestDataConfiguration;
import org.opendatakit.configuration.TestUserServiceConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the task lock mechanism and reports its performance statistics.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestDataConfiguration.class, TestUserServiceConfiguration.class})
@ActiveProfiles("unittest")
@TestExecutionListeners(listeners = {SetupTeardown.class},
    mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public class TaskLockTest {

  static AtomicBoolean inside = new AtomicBoolean(false);
  
  @Autowired
  CallingContext callingContext;

  @Ignore
  static class TaskLockThread extends Thread {
    CyclicBarrier launchBarrier;
    CallingContext callingContext;

    static int ENTRY_ATTEMPTS = 30;

    boolean failed = false;
    int entryCount = 0;
    int declinedEntryCount = 0;
    int lockFailedCount = 0;

    TaskLockThread(CyclicBarrier launchBarrier, CallingContext callingContext) {
      this.launchBarrier = launchBarrier;
      this.callingContext = callingContext;
    }

    @Override
    public void run() {

      try {
        launchBarrier.await();
      } catch (Exception e) {
        failed = true;
        System.out.println("Premature exception from barrier " + e.toString());
      }
      try {
        if (!failed) {

          for (int j = 0; j < ENTRY_ATTEMPTS; ++j) {
            Thread.sleep(0);
            System.out.println("Entry Attempt " + j + " Thread " + getId());
            // gain single-access lock record in database...
            String lockedResourceName = "TASK_LOCK_TESTING";
            String creationLockId = UUID.randomUUID().toString();
            Datastore ds = callingContext.getDatastore();
            User user = callingContext.getCurrentUser();

            int i = 0;
            boolean locked = false;
            while (!locked) {
              if ((++i) % 10 == 0) {
                System.out.println("excessive wait count for startup serialization lock. Count: "
                    + i);
                try {
                  Thread.sleep(PersistConsts.MIN_SETTLE_MILLISECONDS);
                } catch (InterruptedException e) {
                  // we remain in the loop even if we get kicked out.
                }
              } else if (i != 1) {
                try {
                  Thread.sleep(PersistConsts.MIN_SETTLE_MILLISECONDS);
                } catch (InterruptedException e) {
                  // we remain in the loop even if we get kicked out.
                }
              }
              try {
                TaskLock formCreationTaskLock = ds.createTaskLock(user);
                if (formCreationTaskLock.obtainLock(creationLockId, lockedResourceName,
                    TaskLockType.CREATE_FORM)) {
                  locked = true;
                }
                formCreationTaskLock = null;
              } catch (ODKTaskLockException e) {
                e.printStackTrace();
              }
            }
            declinedEntryCount += i;
            System.out.println("Entered " + j + " Thread " + getId());

            // we hold the lock while we toggle inside value here...
            try {
              if (!locked) {
                lockFailedCount++;
              } else {
                if (inside.get()) {
                  System.out.println("Thread " + this.getId() + " finds inside true!");
                  failed = true;
                }
                inside.set(true);
                Thread.sleep(0); // give some other thread a spin...
                inside.set(false);
                ++entryCount;
                System.out.println("Thread " + this.getId() + " inside " + entryCount);
              }
            } finally {
              // release the form creation serialization lock
              try {
                for (i = 0; i < 10; i++) {
                  TaskLock formCreationTaskLock = ds.createTaskLock(user);
                  if (formCreationTaskLock.releaseLock(creationLockId, lockedResourceName,
                      TaskLockType.CREATE_FORM)) {
                    break;
                  }
                  formCreationTaskLock = null;
                  try {
                    Thread.sleep(PersistConsts.MIN_SETTLE_MILLISECONDS);
                  } catch (InterruptedException e) {
                    // just move on, this retry mechanism
                    // is to make things nice
                  }
                }
              } catch (ODKTaskLockException e) {
                e.printStackTrace();
              }
            }

          }
        }
      } catch (Exception e) {
        failed = true;
        System.out.println("FAILED Entry Attempt " + e.toString() + " Thread " + getId());
      }
    }
  }

  @Test
  public void verifyLock() throws ODKDatastoreException {

    int MAX_THREADS = 8;
    CyclicBarrier launchBarrier = new CyclicBarrier(MAX_THREADS);

    List<TaskLockThread> lockTesters = new ArrayList<TaskLockThread>();
    for (int i = 0; i < MAX_THREADS; ++i) {
      TaskLockThread t = new TaskLockThread(launchBarrier, callingContext);
      t.start();
      lockTesters.add(t);
    }

    boolean failure = false;
    for (int i = 0; i < MAX_THREADS; ++i) {
      TaskLockThread t = lockTesters.get(i);
      try {
        t.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
        failure = true;
      }
    }

    int entryTally = 0;
    int declinedEntryTally = 0;

    for (int i = 0; i < MAX_THREADS; ++i) {
      TaskLockThread t = lockTesters.get(i);
      if (t.failed) {
        System.out.println("FAILED Thread " + t.getId());
        failure = true;
      }
      entryTally += t.entryCount;
      declinedEntryTally += t.declinedEntryCount;
    }

    System.out.println("entryCount " + entryTally + " of " + TaskLockThread.ENTRY_ATTEMPTS
        * MAX_THREADS);
    System.out.println("declinedEntryCount " + declinedEntryTally);

    assertEquals(failure, false);
  }
}
