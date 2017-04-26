/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.security.client;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.opendatakit.security.client.CredentialsInfo;
import org.opendatakit.security.client.RealmSecurityInfo;

/**
 * Handles creating the CredentialsInfo object (encrypted passwords) for a user.  These are then
 * passed in the open to the server, where they are used to directly update the credentials there. 
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class CredentialsInfoBuilder {

	// must match that in Spring Security core.codec.Hex
	private static char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	
	private static String genHash(MessageDigest md, String fullSourceString, int len )
							throws NoSuchAlgorithmException {
		byte[] asBytes;
		try {
			asBytes = fullSourceString.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new NoSuchAlgorithmException("Cannot get UTF-8 encoding " + e.getMessage());
		}
		md.update(asBytes);
		
		byte[] messageDigest = md.digest();
		StringBuilder b = new StringBuilder();
		int i = len;
		while ( (2*messageDigest.length) < i ) {
			b.append("0");
			--i;
		}
		
		for ( int j = 0 ; j < messageDigest.length ; ++j ) {
			byte v = messageDigest[j];
			int hi = (v & 0xF0) >> 4;
			int lo = (v & 0x0F);
			b.append(hexDigits[hi]);
			b.append(hexDigits[lo]);
		}
		return b.toString();
	}
	
	public static CredentialsInfo build(String username, RealmSecurityInfo realmInfo, String rawPassword)
							throws NoSuchAlgorithmException {
		// compute the digest auth...
		String fullDigestAuth = username + ":" + realmInfo.getRealmString() + ":" + rawPassword;
		MessageDigest md = MessageDigest.getInstance("MD5");
		String digestAuthHash = genHash(md, fullDigestAuth, 32 );

		String basicAuthSalt = null;
		String basicAuthHash = null;
		
		return new CredentialsInfo(username, digestAuthHash, basicAuthHash, basicAuthSalt );
	}
}
