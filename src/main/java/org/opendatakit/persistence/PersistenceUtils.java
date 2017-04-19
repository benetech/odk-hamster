package org.opendatakit.persistence;


public class PersistenceUtils {

  public final static String newUri() {
    return CommonFieldsBase.newUri();
  }

  public final static String newMD5HashUri(byte[] asBytes) {
   return CommonFieldsBase.newMD5HashUri(asBytes);
  }
}
