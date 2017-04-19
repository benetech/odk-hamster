/*
 * Copyright (C) 2009 Google Inc. 
 * Copyright (C) 2010 University of Washington.
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

package org.opendatakit.constants;

import java.util.Date;

/**
 * Constants used in ODK that are shared everywhere
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public final class BasicConsts {


  
  // general constants



  public static final String EMPTY_STRING = "";
  public static final String FORWARDSLASH = "/";

  public static final String COLON = ":";

  public static final String UTF8_ENCODE = "UTF-8";
  // constant as only needs to be created once
  public static final Date EPOCH = new Date(0);
}
