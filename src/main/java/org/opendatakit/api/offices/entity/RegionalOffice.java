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

package org.opendatakit.api.offices.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "office")
public class RegionalOffice {
  private static final long serialVersionUID = 762805275766667475L;

  @JsonProperty(required = false)
  private String name;

  @JsonProperty(required = false)
  private String officeID;

  @JsonProperty(required = false)
  private String uri;


  public RegionalOffice() {}

  public RegionalOffice(String name, String officeID) {
    this.name = name;
    this.officeID = officeID;
  }

  public RegionalOffice(String URI, String name, String officeID) {
    this.uri = URI;
    this.name = name;
    this.officeID = officeID;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOfficeID() {
    return officeID;
  }

  public void setOfficeID(String officeID) {
    this.officeID = officeID;
  }

  public String getUri() {
    return uri;
  }

  public void getUri(String URI) {
    this.uri = URI;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((officeID == null) ? 0 : officeID.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    RegionalOffice other = (RegionalOffice) obj;
    if (uri == null) {
      if (other.uri != null)
        return false;
    } else if (!uri.equals(other.uri))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (officeID == null) {
      if (other.officeID != null)
        return false;
    } else if (!officeID.equals(other.officeID))
      return false;
    return true;
  }
  
  
}
