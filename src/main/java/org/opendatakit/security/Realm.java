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
package org.opendatakit.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * A bean class used to capture configuration values about this server
 * deployment, its default mailto: domain and the service domains it authorizes.
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class Realm implements InitializingBean {

  private String hostname;
  private String realmString;


  public Realm() {
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (realmString == null) {
      throw new IllegalStateException(
          "realmString (e.g., mydomain.org ODK Aggregate 1.0) must be specified");
    }
    Log log = LogFactory.getLog(Realm.class);
    log.info("Hostname: " + hostname);
    log.info("RealmString: " + realmString);
    log.info("java.library.path: " + System.getProperty("java.library.path"));
  }



  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getRealmString() {
    return realmString;
  }

  public void setRealmString(String realmString) {
    this.realmString = realmString;
  }

}
