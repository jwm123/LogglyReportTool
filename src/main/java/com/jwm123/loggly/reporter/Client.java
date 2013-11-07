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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * com.jwm123.loggly.reporter.Client
 *
 * @author jmcentire
 */
public class Client {
  private Configuration config;
  private String query;
  private String from;
  private String to;

  public Client(Configuration config) throws IllegalArgumentException {
    this.config = config;
    if(config == null || StringUtils.isBlank(config.getUsername()) || StringUtils.isBlank(config.getPassword()) || StringUtils.isBlank(config.getAccount())) {
      throw new IllegalArgumentException("Please rerun with -c flag.");
    }
  }

  public List<Map<String, Object>> getReport() throws Exception {
    final Integer rows = 500;
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
        new AuthScope(config.getAccount()+".loggly.com", 80),
        new UsernamePasswordCredentials(config.getUsername(), config.getPassword()));
    CloseableHttpClient client = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider).build();
    String url = "http://"+config.getAccount()+".loggly.com/api/search?rows=" + rows +
        "&q="+ URLEncoder.encode(query, "UTF8");
    if(StringUtils.isNotBlank(from)) {
      url += "&from="+URLEncoder.encode(from, "UTF8");
    }
    if (StringUtils.isNotBlank(to)) {
      url += "&until=" + URLEncoder.encode(to, "UTF8");
    }
    HttpGet get = new HttpGet(url);
    CloseableHttpResponse resp = null;
    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
    Integer offset=0-rows;
    Integer numFound=0;
    try {
      do {
        offset += rows;
        try {
          get = new HttpGet(url + "&start="+offset);

          resp = client.execute(get);
          if(resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            String response = EntityUtils.toString(resp.getEntity());
            Map<String, Object> responseMap = new Gson().fromJson(response, new TypeToken<Map<String, Object>>() {}.getType());
            if(responseMap.containsKey("numFound")) {
              numFound = ((Number) responseMap.get("numFound")).intValue();
            }
            if(responseMap.containsKey("data")) {
              Collection<?> dataCol = (Collection<?>)responseMap.get("data");
              if(!dataCol.isEmpty()) {
                for(Object dataItem : dataCol) {
                  if(dataItem instanceof Map) {
                    results.add((Map<String, Object>)dataItem);
                  }
                }
              }
            }
          } else {
            System.out.println("status: " + resp.getStatusLine().getStatusCode() + " Response: ["+EntityUtils.toString(resp.getEntity())+"]");
            break;
          }
        } finally {
          get.releaseConnection();
          IOUtils.closeQuietly(resp);
        }
      } while(numFound >= offset+rows);
    } finally {
      IOUtils.closeQuietly(client);
    }

    return results;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public void setTo(String to) {
    this.to = to;
  }
}
