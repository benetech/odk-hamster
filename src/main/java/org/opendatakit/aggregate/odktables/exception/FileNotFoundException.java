/*
 * Copyright (C) 2014 University of Washington
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

package org.opendatakit.aggregate.odktables.exception;

public class FileNotFoundException extends ODKTablesException {
  private static final long serialVersionUID = 1L;

  public FileNotFoundException() {
    super();
  }

  public FileNotFoundException(String message) {
    super(message);
  }

  public FileNotFoundException(Throwable cause) {
    super(cause);
  }

  public FileNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}
