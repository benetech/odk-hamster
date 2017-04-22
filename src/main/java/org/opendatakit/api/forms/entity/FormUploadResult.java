package org.opendatakit.api.forms.entity;

import java.util.List;

import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName="result")
public class FormUploadResult {
  
  @JacksonXmlElementWrapper(useWrapping=false)
  @JacksonXmlProperty(localName="notProcessedFiles")
  List<String> notProcessedFiles;
  
  @JacksonXmlElementWrapper(useWrapping=false)
  @JacksonXmlProperty(localName="manifest")
  OdkTablesFileManifest manifest;
  
  
  public List<String> getNotProcessedFiles() {
    return notProcessedFiles;
  }
  public void setNotProcessedFiles(List<String> notProcessedFiles) {
    this.notProcessedFiles = notProcessedFiles;
  }
  public OdkTablesFileManifest getManifest() {
    return manifest;
  }
  public void setManifest(OdkTablesFileManifest manifest) {
    this.manifest = manifest;
  }
  
  
  
}
