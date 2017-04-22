package org.opendatakit.api;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Explicitly redirect the root URL somewhere.  This is not officially part of the API.
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
@Component
@Path("")
public class RootRedirect {
  
  @Value("${root.redirect.api:/index.html}")
  private String rootRedirectUri;
  
  @GET
  public Response getAppNames(@Context ServletContext sc, @Context HttpServletRequest req,
      @Context HttpHeaders httpHeaders) throws URISyntaxException {

    return Response.temporaryRedirect(new URI(rootRedirectUri)).build();
  }
}
