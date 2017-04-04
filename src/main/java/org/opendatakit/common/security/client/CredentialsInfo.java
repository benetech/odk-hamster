/*
 * Copyright (C) 2011 University of Washington
 *
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

package org.opendatakit.common.security.client;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Transport object for communicating password changes between GWT client and server.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public class CredentialsInfo implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -8984148650618368023L;
  String username;
  String digestAuthHash;
  String basicAuthHash;
  String basicAuthSalt;
  private static final String URL_ENCODING = "UTF-8";
  private static Log logger = LogFactory.getLog(CredentialsInfo.class);

  public String getRequestParameters() {
    StringBuilder postData = new StringBuilder();
    // note param pairs are separated by a '&'
    // and each key-value pair is separated by a '='
    try {
      postData.append("username=").append(URLEncoder.encode(username, URL_ENCODING));
      postData.append("&");
      postData.append("digestAuthHash=").append(URLEncoder.encode(digestAuthHash, URL_ENCODING));
      if (basicAuthHash != null && basicAuthSalt != null) {
        postData.append("&");
        postData.append("basicAuthHash=").append(URLEncoder.encode(basicAuthHash, URL_ENCODING));
        postData.append("&");
        postData.append("basicAuthSalt=").append(URLEncoder.encode(basicAuthSalt, URL_ENCODING));
      }
    } catch (UnsupportedEncodingException e) {
      logger.error("Unable to encode Request Parameters!", e);
    }
    return postData.toString();
  }

  public CredentialsInfo() {}

  public CredentialsInfo(String username, String digestAuthHash, String basicAuthHash,
      String basicAuthSalt) {
    this.username = username;
    this.digestAuthHash = digestAuthHash;
    this.basicAuthHash = basicAuthHash;
    this.basicAuthSalt = basicAuthSalt;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDigestAuthHash() {
    return digestAuthHash;
  }

  public void setDigestAuthHash(String digestAuthHash) {
    this.digestAuthHash = digestAuthHash;
  }

  public String getBasicAuthHash() {
    return basicAuthHash;
  }

  public void setBasicAuthHash(String basicAuthHash) {
    this.basicAuthHash = basicAuthHash;
  }

  public String getBasicAuthSalt() {
    return basicAuthSalt;
  }

  public void setBasicAuthSalt(String basicAuthSalt) {
    this.basicAuthSalt = basicAuthSalt;
  }

}
