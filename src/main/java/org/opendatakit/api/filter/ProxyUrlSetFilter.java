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
package org.opendatakit.api.filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.api.users.UserService;

/**
 * This is the brute force approach to dealing with an external proxy address that doesn't match the
 * address we see in the request object (i.e. pretty.external.com instead of localhost or https instead of http)
 * 
 * @author Caden Howell
 *
 */
@PreMatching
public class ProxyUrlSetFilter implements ContainerRequestFilter {

  private static final Log logger = LogFactory.getLog(ProxyUrlSetFilter.class);

  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    String forwardedPort = requestContext.getHeaderString("x-forwarded-port");
    String forwardedProto = requestContext.getHeaderString("x-forwarded-proto");
    String host = requestContext.getHeaderString("host");

    UriInfo uriInfo = requestContext.getUriInfo();
    URI requestUri = uriInfo.getRequestUri();
    URI baseUri = uriInfo.getBaseUri();
    int forwardedPortInt = uriInfo.getRequestUri().getPort();

    if (StringUtils.isNotEmpty(forwardedPort) || StringUtils.isNotEmpty(forwardedProto)) {
      if (StringUtils.isNotEmpty(forwardedPort)) {
        try {
          forwardedPortInt = Integer.parseInt(forwardedPort);
        } catch (NumberFormatException e) {
          logger.error("Unable to parse x-forwarded-port number " + forwardedPort
              + " Generated URLs in JSON responses may be wrong.");
          // Life goes on. Non-fatal.
        }
      }
      if (StringUtils.isEmpty(forwardedProto)) {
        forwardedProto = uriInfo.getRequestUri().getScheme();
      }
      try {
        URI newRequestUri = new URI(forwardedProto, requestUri.getUserInfo(), host, forwardedPortInt,
            requestUri.getPath(), requestUri.getQuery(), requestUri.getFragment());

        URI newBaseUri =
            new URI(forwardedProto, baseUri.getUserInfo(), host, forwardedPortInt,
                baseUri.getPath(), baseUri.getQuery(), baseUri.getFragment());

        requestContext.setRequestUri(newBaseUri, newRequestUri);
      } catch (URISyntaxException e) {
        logger.error("Unable to update requestUri. Generated URLs in JSON responses may be wrong.");
        // Life goes on. Non-fatal.
      }

    }
  }
}
