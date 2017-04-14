package org.opendatakit.aggregate.users.api;

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
import org.opendatakit.common.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.client.UserSecurityInfo;
import org.opendatakit.common.security.client.UserSecurityInfo.UserType;
import org.opendatakit.common.security.client.exception.AccessDeniedException;
import org.opendatakit.common.security.common.EmailParser;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.security.spring.RegisteredUsersTable;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.ErrorConsts;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("users")
public class Users {

  @Autowired
  private CallingContext callingContext;

  private static final Log logger = LogFactory.getLog(Users.class);


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
