/*
 * Copyright (C) 2012-2013 University of Washington
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

package org.opendatakit.odktables.relation;

/**
 * @author the.dylan.price@gmail.com
 *
 */
public class RUtil {

  public static final String NAMESPACE = "ODKTABLES";

  /**
   * Converts a uuid to a datastore table and column name friendly format.
   */
  public static String convertIdentifier(String id) {
    id = "table:" + id;
    return id.replace('-', '_').replace(':', '_').toUpperCase();
  }

  /**
   * Return a db-safe version of the proposed column name. This should be
   * equivalent to how it would be on the phone. At the moment replaces spaces
   * with underscores and precedes with an underscore.
   *
   * @param proposedName
   * @return
   */
  public static String convertToDbSafeBackingColumnName(String proposedName) {
    return "_" + proposedName.replace(" ", "_");
  }

}
