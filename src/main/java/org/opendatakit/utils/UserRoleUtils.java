package org.opendatakit.utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.constants.SecurityConsts;
import org.opendatakit.security.common.GrantedAuthorityName;

public class UserRoleUtils {
  public static void processRoles(TreeSet<GrantedAuthorityName> grants, Map<String, Object> hashMap) {
    ArrayList<String> roleNames = new ArrayList<String>();
    for (GrantedAuthorityName grant : grants) {
      if (grant.name().startsWith(GrantedAuthorityName.ROLE_PREFIX)) {
        roleNames.add(grant.name());
      }
    }
    hashMap.put(SecurityConsts.ROLES, roleNames);
  }
  
  public static void processRoles(TreeSet<GrantedAuthorityName> grants, UserEntity userEntity) {
    ArrayList<String> roleNames = new ArrayList<String>();
    for (GrantedAuthorityName grant : grants) {
      if (grant.name().startsWith(GrantedAuthorityName.ROLE_PREFIX)) {
        roleNames.add(grant.name());
      }
    }
    userEntity.setRoles(roleNames);
  }
}
