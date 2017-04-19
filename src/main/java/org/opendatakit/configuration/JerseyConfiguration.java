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
package org.opendatakit.configuration;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.opendatakit.api.filter.GzipReaderInterceptor;
import org.opendatakit.api.filter.MultipartFormDataToMixedInterceptor;
import org.opendatakit.api.filter.ProxyUrlSetFilter;
import org.opendatakit.api.odktables.DataService;
import org.opendatakit.api.odktables.DiffService;
import org.opendatakit.api.odktables.FileManifestService;
import org.opendatakit.api.odktables.FileService;
import org.opendatakit.api.odktables.InstanceFileService;
import org.opendatakit.api.odktables.OdkTables;
import org.opendatakit.api.odktables.QueryService;
import org.opendatakit.api.odktables.RealizedTableService;
import org.opendatakit.api.odktables.TableAclService;
import org.opendatakit.api.odktables.TableService;
import org.opendatakit.api.odktables.mapper.IOExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.IOExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.IOExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKDatastoreExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKTablesExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.ODKTaskLockExceptionTextXmlMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionApplicationXmlMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionJsonMapper;
import org.opendatakit.api.odktables.mapper.RuntimeExceptionTextXmlMapper;
import org.opendatakit.api.users.Roles;
import org.opendatakit.api.users.Users;
import org.opendatakit.odktables.entity.serialization.SimpleHTMLMessageWriter;
import org.opendatakit.odktables.entity.serialization.SimpleJSONMessageReaderWriter;
import org.opendatakit.odktables.entity.serialization.SimpleXMLMessageReaderWriter;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("")
public class JerseyConfiguration extends ResourceConfig {
  public JerseyConfiguration() {
    registerEndpoints();
  }

  private void registerEndpoints() {
    // Uncomment to handle proxied URLs
    // register(ProxyUrlSetFilter.class);
    register(GzipReaderInterceptor.class);
    register(MultipartFormDataToMixedInterceptor.class);

    
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
