LogglyReportTool
================

Loggly api version 1 reporting tool.


Preparation
===========

Run the following command to configure the tool: 

```
    java -jar reporter-1.0.0-SNAPSHOT.jar -c
```

Usage
=====

```
    usage: java -jar reporter-1.0.0-SNAPSHOT.jar [options]
     -c                         Prompts for credentials and account.
     -e,--email <email>         Email to send report to. (Does nothing if file
                                option not present.)
     -f,--field <field>         Field to require
        --file <file>           Path to XLS file to update.
        --from <from>           Start time for search. Defaults to -24h
     -g,--group-by <group-by>   Field name to group results by.
     -h,--help                  Prints this message.
     -q,--query <query>         Query to run.
     -s,--subject <subject>     Email subject. (Does nothing if file option
                            not present.)
        --to <to>               End time for search. Default to now.
```
