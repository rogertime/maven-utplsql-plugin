package com.theserverlabs.maven.utplsq;

import java.io.File;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import org.apache.maven.plugin.logging.Log;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Creates and runs the sql to execute the test package or suite.
 */
public class UtplsqlRunner
{
    Log log;
    File outputDir;
    
    public UtplsqlRunner(File  outputDir,Log log)
    {
        this.log  = log;
        this.outputDir = outputDir;
    }
    /**
     * Run the utPLSQL tests in a single package. This method calls the relevant utPLSQL schema stored procedure and obtains the results, exporting
     * them in a Maven Surefire report.
     * 
     * @param conn
     *            the database connection to use
     * @throws SQLException
     *             if there is a problem communicating with the database
     * @throws IOException
     *             if there is a problem writing the report file
     * @throws SplitterException
     *             if there is a problem generating the report
     * 
     * @return retrieved results
     */
    protected TestResults runPackage(Connection conn,String packageName,String testMethod,String setupMethod) throws SQLException, IOException, SplitterException
    {
        CallableStatement stmt = null;
        TestResults testResults = new TestResults();
        String package_stmt = buildPackageStatment(testMethod,setupMethod);
        
        try
        {
            log.info("Running UTPLSQL tests for package " + packageName);

            Calendar startTime = Calendar.getInstance();
             
            // Call the utPLSQL test() method in PL/SQL, binding in the name
            // of the package that we want to execute and expecting the run_id
            // to be passed as an out parameter. We use the run_id to later
            // look up the results of the test.
            stmt = conn.prepareCall(package_stmt);
            stmt.setString(1, packageName);
            stmt.registerOutParameter(2, Types.NUMERIC);
            stmt.execute();
            
            Calendar endTime = Calendar.getInstance();
            
            long durationtimeInMsecs = endTime.getTimeInMillis() - startTime.getTimeInMillis();
            
            int runId = stmt.getInt(2);
            
            log.debug("Package "+packageName+" runId " + runId);

            // build the report for this run ID

            testResults = getSureFireReport().build(conn,runId, packageName,durationtimeInMsecs);

        } finally
        {
            stmt.close();
        }
        return testResults;
    }
    /**
     * Run the utPLSQL tests in a test suite. This method calls the relevant utPLSQL schema stored procedure and obtains the results, exporting them
     * in a Maven Surefire report.
     * 
     * Note that this method calculates the expected run_id for each package within the test suite which may not always be accurate.
     * 
     * @param conn
     *            the database connection to use
     * @throws SQLException
     *             if there is a problem communicating with the database
     * @throws IOException
     *             if there is a problem writing the report file
     * @throws SplitterException
     *             if there is a problem generating the report
     * 
     * @return retrieved results
     */
    protected TestResults runTestSuite(Connection conn,String testSuiteName,String testMethod,String setupMethod) throws SQLException, IOException, SplitterException
    {
        CallableStatement stmt = null;
        TestResults mergedResults = new TestResults();
        String suite_stmt = buildSuiteStatement(testMethod,setupMethod);
        try
        {
            log.info("Running UTPLSQL test suite " + testSuiteName);

            Calendar startTime = Calendar.getInstance();
            
            // execute the test suite, binding two output parameters - the
            // run_id that corresponds to the test suite execution and the
            // number of test packages in the test suite. We'll use these
            // values later to construct a report for each package.
            suite_stmt = buildSuiteStatement(testMethod,setupMethod);
            stmt = conn.prepareCall(suite_stmt);
            stmt.setString(1, testSuiteName);
            stmt.setString(2, testSuiteName);
            stmt.registerOutParameter(3, Types.NUMERIC);
            stmt.registerOutParameter(4, Types.NUMERIC);
            stmt.execute();
            
            Calendar endTime = Calendar.getInstance();
            
            long durationtimeInMsecs = endTime.getTimeInMillis() - startTime.getTimeInMillis();
           
            int runId = stmt.getInt(3);
            int packageCount = stmt.getInt(4);

            // each package was executed with a separate run_id which we
            // must derive. We start with the run_id of the test suite,
            // which will be the latest, and then subtract one from this
            // number on each iteration. The number of iterations is the
            // number of packages in the test suite.

            SureFireReport report = getSureFireReport();
            
            for (int i = 1; i <= packageCount; i++)
            {
                int pkgRunId = runId - i;
                mergedResults.append(report.build(conn,pkgRunId, testSuiteName + "-" + pkgRunId,durationtimeInMsecs));
            }

        } finally
        {
            stmt.close();
        }

        return mergedResults;

    }
    
    protected SureFireReport getSureFireReport() throws IOException
    {
        return new SureFireReport(outputDir,log);
    }
    /**
     * Build the sql to execute the package      
     * 
     * @param testMethod
     * @param setupMethod
     * @return
     */
    private String buildPackageStatment(String testMethod,String setupMethod)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("begin ");
        sb.append("utplsql.");
        sb.append(testMethod);
        sb.append("(?, ");
        if ("test".equals(testMethod))
        {
            sb.append("recompile_in => FALSE, ");
        }
        // this hack because JDBC is forbidden from pushing a Boolean to PL/SQL
        sb.append("per_method_setup_in => ");
        sb.append(setupMethod);
        sb.append("); ");
        // end hack
        sb.append(" ? := utplsql2.runnum; ");
        sb.append("end; ");
        return sb.toString();
    }
    /**
     * SQL to run a utplsql suite
     * 
     * @param testMethod
     * @param setupMethod
     * @return
     */
    private String buildSuiteStatement(String testMethod,String setupMethod)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("declare ");
        sb.append("v_suite_pkg_count PLS_INTEGER; ");
        sb.append("begin ");
        sb.append("utplsql.");
        sb.append(testMethod);
        sb.append("suite");
        sb.append("(?, ");
        if ("test".equals(testMethod))
        {
            sb.append("recompile_in => FALSE, ");
        }
        // this hack because JDBC is forbidden from pushing a Boolean to PL/SQL
        sb.append("per_method_setup_in => ");
        sb.append(setupMethod);
        sb.append("); ");
        // end hack
        sb.append("  select count(*) suite_package_count ");
        sb.append("  into v_suite_pkg_count ");
        sb.append("  from ut_suite s, ut_package p  ");
        sb.append("  where s.id = p.suite_id  ");
        sb.append("  and s.name = Upper(?); ");
        sb.append("  ? := utplsql2.runnum;  ");
        sb.append("  ? := v_suite_pkg_count;  ");
        sb.append("end; ");
        return sb.toString();
    }
 
}
