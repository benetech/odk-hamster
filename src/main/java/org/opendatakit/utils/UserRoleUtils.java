package org.opendatakit.utils;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;

import org.opendatakit.api.users.entity.UserEntity;
import org.opendatakit.constants.SecurityConsts;
import org.opendatakit.security.User;
import org.opendatakit.security.client.UserSecurityInfo.UserType;
import org.opendatakit.security.common.EmailParser;
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

	public static UserEntity getEntityFromUserSecurityInfo(
			org.opendatakit.security.client.UserSecurityInfo userSecurityInfo) {
		UserEntity userEntity = new UserEntity();

		if (userSecurityInfo.getType() == UserType.ANONYMOUS) {
			userEntity.setUserId("anonymous");
			userEntity.setFullName(User.ANONYMOUS_USER_NICKNAME);
		} else {
			if (userSecurityInfo.getEmail() == null) {
				userEntity.setOfficeId(userSecurityInfo.getOfficeId());
				userEntity.setUserId("username:" + userSecurityInfo.getUsername());
				if (userSecurityInfo.getFullName() == null) {
					userEntity.setFullName(userSecurityInfo.getUsername());
				} else {
					userEntity.setFullName(userSecurityInfo.getFullName());
				}
			} else {
				userEntity.setUserId(userSecurityInfo.getEmail());
				if (userSecurityInfo.getFullName() == null) {
					userEntity.setFullName(userSecurityInfo.getEmail().substring(EmailParser.K_MAILTO.length()));
				} else {
					userEntity.setFullName(userSecurityInfo.getFullName());
				}
			}
		}
		UserRoleUtils.processRoles(userSecurityInfo.getGrantedAuthorities(), userEntity);
		return userEntity;
	}
}
