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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * com.jwm123.loggly.reporter.AppLauncher
 *
 * @author jmcentire
 */
public class AppLauncher {
  private static Options opts;
  private static Configuration config;
  private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MMMMMMMMMMMM");

  public static void main(String args[]) throws Exception {
    try {
      CommandLine cl = parseCLI(args);
      try {
        config = new Configuration();
      } catch(Exception e) {
        e.printStackTrace();
        System.err.println("ERROR: Failed to read in persisted configuration.");
      }
      if(cl.hasOption("h")) {

        HelpFormatter help = new HelpFormatter();
        String jarName = AppLauncher.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        if(jarName.contains("/")) {
          jarName = jarName.substring(jarName.lastIndexOf("/")+1);
        }
        help.printHelp("java -jar "+jarName+ " [options]", opts);
      }
      if(cl.hasOption("c")) {
        config.update();
      }
      if(cl.hasOption("q")) {
        Client client = new Client(config);
        client.setQuery(cl.getOptionValue("q"));
        if(cl.hasOption("from")) {
          client.setFrom(cl.getOptionValue("from"));
        }
        if(cl.hasOption("to")) {
          client.setTo(cl.getOptionValue("to"));
        }
        List<Map<String, Object>> report = client.getReport();

        if(report != null) {
          List<Map<String, String>> reportContent = new ArrayList<Map<String, String>>();
          ReportGenerator generator = null;
          if(cl.hasOption("file")) {
            generator = new ReportGenerator(new File(cl.getOptionValue("file")));
          }
          byte reportFile[] = null;

          if(cl.hasOption("g")) {
            System.out.println("Search results: "+report.size());
            Set<Object> values = new TreeSet<Object>();
            Map<Object, Integer> counts = new HashMap<Object, Integer>();
            for(String groupBy : cl.getOptionValues("g")) {
              for(Map<String, Object> result : report) {
                if(mapContains(result, groupBy)) {
                  Object value = mapGet(result, groupBy);
                  values.add(value);
                  if(counts.containsKey(value)) {
                    counts.put(value, counts.get(value) + 1);
                  } else {
                    counts.put(value, 1);
                  }
                }
              }
              System.out.println("For key: " + groupBy);
              for(Object value : values) {
                System.out.println("  " + value + ": " + counts.get(value));
              }
            }
            if (cl.hasOption("file")) {
              Map<String, String> reportAddition = new LinkedHashMap<String, String>();
              reportAddition.put("Month", MONTH_FORMAT.format(new Date()));
              reportContent.add(reportAddition);
              for(Object value : values) {
                reportAddition = new LinkedHashMap<String, String>();
                reportAddition.put(value.toString(), "" + counts.get(value));
                reportContent.add(reportAddition);
              }
              reportAddition = new LinkedHashMap<String, String>();
              reportAddition.put("Total", "" + report.size());
              reportContent.add(reportAddition);
            }
          } else {
            System.out.println("The Search ["+cl.getOptionValue("q")+"] yielded "+report.size()+" results.");
            if (cl.hasOption("file")) {
              Map<String, String> reportAddition = new LinkedHashMap<String, String>();
              reportAddition.put("Month", MONTH_FORMAT.format(new Date()));
              reportContent.add(reportAddition);
              reportAddition = new LinkedHashMap<String, String>();
              reportAddition.put("Count", ""+report.size());
              reportContent.add(reportAddition);
            }
          }
          if (cl.hasOption("file")) {
            reportFile = generator.build(reportContent);
            File reportFileObj = new File(cl.getOptionValue("file"));
            FileUtils.writeByteArrayToFile(reportFileObj, reportFile);
            if(cl.hasOption("e")) {
              ReportMailer mailer = new ReportMailer(config, cl.getOptionValues("e"), cl.getOptionValue("s"), reportFileObj.getName(), reportFile);
              mailer.send();
            }
          }
        }
      }

    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
  }

  private static boolean mapContains(Map<String, Object> result, String groupBy) {
    return mapGet(result, groupBy) != null;
  }

  private static Object mapGet(Map<String, Object> result, String groupBy) {
    String path[] = groupBy.split("\\.");
    Map<String, Object> subMap = result;
    for(int i = 0; i<path.length; i++) {
      String pathEl = path[i];
      if(subMap != null) {
        Object value = subMap.get(pathEl);
        if(value != null) {
          if(value instanceof Map) {
            subMap = (Map<String, Object>) value;
          } else if(i + 1 == path.length) {
            return value;
          } else {
            return null;
          }
        }
      } else {
        return null;
      }
    }
    return null;
  }

  private static CommandLine parseCLI(String args[]) throws Exception {
    CommandLineParser parser = new GnuParser();
    opts = new Options();
    Option config = OptionBuilder.withArgName("configure").hasArg(false).withDescription("Prompts for credentials and account.").create("c");
    Option query = OptionBuilder.withArgName("query").withLongOpt("query").hasArg(true).withDescription("Query to run.").create("q");
    Option field = OptionBuilder.withArgName("field").withLongOpt("field").hasArg(true).withDescription("Field to require").create("f");
    Option group = OptionBuilder.withArgName("group-by").withLongOpt("group-by").hasArg(true).withDescription("Field name to group results by.").create("g");
    Option from = OptionBuilder.withArgName("from").withLongOpt("from").hasArg(true).withDescription("Start time for search. Defaults to -24h").create();
    Option to = OptionBuilder.withArgName("to").withLongOpt("to").hasArg(true).withDescription("End time for search. Default to now.").create();
    Option reportFile = OptionBuilder.withArgName("file").withLongOpt("file").hasArg(true).withDescription("Path to XLS file to update.").create();
    Option email = OptionBuilder.withArgName("email").withLongOpt("email").hasArgs().withDescription("Email to send report to. (Does nothing if file option not present.)").create("e");
    Option subject = OptionBuilder.withArgName("subject").withLongOpt("subject").hasArg(true).withDescription("Email subject. (Does nothing if file option not present.)").create("s");
    Option help = OptionBuilder.withArgName("help").withLongOpt("help").hasArg(false).withDescription("Prints this message.").create("h");
    opts.addOption(config).addOption(query).addOption(field).addOption(group).addOption(from).addOption(to).addOption(reportFile).addOption(email).addOption(subject).addOption(help);

    return parser.parse(opts, args, true);
  }
}
