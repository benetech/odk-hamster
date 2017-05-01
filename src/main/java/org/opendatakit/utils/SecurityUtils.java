package org.opendatakit.utils;

import org.opendatakit.context.CallingContext;
import org.opendatakit.persistence.client.exception.DatastoreFailureException;
import org.opendatakit.security.client.CredentialsInfo;
import org.opendatakit.security.client.exception.AccessDeniedException;
import org.opendatakit.security.server.SecurityServiceUtil;
import org.opendatakit.security.spring.BasicUsingDigestPasswordEncoder;

public class SecurityUtils {

  public static void updateCleartextPassword(CallingContext callingContext, String username,
      String password) throws AccessDeniedException, DatastoreFailureException {
    BasicUsingDigestPasswordEncoder encoder = new BasicUsingDigestPasswordEncoder();
    encoder.setRealmName(callingContext.getUserService().getCurrentRealm().getRealmString());
    String digestAuthHash = encoder.encodePassword(password, username);
    updateDigestPassword(callingContext, username, digestAuthHash);
  }

  public static void updateDigestPassword(CallingContext callingContext, String username,
      String digestAuthHash) throws AccessDeniedException, DatastoreFailureException {
    CredentialsInfo credential = new CredentialsInfo();
    credential.setUsername(username);
    credential.setDigestAuthHash(digestAuthHash);
    SecurityServiceUtil.setUserCredentials(credential, callingContext);
  }

}
