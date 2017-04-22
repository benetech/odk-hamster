/* Licensed under the Apache License, Version 2.0 (the "License"); you may not
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
package org.opendatakit.api.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.security.User;
import org.opendatakit.security.client.UserSecurityInfo;
import org.opendatakit.security.client.UserSecurityInfo.UserType;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.opendatakit.security.common.EmailParser;
import org.opendatakit.security.common.GrantedAuthorityName;
import org.opendatakit.security.server.SecurityServiceUtil;
import org.opendatakit.security.spring.RegisteredUsersTable;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;

@Api(value = "/users", description = "ODK User API")
@Path("users")
public class UserService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(UserService.class);


  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String USER_ID = "user_id";
  private static final String FULL_NAME = "full_name";
  private static final String ROLES = "roles";


  @GET
  @Path("list")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getList(@Context ServletContext sc, @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders) throws IOException {
    TreeSet<GrantedAuthorityName> grants;
    try {
      grants = SecurityServiceUtil.getCurrentUserSecurityInfo(callingContext);
    } catch (ODKDatastoreException e) {
      logger.error("Retrieving users persistence error: " + e.toString());
      e.printStackTrace();
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    boolean returnFullList = false;
    for (GrantedAuthorityName grant : grants) {
      if (grant.equals(GrantedAuthorityName.ROLE_SITE_ACCESS_ADMIN)
          || grant.equals(GrantedAuthorityName.ROLE_ADMINISTER_TABLES)
          || grant.equals(GrantedAuthorityName.ROLE_SUPER_USER_TABLES)) {
        returnFullList = true;
        break;
      }
    }

    // returned object (will be JSON serialized).
    ArrayList<HashMap<String, Object>> listOfUsers = new ArrayList<HashMap<String, Object>>();

    HashMap<String, Object> hashMap;
    if (!returnFullList) {
      // only return ourself -- we don't have privileges to see everyone
      hashMap = new HashMap<String, Object>();
      User user = callingContext.getCurrentUser();
      if (user.isAnonymous()) {
        hashMap.put(USER_ID, "anonymous");
        hashMap.put(FULL_NAME, User.ANONYMOUS_USER_NICKNAME);
      } else {
        RegisteredUsersTable entry;
        try {
          entry = RegisteredUsersTable.getUserByUri(user.getUriUser(),
              callingContext.getDatastore(), callingContext.getCurrentUser());
        } catch (ODKDatastoreException e) {
          logger.error("Retrieving users persistence error: " + e.toString(), e);
          throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        }
        if (user.getEmail() == null) {
          hashMap.put(USER_ID, "username:" + entry.getUsername());
          if (user.getNickname() == null) {
            hashMap.put(FULL_NAME, entry.getUsername());
          } else {
            hashMap.put(FULL_NAME, user.getNickname());
          }
        } else {
          hashMap.put(USER_ID, entry.getEmail());
          if (user.getNickname() == null) {
            hashMap.put(FULL_NAME, entry.getEmail().substring(EmailParser.K_MAILTO.length()));
          } else {
            hashMap.put(FULL_NAME, user.getNickname());
          }
        }
      }
      processRoles(grants, hashMap);
      listOfUsers.add(hashMap);
    } else {
      // we have privileges to see all users -- return the full mapping
      try {
        ArrayList<UserSecurityInfo> allUsers =
            SecurityServiceUtil.getAllUsers(true, callingContext);
        for (UserSecurityInfo i : allUsers) {
          hashMap = new HashMap<String, Object>();
          if (i.getType() == UserType.ANONYMOUS) {
            hashMap.put(USER_ID, "anonymous");
            hashMap.put(FULL_NAME, User.ANONYMOUS_USER_NICKNAME);
          } else if (i.getEmail() == null) {
            hashMap.put(USER_ID, "username:" + i.getUsername());
            if (i.getFullName() == null) {
              hashMap.put(FULL_NAME, i.getUsername());
            } else {
              hashMap.put(FULL_NAME, i.getFullName());
            }
          } else {
            // already has the mailto: prefix
            hashMap.put(USER_ID, i.getEmail());
            if (i.getFullName() == null) {
              hashMap.put(FULL_NAME, i.getEmail().substring(EmailParser.K_MAILTO.length()));
            } else {
              hashMap.put(FULL_NAME, i.getFullName());
            }
          }
          processRoles(i.getGrantedAuthorities(), hashMap);
          listOfUsers.add(hashMap);
        }
      } catch (DatastoreFailureException e) {
        logger.error("Retrieving users persistence error: " + e.toString(), e);
        throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      } catch (AccessDeniedException e) {
        logger.error("Retrieving users access denied error: " + e.toString(), e);
        throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      }
    }


    // Need to set host header? original has
    // resp.addHeader(HttpHeaders.HOST, cc.getServerURL());

    return Response.ok(mapper.writeValueAsString(listOfUsers))
        .encoding(BasicConsts.UTF8_ENCODE).type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }


  private void processRoles(TreeSet<GrantedAuthorityName> grants, HashMap<String, Object> hashMap) {
    ArrayList<String> roleNames = new ArrayList<String>();
    for (GrantedAuthorityName grant : grants) {
      if (grant.name().startsWith(GrantedAuthorityName.ROLE_PREFIX)) {
        roleNames.add(grant.name());
      }
    }
    hashMap.put(ROLES, roleNames);
  }

}
