package org.opendatakit.security.spring;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.authentication.encoding.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;

/**
 * Imitate the password encoder for Digest, so that Basic Auth can use the same password field in the database. This
 * is convenient for spin-up testing and development. It can be disabled when Basic Auth is
 * disabled, and Digest Auth can continue to be used.
 * 
 * Format is as A1 value in RFC 2617.
 * 
 * @author Caden Howell <cadenh@benetech.org>
 *
 */
public class BasicUsingDigestPasswordEncoder extends MessageDigestPasswordEncoder {

  public BasicUsingDigestPasswordEncoder() {
    super("MD5");
  }

  private String realmName = null;

  /**
   * Salt is username.
   */
  @Override
  public String encodePassword(String rawPass, Object salt) {
    return encodePasswordInA1Format((String) salt, realmName, rawPass);
  }

  /**
   * Salt is username.
   */
  @Override
  public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
    String pass1 = "" + encPass;
    String pass2 = encodePassword(rawPass, salt);

    return PasswordEncoderUtils.equals(pass1, pass2);
  }

  public String getRealmName() {
    return realmName;
  }

  /**
   * From org.springframework.security.web.authentication.www.DigestAuthUtils
   * 
   * @param realmName
   */
  public void setRealmName(String realmName) {
    this.realmName = realmName;
  }

  /**
   * From org.springframework.security.web.authentication.www.DigestAuthUtils
   * 
   * @param username
   * @param realm
   * @param password
   * @return
   */
  static String encodePasswordInA1Format(String username, String realm, String password) {
    String a1 = username + ":" + realm + ":" + password;

    return md5Hex(a1);
  }

  static String md5Hex(String data) {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("No MD5 algorithm available!");
    }

    return new String(Hex.encode(digest.digest(data.getBytes())));
  }

}

/** 
 * From org.springframework.security.authentication.encoding
 * 
 * Utility for constant time comparison to prevent against timing attacks.
 *
 * @author Rob Winch
 *
 */
class PasswordEncoderUtils {

  /**
   * Constant time comparison to prevent against timing attacks.
   * @param expected
   * @param actual
   * @return
   */
  static boolean equals(String expected, String actual) {
      byte[] expectedBytes = bytesUtf8(expected);
      byte[] actualBytes = bytesUtf8(actual);
      int expectedLength = expectedBytes == null ? -1 : expectedBytes.length;
      int actualLength = actualBytes == null ? -1 : actualBytes.length;

      int result = expectedLength == actualLength ? 0 : 1;
      for (int i = 0; i < actualLength; i++) {
          byte expectedByte = expectedLength <= 0 ? 0 : expectedBytes[i % expectedLength];
          byte actualByte = actualBytes[i % actualLength];
          result |= expectedByte ^ actualByte;
      }
      return result == 0;
  }

  private static byte[] bytesUtf8(String s) {
      if (s == null) {
          return null;
      }

      return Utf8.encode(s); // need to check if Utf8.encode() runs in constant time (probably not). This may leak length of string.
  }

  private PasswordEncoderUtils() {
  }
}
