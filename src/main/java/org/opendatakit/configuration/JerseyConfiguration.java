package org.opendatakit.configuration;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.opendatakit.aggregate.odktables.api.DataService;
import org.opendatakit.aggregate.odktables.api.DiffService;
import org.opendatakit.aggregate.odktables.api.FileManifestService;
import org.opendatakit.aggregate.odktables.api.FileService;
import org.opendatakit.aggregate.odktables.api.InstanceFileService;
import org.opendatakit.aggregate.odktables.api.OdkTables;
import org.opendatakit.aggregate.odktables.api.QueryService;
import org.opendatakit.aggregate.odktables.api.RealizedTableService;
import org.opendatakit.aggregate.odktables.api.TableAclService;
import org.opendatakit.aggregate.odktables.api.TableService;
import org.opendatakit.aggregate.odktables.api.filter.GzipReaderInterceptor;
import org.opendatakit.aggregate.odktables.api.mapper.IOExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.IOExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.api.mapper.IOExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKDatastoreExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKDatastoreExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKDatastoreExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTablesExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTablesExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTablesExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTaskLockExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTaskLockExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.api.mapper.ODKTaskLockExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.RuntimeExceptionApplicationXmlMapper;
import org.opendatakit.aggregate.odktables.api.mapper.RuntimeExceptionJsonMapper;
import org.opendatakit.aggregate.odktables.api.mapper.RuntimeExceptionTextXmlMapper;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleHTMLMessageWriter;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleJSONMessageReaderWriter;
import org.opendatakit.aggregate.odktables.entity.serialization.SimpleXMLMessageReaderWriter;
import org.opendatakit.aggregate.users.api.Roles;
import org.opendatakit.aggregate.users.api.Users;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("")
public class JerseyConfiguration extends ResourceConfig {
  public JerseyConfiguration() {
    registerEndpoints();
  }

  private void registerEndpoints() {
    
    register(GzipReaderInterceptor.class);
    
    register(MultiPartFeature.class);
    
    // ODK Tables Synchronization 2.0 API
    register(OdkTables.class);
    register(DataService.class);
    register(DiffService.class);
    register(FileManifestService.class);
    register(FileService.class);
    register(InstanceFileService.class);
    register(QueryService.class);
    register(RealizedTableService.class);
    register(TableAclService.class);
    register(TableService.class);

    // Legacy User/Roles ODK 1.0 API
    register(Roles.class);
    register(Users.class);
    
    // Mapper classes
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
