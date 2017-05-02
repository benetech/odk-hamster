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
package org.opendatakit.api.users.entity;

import java.util.Arrays;
import java.util.List;

import org.opendatakit.security.User;
import org.opendatakit.security.client.UserSecurityInfo;
import org.opendatakit.security.client.UserSecurityInfo.UserType;
import org.opendatakit.security.common.EmailParser;
import org.opendatakit.utils.UserRoleUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * For type safety passing user "on the wire" with Rest endpoints.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
@JacksonXmlRootElement(localName = "user")
@JsonRootName("user")
public class UserEntity {
  
  // Legacy ODK 1.0 Naming
  @JsonProperty(value="user_id", required = false)
  @JacksonXmlProperty(localName="user_id")
  String userId;
  
  @JsonProperty(value="full_name",required = false)
  @JacksonXmlProperty(localName="full_name")
  String fullName;
  

  @JsonProperty(required = false)
  List<String> roles;
  
  @JsonProperty(required = false)
  @JsonInclude(Include.NON_NULL)
  String officeId;


  
  public UserEntity() {
    super();
  }

  public UserEntity(String user_id, String full_name, String officeId, String... roles) {
    this(user_id, full_name, officeId, Arrays.asList(roles));
  }

  public UserEntity(String user_id, String full_name, String officeId, List<String> roles) {
    this.roles = roles;
    this.officeId = officeId;
    this.fullName = full_name;
    this.userId = user_id;
  }

  public UserEntity(UserSecurityInfo userSecurityInfo) {

    if (userSecurityInfo.getType() == UserType.ANONYMOUS) {
      this.userId = "anonymous";
      this.fullName = User.ANONYMOUS_USER_NICKNAME;
    } else {
      if (userSecurityInfo.getEmail() == null) {
        this.officeId = userSecurityInfo.getOfficeId();
        this.userId = "username:" + userSecurityInfo.getUsername();
        if (userSecurityInfo.getFullName() == null) {
          this.fullName = userSecurityInfo.getUsername();
        } else {
          this.fullName = userSecurityInfo.getFullName();
        }
      } else {
        this.userId = userSecurityInfo.getEmail();
        if (userSecurityInfo.getFullName() == null) {
          this.fullName = userSecurityInfo.getEmail().substring(EmailParser.K_MAILTO.length());
        } else {
          this.fullName = userSecurityInfo.getFullName();
        }
      }
    }
    UserRoleUtils.processRoles(userSecurityInfo.getGrantedAuthorities(),this);
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String user_id) {
    this.userId = user_id;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String full_name) {
    this.fullName = full_name;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public String getOfficeId() {
    return officeId;
  }

  public void setOfficeId(String officeId) {
    this.officeId = officeId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
    result = prime * result + ((officeId == null) ? 0 : officeId.hashCode());
    result = prime * result + ((roles == null) ? 0 : roles.hashCode());
    result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
    UserEntity other = (UserEntity) obj;
    if (fullName == null) {
      if (other.fullName != null)
        return false;
    } else if (!fullName.equals(other.fullName))
      return false;
    if (officeId == null) {
      if (other.officeId != null)
        return false;
    } else if (!officeId.equals(other.officeId))
      return false;
    if (roles == null) {
      if (other.roles != null)
        return false;
    } else if (!roles.equals(other.roles))
      return false;
    if (userId == null) {
      if (other.userId != null)
        return false;
    } else if (!userId.equals(other.userId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "UserEntity [userId=" + userId + ", fullName=" + fullName + ", roles=" + roles
        + ", officeId=" + officeId + "]";
  }
  
  

}
