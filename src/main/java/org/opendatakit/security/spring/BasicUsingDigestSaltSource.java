/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.security.spring;

import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.security.client.UserSecurityInfo;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Fetches the salt used when Basic Auth shares DB password field with Digest Auth.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
public class BasicUsingDigestSaltSource implements SaltSource {



  public BasicUsingDigestSaltSource() {
    // Nothing.
  }

  @Override
  public Object getSalt(UserDetails userDetail) {
    OdkServerUser user = (OdkServerUser) userDetail;
    return UserSecurityInfo.getDisplayName(user.getUsername());

  }

  

}
