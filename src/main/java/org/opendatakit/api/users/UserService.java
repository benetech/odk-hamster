/*
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
package org.opendatakit.api.users;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.api.admin.UserAdminService;
import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.constants.SecurityConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.table.RegisteredUsersTable;
import org.opendatakit.security.User;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.opendatakit.security.common.EmailParser;
import org.opendatakit.security.common.GrantedAuthorityName;
import org.opendatakit.security.server.SecurityServiceUtil;
import org.opendatakit.utils.SecurityUtils;
import org.opendatakit.utils.UserRoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "/users", description = "ODK User API",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("users")
public class UserService {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(UserService.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  @ApiOperation(response = UserEntity.class, responseContainer = "List",
      value = "This endpoint is backwards-compatible with ODK Aggregate/Survey sync.  With admin privileges, this call retrieves user information for all users.  Without admin privileges, this retrieves user information for the current user.")
  @GET
  @Path("list")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getList(@Context HttpHeaders httpHeaders) throws IOException {
    TreeSet<GrantedAuthorityName> grants;
    try {
      grants = SecurityServiceUtil.getCurrentUserSecurityInfo(callingContext);
    } catch (ODKDatastoreException e) {
      logger.error("Retrieving users persistence error: " + e.toString());
      e.printStackTrace();
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
    if (!returnFullList) {
      ArrayList<HashMap<String, Object>> listOfUsers = new ArrayList<HashMap<String, Object>>();
      listOfUsers.add(internalGetUser(grants));
      return Response.ok(mapper.writeValueAsString(listOfUsers)).encoding(BasicConsts.UTF8_ENCODE)
          .type(MediaType.APPLICATION_JSON)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      // we have privileges to see all users -- return the full mapping
      return UserAdminService.internalGetList(callingContext);
    }

  }

  @ApiOperation(response = UserEntity.class, value = "Return just the current user.")
  @GET
  @Path("current")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getCurrent(@Context HttpHeaders httpHeaders) throws IOException {
    TreeSet<GrantedAuthorityName> grants;
    
    try {
      grants = SecurityServiceUtil.getCurrentUserSecurityInfo(callingContext);
    } catch (ODKDatastoreException e) {
      logger.error("Retrieving users persistence error: " + e.toString());
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    HashMap<String, Object> userInfoMap  = internalGetUser(grants);
    UserEntity userEntity = new UserEntity((String) userInfoMap.get(SecurityConsts.USER_ID),
        (String) userInfoMap.get(SecurityConsts.FULL_NAME),
        (String) userInfoMap.get(SecurityConsts.OFFICE_ID), 
        (List<String>)userInfoMap.get(SecurityConsts.ROLES));
    
    // Need to set host header? original has
    // resp.addHeader(HttpHeaders.HOST, cc.getServerURL());
    return Response.ok(userEntity)
        .encoding(BasicConsts.UTF8_ENCODE).type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();

  }

  @ApiOperation(
      value = "Set a password in cleartext.  Probably a good idea to disable this endpoint in production.")
  @POST
  @Path("current/password")
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response setUserPassword(String password)
      throws AccessDeniedException, DatastoreFailureException {

    String username = null;
    User user = callingContext.getCurrentUser();
    logger.debug("UriUser: " + user.getUriUser());

    RegisteredUsersTable registeredUsersTable;

    try {
      registeredUsersTable =
          RegisteredUsersTable.getUserByUri(user.getUriUser(), callingContext.getDatastore(), user);
      username = registeredUsersTable.getUsername();
      logger.debug("Username: " + username);
    } catch (ODKDatastoreException e) {
      logger.error("Retrieving users persistence error: " + e.toString());
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    if (StringUtils.isNotBlank(username)) {
      SecurityUtils.updateCleartextPassword(callingContext, username, password);
      return Response.status(Status.OK)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      return Response.status(Status.NOT_FOUND)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    }
  }

  private HashMap<String, Object> internalGetUser(TreeSet<GrantedAuthorityName> grants)
      throws JsonProcessingException {
    HashMap<String, Object> userInfoMap;
    userInfoMap = new HashMap<String, Object>();
    User user = callingContext.getCurrentUser();
    if (user.isAnonymous()) {
      userInfoMap.put(SecurityConsts.USER_ID, "anonymous");
      userInfoMap.put(SecurityConsts.FULL_NAME, User.ANONYMOUS_USER_NICKNAME);
    } else {
      RegisteredUsersTable entry;
      try {
        entry = RegisteredUsersTable.getUserByUri(user.getUriUser(), callingContext.getDatastore(),
            callingContext.getCurrentUser());
      } catch (ODKDatastoreException e) {
        logger.error("Retrieving users persistence error: " + e.toString(), e);
        throw new WebApplicationException(
            ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

      }
      userInfoMap.put(SecurityConsts.OFFICE_ID, entry.getOfficeId());
      if (user.getEmail() == null) {
        userInfoMap.put(SecurityConsts.USER_ID, "username:" + entry.getUsername());
        if (user.getNickname() == null) {
          userInfoMap.put(SecurityConsts.FULL_NAME, entry.getUsername());
        } else {
          userInfoMap.put(SecurityConsts.FULL_NAME, user.getNickname());
        }
      } else {
        userInfoMap.put(SecurityConsts.USER_ID, entry.getEmail());
        if (user.getNickname() == null) {
          userInfoMap.put(SecurityConsts.FULL_NAME,
              entry.getEmail().substring(EmailParser.K_MAILTO.length()));
        } else {
          userInfoMap.put(SecurityConsts.FULL_NAME, user.getNickname());
        }
      }
    }
    UserRoleUtils.processRoles(grants, userInfoMap);
    return userInfoMap;

  }

}
