# Changes #

## Version 1.31 ##

The 'failOnNoTests tag' now functions on each package in the 'packages tag' rather than all.

The failure reports to the console are now switched off by default but can be activated using the new 'writeFailuresToConsole tag'

## Version 1.3 ##

This is an enhancement release focused upon making it easier to identify exactly which test has failed. To improve this aspect the contents of Surefire attribute fields has changed to generate a format similar to TestNG.

The classname attribute now holds the `<packagename>.<methodname>` freeing the `<name>` attribute to hold just the test description. Additional results and debug output is combined and placed into message attribute of the failure message, the type field is now used for the Assert test type. This significantly improves the readability of the surefire xml and the site report.

This version adds the 'packages tag' which permits multiple packages to be tested without the need for a suite procedure.

## Version 1.2 ##

Released Snapshot-1.2 with a patch from Paul Savage to the UtPlsqlMojo and ResultSplitter classes. Here are the details from Paul:

  * The maven command ‘surefire-report:report-only’ would crash if any failures were present in the xml output. This was caused by a type attribute missing from the failure tag – now fixed.

  * The console reporting has been enhanced to make it more like JUnit report output.

  * If the PLSQL installation has un-compiled or missing packages the plug-in would report 0 tests run and ‘success’. This has been changed to raise an exception and warn users of possible failure conditions if 0 tests are detected, in any case why would you use the plugin for 0 tests! A configuration tag/property ‘failOnNoTests’ has been added to permit configuration of this behavior, by default its set to true to bring attention to the problem.

All other downloads have been marked as 'deprecated'.

## Version 1.1 ##

Released Snapshot-1.1 with a patch from Nikolaus Chorherr to the UtPlsqlMojo class. This change fixes a missing type-attribute of the failure-tag of the surefire-test-report-xml which causes surefire-report-plugin to crash with a NPE.