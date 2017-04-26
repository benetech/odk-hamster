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
package org.opendatakit.context;

import org.opendatakit.persistence.Datastore;
import org.opendatakit.security.User;
import org.opendatakit.security.UserService;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;

/**
 * Context in which the call occurs.  
 * 
 * This is a legacy wrapper for various context information held over from ODK Aggregate
 * It's being whittled down and responsibility for dependency injection is moving to Spring
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public interface CallingContext {

   
   /**
    * @return the datastore
    */
   public Datastore getDatastore();
   
   /**
    * @return the user identity service
    */
   public UserService getUserService();
   
   /**
    * @return the role hierarchy service
    */
   public RoleHierarchy getHierarchicalRoleRelationships();
   
   /**
    * Set whether or not we should act as the daemon user.
    * Effectively a run-as feature, but not necessarily
    * linked with security.
    * 
    * @param asDaemon
    */
   public void setAsDaemon(boolean asDaemon );
   
   /**
    * @return whether or not we are acting as the daemon user.
    */
   public boolean getAsDaemon();
   
   /**
    * @return the logged-in user, anonymous user, or the daemon user.
    */
   public User getCurrentUser();
   
   /**
    * @return the slash-rooted path of this web application
    */
   public String getWebApplicationURL();
   
   /**
    * Use this to form the URLs for pages within this web application.
    * 
    * @param servletAddr -- the root-relative path of a servlet
    * @return the slash-rooted path for the servlet within this web application
    */
   public String getWebApplicationURL(String servletAddr);
   

}