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
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transparently decompress incoming GZIP.
 * 
 * https://jersey.java.net/documentation/latest/filters-and-interceptors.html#d0e8344
 * http://stackoverflow.com/questions/25542450/gzip-format-decompress-jersey
 */

public class GzipReaderInterceptor implements ReaderInterceptor {

  private static final String GZIP = "gzip";

  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {

    List<String> contentEncodings = context.getHeaders().get(HttpHeaders.CONTENT_ENCODING);
    
    if (contentEncodings != null && contentEncodings.contains(GZIP)) {
      final InputStream originalInputStream = context.getInputStream();
      context.setInputStream(new GZIPInputStream(originalInputStream));
    }
    return context.proceed();
  }

}
