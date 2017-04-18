package org.opendatakit.aggregate.odktables.api.filter;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

public class GzipReaderInterceptor implements ReaderInterceptor {

  @Override
  public Object aroundReadFrom(ReaderInterceptorContext context)
      throws IOException, WebApplicationException {
    final InputStream originalInputStream = context.getInputStream();
    context.setInputStream(new GZIPInputStream(originalInputStream));
    return context.proceed();
  }

}
