package org.opendatakit.aggregate.users.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.common.security.common.GrantedAuthorityName;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("roles")
public class Roles {

  @Autowired
  private CallingContext callingContext;


  private static final ObjectMapper mapper = new ObjectMapper();

  

  @GET
  @Path("granted")
  @Produces({MediaType.APPLICATION_JSON, ApiConstants.MEDIA_TEXT_XML_UTF8,
      ApiConstants.MEDIA_APPLICATION_XML_UTF8})
  public Response getGranted(@Context ServletContext sc, @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders) throws IOException {
    
    Set<GrantedAuthority> grants = callingContext.getCurrentUser().getDirectAuthorities();
    RoleHierarchy rh = (RoleHierarchy) callingContext.getHierarchicalRoleRelationships();
    Collection<? extends GrantedAuthority> roles = rh.getReachableGrantedAuthorities(grants);
    ArrayList<String> roleNames = new ArrayList<String>();
    for ( GrantedAuthority a : roles ) {
      if (a.getAuthority().startsWith(GrantedAuthorityName.ROLE_PREFIX)) {
        roleNames.add(a.getAuthority());
      }
    }
    
    // Need to set host header?  original has     
    // resp.addHeader(HttpHeaders.HOST, cc.getServerURL());

    return Response.ok(mapper.writeValueAsString(roleNames)).encoding(BasicConsts.UTF8_ENCODE)
        .type(MediaType.APPLICATION_JSON)
        .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
        .header("Access-Control-Allow-Origin", "*")
        .header("Access-Control-Allow-Credentials", "true").build();
  }
  
}
