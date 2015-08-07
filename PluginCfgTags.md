# Introduction #

The plugin supports a number of configuration tags that can be added to the 'pom.xml' to define the database connection and its  running and reporting behavior. All available tags are defined below:-

# Driver Tag #

This is an optional tag that defines the JDBC driver to use, if it is not specified a default Oracle Driver db driver shall be used.
```
   <driver>oracle.jdbc.driver.OracleDriver</driver>
```
Type: Optional, Permitted Values: java classname, Default Value: oracle.jdbc.driver.OracleDriver

# URL Tag #

This tag defines the JDBC URL connection to the database, the example below assumes an Oracle JDBC connection.
```
   <url>jdbc:oracle:thin:@myhost:1521:mydbinstance</url>
```
Type: Mandatory, Permitted Values: url text, Default Value: NONE

# Username Tag #

The username used to connect to the database.
```
   <username>CaptSensible</username>
```
Type: Mandatory, Permitted Values: username text, Default Value: NONE

# Password Tag #

The password to connect to the database.
```
   <password>FanClub</password>
```
Type: Mandatory, Permitted Values: password text, Default Value: NONE

# Package Name Tag #

This is the name of the package to test (not the test package), this value has been superseded in v1.3 by the package tag.
```
   <packageName>MyPackageToBeTested</packageName>
```
NB/ testSuiteName, packageName and packages are mutually exclusive.

Type: Optional, Permitted Values: sql package to be tested, Default Value: NONE

# Test Suite Name Tag #

This tag can be used to run a utPLSQL test suite,
```
   <testSuiteName>mySuiteName</testSuiteName>
```
NB/ testSuiteName, packageName and packages are mutually exclusive.

Type: Optional, Permitted Values: valid utPLSQL suite procedure, Default Value: NONE

# Packages Tag #

This tag was introduced in v1.3 and offers an alternative way to run a suite of packages. To test multiple packages within the same maven project add a set of param tags for each package.
```
   <packages>
       <param>mypkg1</param>
       <param>mypkg2</param>
   </packages>
```
NB/ testSuiteName, packageName and packages are mutually exclusive.

Type: Optional, Permitted Values: valid sql package name, Default Value: NONE

# Setup Method Tag #

This tag can be used to instruct utPLSQL to run ut\_setup and ut\_teardown after every method rather than once per package - the default value is FALSE.
```
   <setupMethod>TRUE</setupMethod>
```
Type: Optional, Permitted Values: TRUE|FALSE, Default Value: FALSE

# Test Method Tag #

The value of this tag defines the utplsql method to run and be set to 'test or 'run'. For the default option 'test' the 'recompile\_in => FALSE' option is added to the command options.
```
   <testMethod>test</testMethod>
```
Type: Optional, Permitted Values: test|run, Default Value: test

# Output Directory Tag #

This value maps onto the maven ${project.build.directory} and defines the directory where a surefire directory and report shall be added. Unless you have a none standard maven configuration stick to the default value
```
   <outputDirectory>${project.build.directory}</outputDirectory>
```
Type: Optional, Permitted Values: full directory path, Default Value: ${project.build.directory}

# Fail On No Tests Tag #

In the case of a database package compilation error utPLSQL will fail and report 0 tests run. The default behavior is to raise an error to highlight this potential problem, if this is not required this value should be set to false (functionality introduced in v1.2).
```
   <failOnNoTests>TRUE</failOnNoTests;>
```
Type: Optional, Permitted Values: TRUE|FALSE, Default Value: TRUE

# Write Failures To Console Tag #

Since v1.1 failure messages have been output directly to the console, from v1.31 this is switched off by default but can be reactivated by using this tag.

```
   <writeFailuresToConsole>FALSE</writeFailuresToConsole>
```

Type: Optional, Permitted Values: TRUE|FALSE, Default Value: FALSE