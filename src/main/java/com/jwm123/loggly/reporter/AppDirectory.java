/**
 *    Copyright 2013 jwm123
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.jwm123.loggly.reporter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * com.jwm123.loggly.reporter.AppDirectory
 *
 * @author jmcentire
 */
public class AppDirectory {
  private File appDir;

  public AppDirectory() {
    String userDirectory = System.getProperty("user.home");
    appDir = new File(userDirectory);
  }

  public File getFileDir(String relativePath) throws IOException {
    File relFile = new File(appDir, relativePath);
    if(!relFile.getParentFile().exists()) {
      FileUtils.forceMkdir(relFile.getParentFile());
    }
    return relFile;
  }
}
