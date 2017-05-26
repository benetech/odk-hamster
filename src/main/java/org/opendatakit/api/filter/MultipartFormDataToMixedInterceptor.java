/* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Change multipart/form-data to multipart/mixed
 *
 * ODK Survey is setting content disposition of individual parts of 
 * the multipart/form-data to "file" instead of "form-data", which breaks
 * the specification for the content type.
 * 
 * Until we can fix the client, we will rewrite the Content Type header
 * to be more forgiving.
 * See ODK-170 Content disposition part header sent by ODK Survey for multipart-form data is invalid value
 */

public class MultipartFormDataToMixedInterceptor implements ReaderInterceptor {

  private static Log logger = LogFactory.getLog(MultipartFormDataToMixedInterceptor.class);
  private static final String BOUNDARY_PARAM = ".*boundary=(.*)[;\\s]?";
  private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_PARAM,Pattern.CASE_INSENSITIVE );


  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {
    
    logger.debug("Content type: " + context.getHeaders().get(HttpHeaders.CONTENT_TYPE));
    List<String> contentTypes = context.getHeaders().get(HttpHeaders.CONTENT_TYPE);
    
    for (int i = 0; i < contentTypes.size(); i++) {
      if (contentTypes.get(i).contains(MediaType.MULTIPART_FORM_DATA)) {
        Map<String,String> params = new HashMap<String,String>();

        // Add boundary parameter here.
        Matcher matcher = BOUNDARY_PATTERN.matcher(contentTypes.get(i));
        if (matcher.matches()) {
          String boundary = matcher.group(1);
          if (StringUtils.isNotBlank(boundary)) {
            params.put("boundary", boundary);
          }
        }
        MediaType mixed = new MediaType("multipart", "mixed", params);

        context.setMediaType(mixed);
        String newContentType = contentTypes.get(i).replace(MediaType.MULTIPART_FORM_DATA, "multipart/mixed");
        context.getHeaders().replace(HttpHeaders.CONTENT_TYPE, Arrays.asList(newContentType));
      }
    }
    return context.proceed();
  }

}
