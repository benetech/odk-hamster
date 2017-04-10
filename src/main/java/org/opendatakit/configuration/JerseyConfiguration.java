package org.opendatakit.configuration;

import org.glassfish.jersey.server.ResourceConfig;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleHTMLMessageWriter;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleJSONMessageReaderWriter;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleXMLMessageReaderWriter;
import org.opendatakit.aggregate.odktables.impl.api.IOExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.IOExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.impl.api.IOExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKDatastoreExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKDatastoreExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKDatastoreExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTablesExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTablesExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTablesExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTaskLockExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTaskLockExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.impl.api.ODKTaskLockExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.OdkTablesImpl;
import org.opendatakit.aggregate.odktables.impl.api.RuntimeExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.impl.api.RuntimeExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.impl.api.RuntimeExceptionTextXmlMapper;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfiguration extends ResourceConfig {
  public JerseyConfiguration() {
    registerEndpoints();
  }

  private void registerEndpoints() {
    register(OdkTablesImpl.class);
    register(SimpleHTMLMessageWriter.class);
    register(SimpleJSONMessageReaderWriter.class);
    register(SimpleXMLMessageReaderWriter.class);
    register(ODKDatastoreExceptionJsonMapper.class);
    register(ODKDatastoreExceptionTextXmlMapper.class);
    register(ODKDatastoreExceptionApplicationXmlMapper.class);
    register(ODKTablesExceptionJsonMapper.class);
    register(ODKTablesExceptionTextXmlMapper.class);
    register(ODKTablesExceptionApplicationXmlMapper.class);
    register(ODKTaskLockExceptionJsonMapper.class);
    register(ODKTaskLockExceptionTextXmlMapper.class);
    register(ODKTaskLockExceptionApplicationXmlMapper.class);
    register(IOExceptionJsonMapper.class);
    register(IOExceptionTextXmlMapper.class);
    register(IOExceptionApplicationXmlMapper.class);
    register(RuntimeExceptionJsonMapper.class);
    register(RuntimeExceptionTextXmlMapper.class);
    register(RuntimeExceptionApplicationXmlMapper.class);
     
  }
}
