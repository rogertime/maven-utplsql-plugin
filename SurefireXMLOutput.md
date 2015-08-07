# Introduction #

One problem with the existing Surefire output is for large number of tests it’s difficult to quickly identify which test has failed. To improve this aspect the contents of Surefire attribute fields has changed to generate a format similar to TestNG.<br />

The classname attribute now holds the `<packagename>.<methodname>` freeing the `<name>` attribute to hold just the test description. Additional results and debug output is combined and placed into message attribute of the failure message, the type field is now used for the Assert test type. This significantly improves the readability of the surefire xml and the site report


# Sample Surefire XML Output #
```
<?xml version="1.0" encoding="UTF-8" ?>
<testsuite name="TestPkg" tests="14" failures="5" skipped="0" errors="0" time="0">
   <testcase classname=".ut_setup" name="Migration Started" time="0"></testcase>
   <testcase classname=".ut_setup" name="Check Migration Outcome" time="0"></testcase>
   <testcase classname="TestPkg.UT_ADDCANCELLED" name="Check Pending Status Position for  Migration  add cancelled  14" time="0"></testcase>
   <testcase classname="TestPkg.UT_2MSVACTIVECANCELLED" name="Unable to run ut_TestPkg.UT_2MSVACTIVECANCELLED" time="0">
       <failure type="Assert Type Unknown" message="ORA-06533 Subscript beyond count"/>
   </testcase>
   <testcase classname="TestPkg.UT_MODIFYACTIVE" name="Check Start End Version for attribute Billing_Account_Number for service Migration  modify active  11 startVersion1" time="0"></testcase>
   <testcase classname="TestPkg.UT_MODIFYACTIVE" name="Check Start End Version for attribute Billing_Account_Number for service Migration  modify active  11 startVersion6" time="0">
       <failure type="EQQUERYVALUE" message="Query select count(*) from usm_nva_mobile, usmservice_o, usmservicecharacteristic_m where usmservice_o.name = &apos;Migration  modify active  11&apos; and usm_nva_mobile.usmserviceid=usmservice_o.usmserviceid and usmservicecharacteristic_m.usmcharacteristicid = usm_nva_mobile.usmcharacteristicid and usmservicecharacteristic_m.name = &apos;Billing_Account_Number&apos; and usm_nva_mobile.startversionnumber =6 and usm_nva_mobile.endversionnumber IS NULL and usm_nva_mobile.attributestatusflag = 0 returned value 0 that does  not match 1"/>
   </testcase>
   <testcase classname="TestPkg.UT_MODIFYACTIVE" name="Check Start End Version for attribute Billing_Account_Number for service Migration  modify active  11 startVersion7" time="0"></testcase>
   <testcase classname="TestPkg.UT_MODIFYACTIVE" name="Check Start End Version for attribute Barring_Level for service Migration  modify active  11 startVersion1" time="0"></testcase>
    <testcase classname="TestPkg.UT_MODIFYPENDING" name="Check Start End Version for attribute Billing_Account_Number for service Migration  modify pending  13 startVersion6" time="0">
       <failure type="EQQUERYVALUE" message="Query select count(*) from usm_nva_mobile, usmservice_o, usmservicecharacteristic_m where usmservice_o.name = &apos;Migration  modify pending  13&apos; and usm_nva_mobile.usmserviceid=usmservice_o.usmserviceid and usmservicecharacteristic_m.usmcharacteristicid = usm_nva_mobile.usmcharacteristicid and usmservicecharacteristic_m.name = &apos;Billing_Account_Number&apos; and usm_nva_mobile.startversionnumber =6 and usm_nva_mobile.endversionnumber IS NULL and usm_nva_mobile.attributestatusflag = 0 returned value 0 that does  not match 1"/>
   </testcase>
   <testcase classname="TestPkg.UT_MODIFYPENDING" name="Check Start End Version for attribute Billing_Account_Number for service Migration  modify pending  13 startVersion7" time="0"></testcase>
   <testcase classname="TestPkg.UT_DELETEACTIVE" name="Check Pending Status Position for  Migration  delete active  7" time="0">
       <failure type="EQ" message="Expected 7 and got 0"/>
   </testcase>
   <testcase classname="TestPkg.UT_MSVACTIVE" name="Check Start End Version for attribute Billing_Account_Number for service Migration  single msv active  17 startVersion1" time="0">
       <failure type="EQQUERYVALUE" message="Query select count(*) from usm_nva_mobile, usmservice_o, usmservicecharacteristic_m where usmservice_o.name = &apos;Migration  single msv active  17&apos; and usm_nva_mobile.usmserviceid=usmservice_o.usmserviceid and usmservicecharacteristic_m.usmcharacteristicid = usm_nva_mobile.usmcharacteristicid and usmservicecharacteristic_m.name = &apos;Billing_Account_Number&apos; and usm_nva_mobile.startversionnumber =1 and usm_nva_mobile.endversionnumber =1 and usm_nva_mobile.attributestatusflag = 0 returned value 0 that does  not match 1"/>
   </testcase>
   <testcase classname="TestPkg.UT_MSVACTIVE" name="Check Start End Version for attribute Barring_Level for service Migration  single msv active  17 startVersion1" time="0"></testcase>
   <testcase classname="TestPkg.UT_WITHNOCHANGE" name="Teardown Complete" time="0"></testcase>
</testsuite>
```