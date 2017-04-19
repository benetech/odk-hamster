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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;

/**
 * This is the brute force approach to dealing with an external proxy address that doesn't match the
 * address we see in the request object (i.e. pretty.external.com instead of localhost)
 * 
 * @author Caden Howell
 *
 */
public class ProxyUrlSetFilter implements ContainerRequestFilter {
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    UriInfo uriInfo = requestContext.getUriInfo();
    String externalRootUrl = System.getenv("EXTERNAL_ROOT_URL");
    if (StringUtils.isNotEmpty(externalRootUrl)) {
      URI baseUri = URI.create(externalRootUrl);
      requestContext.setRequestUri(baseUri, uriInfo.getRequestUri());
    }
  }
}
