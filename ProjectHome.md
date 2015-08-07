# Introduction #

Many businesses rely on PL/SQL for their day-to-day operation. The fact that it is relatively old and does not mean that you cannot apply Extreme Programming(XP) practices to PL/SQL development. A few years ago, Steven Feuerstein developed utPLSQL, a unit testing framework for PL/SQL, which covers the testing part of XP. The goal of this project is to develop and maintain a Maven plugin that allows you to execute utPLSQL unit tests in an Oracle database and generate a surefire-compliant report. This plugin can then form the basis for a continuous integration solution for PL/SQL.

The original motivation and idea for this plugin is described in [this blog post](http://www.theserverlabs.com/blog/2009/05/18/continuous-integration-with-oracle-plsql-utplsql-and-hudson/), although you should consider the information contained within this site as more relevant than that in the post.

# How It Works #

The utplsql plugin converts the configuration tags it finds in the maven pom.xml into an sql script. When this script is executed it calls the utplsql test packages to execute the desired tests. Upon completion the UTR\_OUTCOME table is updated with the outcome. The table contains a description column which is a merged summary of the tests executed and any related debug output. The format of the column is approximately :-

> <Package Name>.<Method Name>:<Assert Type> <"Test Description" Result>: <Query failure information>.

Post execution the contents of UTR\_OUTCOME is retrieved by the plugin and converted into a Surefire xml file.

# Typical Development Cycle #

The first step is to install utplsql v2.2 into the database, development can then proceed in a pure db environment using all the tools utplsql provides. When tests are mature the next step is to integrate the tests with maven and use the plug-in to execute the tests. The last step may consist of integration with a continuous integration tool.

# Results Reporting #

After the plug-in has gathered the results the outcome of the tests are reported to the command line in a typical JUnit form :-
```
 ------------------------------------
 TESTS
 ------------------------------------
 utplsql:Testing package pkgmigrationstate
 Successes: 16, Failures: 0
 
 Results:
 Tests run: 16, Failures: 0

```

A Surefire xml report is also generated and saved to the standard surefire subdirectory, for details concerning its contents please refer to the SurefireXMLOutput wiki.

Report Location

```
 ./target/surefire-reports/utplsql-<package name>-report.xml
```

A site report can be run at a location further up the maven tree to generate a report similar to the sample site Surefire Report found at

http://mojo.codehaus.org/cobertura-maven-plugin/surefire-report.html<br />
http://maven.apache.org/plugins/maven-site-plugin-3.0-beta-3/surefire-report.html


# Enhancing the plugin #

If you want to download the source code for the plugin, just execute the following in a command window (you must have the subversion client installed):

```
svn checkout http://maven-utplsql-plugin.googlecode.com/svn/trunk/ maven-utplsql-plugin-read-only
```

To compile, run the tests for and install the plugin in your local maven repository, just run:

```
mvn install
```

If you want to contribute any fixes or enhancements, just get in touch with us. [mailto:engineering@theserverlabs.com](mailto:engineering@theserverlabs.com)