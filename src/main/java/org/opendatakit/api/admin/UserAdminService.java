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
package org.opendatakit.api.admin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.constants.BasicConsts;
import org.opendatakit.constants.ErrorConsts;
import org.opendatakit.constants.SecurityConsts;
import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.Datastore;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.persistence.exception.ODKDatastoreException;
import org.opendatakit.persistence.table.OdkRegionalOfficeTable;
import org.opendatakit.persistence.table.RegisteredUsersTable;
import org.opendatakit.persistence.table.UserGrantedAuthority;
import org.opendatakit.security.User;
import org.opendatakit.security.client.UserSecurityInfo;
import org.opendatakit.security.client.UserSecurityInfo.UserType;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.opendatakit.security.common.EmailParser;
import org.opendatakit.security.server.SecurityServiceUtil;
import org.opendatakit.utils.SecurityUtils;
import org.opendatakit.utils.UserRoleUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Authorization;

@Api(value = "/admin/users", description = "ODK User Admin API",
    authorizations = {@Authorization(value = "basicAuth")})
@Path("/admin/users")
public class UserAdminService {
  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(UserAdminService.class);

  private static final ObjectMapper mapper = new ObjectMapper();

  @GET
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getList() throws IOException {
    return internalGetList(callingContext);
  }

  @GET
  @Path("/username:{username}")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getUser(@PathParam("username") String username) {
    UserEntity resultUserEntity = null;
    try {

      RegisteredUsersTable user = RegisteredUsersTable.getUserByUsername(username,
          callingContext.getUserService(), callingContext.getDatastore());

      if (user == null) {
        return Response.status(Status.NOT_FOUND).entity("User not found.")
            .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Credentials", "true").build();
      }
      UserSecurityInfo userSecurityInfo =
          new UserSecurityInfo(user.getUsername(), user.getFullName(), user.getEmail(),
              UserSecurityInfo.UserType.REGISTERED, user.getOfficeId());

      SecurityServiceUtil.setAuthenticationLists(userSecurityInfo, user.getUri(), callingContext);
      resultUserEntity = new UserEntity(userSecurityInfo);

    } catch (ODKDatastoreException e) {
      logger.error("Error retrieving ", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    return Response.ok().entity(resultUserEntity).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }


  /**
   * Update user password
   *
   * @throws DatastoreFailureException
   *
   */
  @ApiOperation(
      value = "Set a password in cleartext.  Probably a good idea to disable this endpoint in production.")
  @POST
  @Path("username:{username}/password")
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response setUserPassword(@PathParam("username") String username, String password)
      throws AccessDeniedException, DatastoreFailureException {

    SecurityUtils.updateCleartextPassword(callingContext, username, password);

    return Response.status(Status.OK)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  /**
   * Update user password in digest format
   *
   * @throws DatastoreFailureException
   *
   */
  @ApiOperation(value = "Set a password using digest hash.")
  @POST
  @Path("username:{username}/password/digest")
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response setUserDigestPassword(@PathParam("username") String username, String password)
      throws AccessDeniedException, DatastoreFailureException {

    SecurityUtils.updateDigestPassword(callingContext, username, password);

    return Response.status(Status.OK)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }


  /**
   * Add or update user to database.
   *
   * @throws DatastoreFailureException
   */
  @POST
  @Path("/")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  @Consumes({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response putUser(UserEntity userEntity)
      throws AccessDeniedException, DatastoreFailureException {

    try {

      String fullName = userEntity.getFullName();
      String officeId = userEntity.getOfficeId();
      String userId = userEntity.getUserId();

      String username = null;
      String email = null;
      if (userId.toLowerCase().startsWith(EmailParser.K_MAILTO)) {
        email = userId.substring(EmailParser.K_MAILTO.length());
      }
      if (userId.toLowerCase().startsWith(SecurityConsts.USERNAME_COLON)) {
        username = userId.substring(SecurityConsts.USERNAME_COLON.length());
      }

      @SuppressWarnings("unchecked")
      List<String> roles = userEntity.getRoles();
      UserSecurityInfo userSecurityInfo = new UserSecurityInfo(username, fullName, email,
          UserSecurityInfo.UserType.REGISTERED, officeId);

      RegisteredUsersTable user =
          RegisteredUsersTable.assertActiveUserByUserSecurityInfo(userSecurityInfo, callingContext);

      UserGrantedAuthority.assertUserGrantedAuthorities(user.getUri(), roles, callingContext);

      UserSecurityInfo resultUserSecurityInfo =
          new UserSecurityInfo(user.getUsername(), user.getFullName(), user.getEmail(),
              UserSecurityInfo.UserType.REGISTERED, user.getOfficeId());

      SecurityServiceUtil.setAuthenticationLists(resultUserSecurityInfo, user.getUri(), callingContext);
      UserEntity resultUserEntity = new UserEntity(resultUserSecurityInfo);

      String eTag = Integer.toHexString(resultUserEntity.hashCode());

      return Response.status(Status.CREATED).entity(resultUserEntity).header(HttpHeaders.ETAG, eTag)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();

    } catch (ODKDatastoreException e) {
      logger.error("Error creating/updating user", e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_BAD_REQUEST);

    }

  }

  @DELETE
  @Path("username:{username}")
  public Response deleteUser(@PathParam("username") String username)
      throws IOException, DatastoreFailureException {
    Datastore ds = callingContext.getDatastore();
    User user = callingContext.getCurrentUser();
    try {
      RegisteredUsersTable deleteUser = RegisteredUsersTable.getUserByUsername(username,
          callingContext.getUserService(), callingContext.getDatastore());
      if (deleteUser != null) {
        UserGrantedAuthority.deleteGrantedAuthoritiesForUser(deleteUser.getUri(),
            callingContext.getUserService(), callingContext.getDatastore(), user);
        ds.deleteEntity(deleteUser.getEntityKey(), user);
      }
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
      throw new DatastoreFailureException(e);
    }
    return Response.status(Status.NO_CONTENT)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }

  public static Response internalGetList(CallingContext callingContext)
      throws JsonProcessingException {
    ArrayList<UserEntity> listOfUsers = new ArrayList<UserEntity>();

    UserEntity userEntity;
    try {
      ArrayList<UserSecurityInfo> allUsers = SecurityServiceUtil.getAllUsers(true, callingContext);
      for (UserSecurityInfo i : allUsers) {
        userEntity = new UserEntity(i);
        listOfUsers.add(userEntity);
      }
    } catch (DatastoreFailureException e) {
      logger.error("Retrieving users persistence error: " + e.toString(), e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    } catch (AccessDeniedException e) {
      logger.error("Retrieving users access denied error: " + e.toString(), e);
      throw new WebApplicationException(ErrorConsts.PERSISTENCE_LAYER_PROBLEM + "\n" + e.toString(),
          HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

    }
    // Need to set host header? original has
    // resp.addHeader(HttpHeaders.HOST, cc.getServerURL());

    return Response.ok(mapper.writeValueAsString(listOfUsers)).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }
}
