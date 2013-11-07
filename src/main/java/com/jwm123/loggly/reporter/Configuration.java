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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Console;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * com.jwm123.loggly.reporter.Configuration
 *
 * @author jmcentire
 */
public class Configuration {
  private TripleDesCipher cipher;
  private static final String CONFIG_LOCATION = ".loggly/config";

  private String username;
  private String password;
  private String account;
  private String mailServer;
  private String mailFrom;
  private String mailUsername;
  private String mailPassword;
  private Integer mailPort;
  private AppDirectory appDir;

  public Configuration() throws Exception {
    appDir = new AppDirectory();
    cipher = new TripleDesCipher(".loggly/des.key", appDir);
    readIn();
  }

  public void update() throws Exception {
    Console console = System.console();
    do {
      username = console.readLine("Username: ");
    } while (StringUtils.isBlank(username));
    do {
      password = new String(console.readPassword("Password: "));
    } while (StringUtils.isBlank(password));
    do {
      account = console.readLine("Loggly Account: ");
    } while (StringUtils.isBlank(account));
    mailServer = console.readLine("Mail Server: ");
    mailFrom = console.readLine("Mail From: ");
    String mailPortStr = console.readLine("Mail Port [25]: ");
    if(StringUtils.isBlank(mailPortStr) || !mailPortStr.matches("\\d*")) {
      mailPort = 25;
    } else {
      mailPort = new Integer(mailPortStr);
    }
    if(StringUtils.isNotBlank(mailServer)) {
      mailUsername = console.readLine("Mail Username: ");
      if(StringUtils.isNotBlank(mailUsername)) {
        mailPassword = new String(console.readPassword("Mail Password: "));
      }
    }
    writeOut();
  }

  private void readIn() throws Exception {
    File configFile = getAppDir().getFileDir(CONFIG_LOCATION);
    if(configFile.exists()) {
      String encoded = FileUtils.readFileToString(configFile);
      String jsonData = cipher.decode(encoded);
      Map<String, String> config = new Gson().fromJson(jsonData, new TypeToken<Map<String, String>>(){}.getType());
      username = config.get("username");
      password = config.get("password");
      account = config.get("account");
      mailServer = config.get("mail-server");
      mailUsername = config.get("mail-user");
      mailPassword = config.get("mail-password");
      mailFrom = config.get("mail-from");
      if(config.containsKey("mail-port")) {
        mailPort = new Integer(config.get("mail-port"));
      }
    }
  }

  private void writeOut() throws Exception {
    Map<String, String> config = new HashMap<String, String>();
    config.put("username", username);
    config.put("password", password);
    config.put("account", account);
    if(StringUtils.isNotBlank(mailServer)) {
      config.put("mail-server", mailServer);
      config.put("mail-port", mailPort.toString());
      config.put("mail-from", mailFrom);
      if(StringUtils.isNotBlank(mailUsername)) {
        config.put("mail-user", mailUsername);
        config.put("mail-password", mailPassword);
      }
    }
    String jsonData = new Gson().toJson(config);
    String encoded = cipher.encode(jsonData);
    File configFile = getAppDir().getFileDir(CONFIG_LOCATION);
    FileUtils.deleteQuietly(configFile);
    FileUtils.writeStringToFile(configFile, encoded);
  }

   public String toString() {
     return ToStringBuilder.reflectionToString(this);
   }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getAccount() {
    return account;
  }

  public String getMailServer() {
    return mailServer;
  }

  public String getMailUsername() {
    return mailUsername;
  }

  public String getMailPassword() {
    return mailPassword;
  }

  public Integer getMailPort() {
    return mailPort;
  }

  public String getMailFrom() {
    return mailFrom;
  }

  public AppDirectory getAppDir() {
    return appDir;
  }
}
