package com.theserverlabs.maven.utplsq;

/*
 * Copyright 2009 The Server Labs.
 *
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Mojo which contains a goal that executes a UTPLSQL unit test, obtaining the
 * results from the database and presenting them in an XML report compatible
 * with the Maven Surefire report standards.
 * 
 * @goal execute
 * 
 * @phase process-test-resources
 */
public class UtplsqlMojo extends AbstractMojo
{

    /**
     * The JDBC driver to use. Defaults to Oracle.
     * 
     * @parameter default-value="oracle.jdbc.driver.OracleDriver"
     */
    private String driver;

    /**
     * The JDBC URL to use.
     * 
     * @parameter
     */
    private String url;

    /**
     * The username to connect to the database.
     * 
     * @parameter
     */
    private String username;

    /**
     * The password to connect to the database.
     * 
     * @parameter
     */
    private String password;

    /**
     * The type of test method to execute. Can be either test or run. Defaults to test.
     * 
     * @parameter default-value="test"
     */
    private String testMethod;

    /**
     * The name of the package to test. You must specify either this or the suiteName parameter.
     * 
     * @parameter
     */
    private String packageName;

    /**
     * The name of the suite to test.
     * 
     * @parameter
     */
    private String testSuiteName;

    /**
     * The setup method to use. set to TRUE to execute setup and teardown for each procedure. FALSE to execute for each package. Default is FALSE.
     * 
     * @parameter default-value="FALSE"
     */
    private String setupMethod;

    /**
     * Location to which we will write the report file. Defaults to the Maven
     * /target directory of the project.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * If there is a connection problem or DB PLSQL installation error the plugin reports 0 tests run.
     * This is an error condition which should be flagged, by default an error exception shall be raised if
     * 0 tests are run. This state can be unset in the maven plugin configuration if desired
     * 
     * @parameter expression=true
     */
    private Boolean failOnNoTests;

    /**
     * Do the main work of the plugin here.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        Connection conn = null;

        try
        {
            getLog().info("using JDBC driver : " + driver);
            Class.forName(driver);

            conn = DriverManager.getConnection(url, username, password);

            TestResult testResult = null;
            String testTitle = null,testName = null;

            if (!StringUtils.isEmpty(packageName))
            {
                testResult = runPackage(conn);
                testName   = packageName;
                testTitle = "Running utplsql package " + testName;
            }
            else
            {
                testResult = runTestSuite(conn);
                testName   = testSuiteName;
                testTitle = "Running utplsql test suite  " + testName;
            }

            if (testResult != null)
            {
                int testsRun = testResult.successCounter + testResult.failureCounter;
                
                getLog().info("\n------------------------------------\n" + "TESTS\n" + "------------------------------------\n" + testTitle + "\n"
                                + "Successes: " + testResult.successCounter + ", Failures: " + testResult.failureCounter + "\n\n" + "Results:\n"
                                + "Tests run: " + testsRun + ", Failures: "
                                + testResult.failureCounter + "\n");
                
                for (Iterator i = testResult.failureDescriptions.iterator(); i.hasNext();)
                {
                    getLog().info("------------------------------------");
                    getLog().info((String) i.next());
                }
                if (testResult.failureCounter > 0)
                {
                    throw new MojoFailureException("utPLSQL tests failed");
                }
                
                if (testsRun == 0 && failOnNoTests.booleanValue())
                {
                    String noTestErrorMsg = "\n\n-------------------------\nutPLSQL Fail On No Tests"+
                                            "\n-------------------------\n" +
                                            "Please check mvn profile for correct db configuration and db\n"+
                                            "for valid installation of pkg 'ut_"+
                                             testName + "' and pkg|func|proc '"+ testName+
                                             "'\n";
                    
                    throw new MojoFailureException(noTestErrorMsg);
               
                }

            }

        } catch (ClassNotFoundException e)
        {
            throw new MojoExecutionException("JDBC Driver class not found", e);
        } catch (SQLException e)
        {
            throw new MojoExecutionException("Problem connecting to DB or executing SQL", e);
        } catch (IOException e)
        {
            throw new MojoExecutionException("Could not build report", e);
        } catch (SplitterException e)
        {
            throw new MojoExecutionException("utPLSQL results not in expected format", e);
        } catch (MojoFailureException e)
        {
            throw e;
        } finally
        {
            if (conn != null)
            {
                try
                {
                    conn.close();
                } catch (SQLException e)
                {
                }
            }
        }
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
    private TestResult runPackage(Connection conn) throws SQLException, IOException, SplitterException
    {
        CallableStatement stmt = null;
        TestResult testResult = null;
        String package_stmt = buildPackageStatment();
        try
        {
            getLog().info("Running UTPLSQL tests for package " + packageName);

            // Call the utPLSQL test() method in PL/SQL, binding in the name
            // of the package that we want to execute and expecting the run_id
            // to be passed as an out parameter. We use the run_id to later
            // look up the results of the test.
            stmt = conn.prepareCall(package_stmt);
            stmt.setString(1, packageName);
            stmt.registerOutParameter(2, Types.NUMERIC);
            stmt.execute();
            int runId = stmt.getInt(2);
            getLog().info("Creating test report for run " + runId);

            // build the report for this run ID
            File surefireDir = createAndCleanOutputDir();
            testResult = buildSurefirePackageReport(conn, surefireDir, runId, packageName);

        } finally
        {
            stmt.close();
        }
        return testResult;
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
    private TestResult runTestSuite(Connection conn) throws SQLException, IOException, SplitterException
    {
        CallableStatement stmt = null;
        TestResult testResult = new TestResult();
        String suite_stmt = buildSuiteStatement();
        try
        {
            getLog().info("Running UTPLSQL test suite " + testSuiteName);

            // execute the test suite, binding two output parameters - the
            // run_id that corresponds to the test suite execution and the
            // number of test packages in the test suite. We'll use these
            // values later to construct a report for each package.
            suite_stmt = buildSuiteStatement();
            stmt = conn.prepareCall(suite_stmt);
            stmt.setString(1, testSuiteName);
            stmt.setString(2, testSuiteName);
            stmt.registerOutParameter(3, Types.NUMERIC);
            stmt.registerOutParameter(4, Types.NUMERIC);
            stmt.execute();
            int runId = stmt.getInt(3);
            int packageCount = stmt.getInt(4);

            // each package was executed with a separate run_id which we
            // must derive. We start with the run_id of the test suite,
            // which will be the latest, and then subtract one from this
            // number on each iteration. The number of iterations is the
            // number of packages in the test suite.
            File surefireDir = createAndCleanOutputDir();
            for (int i = 1; i <= packageCount; i++)
            {
                int pkgRunId = runId - i;
                testResult.append(buildSurefirePackageReport(conn, surefireDir, pkgRunId, testSuiteName + "-" + pkgRunId));
            }

        } finally
        {
            stmt.close();
        }

        return testResult;

    }

    /**
     * Creates the output directory (if necessary) and removes any old reports that are there. The removal may not be necessary.
     * 
     * @return the File object that corresponds to the output directory
     * @throws IOException
     *             if there was a problem creating the directory or removing old contents.
     */
    private File createAndCleanOutputDir() throws IOException
    {
        File surefireDir = new File(outputDirectory + "/surefire-reports");
        FileUtils.forceMkdir(surefireDir);
        FileUtils.cleanDirectory(surefireDir);
        return surefireDir;
    }

    /**
     * Given the run_id of a test package run, create a report in surefire XML format.
     * 
     * @param conn
     *            the connection to the database
     * @param surefireDir
     *            the directory to which we will write the surefire report
     * @param runId
     *            the test run ID
     * @param suiteOrPackageName
     *            the suite or package name
     * @throws SQLException
     *             if there was a problem getting the report data from the database
     * @throws IOException
     *             if there was a problem outputting the report to the filesystem
     * @throws SplitterException
     *             if there was a problem generating the report
     * 
     * @return retrieved results
     */
    private TestResult buildSurefirePackageReport(Connection conn, File surefireDir, int runId, String suiteOrPackageName) throws SQLException,
                    IOException, SplitterException
    {

        // get the status and description for each test that was run in the
        // test run.
        PreparedStatement stmt = conn.prepareStatement("select status, description from utr_outcome where run_id = ?");
        stmt.setInt(1, runId);
        ResultSet rs = stmt.executeQuery();

        int failureCounter = 0;
        int successCounter = 0;
        StringBuffer sb = new StringBuffer();

        TestResult testResult = new TestResult();

        while (rs.next())
        {
            String status = rs.getString("status");
            String desc = rs.getString("description");
            getLog().debug("processing description: " + desc);

            // work out if the test was a success or failure
            // (only statuses possible in utPLSQL)
            if ("SUCCESS".equals(status))
            {
                successCounter++;
            }
            else
            {
                failureCounter++;
            }

            // use the ResultSpitter to get the varios data elements
            // contained in the description
            // (the utPLSQL schema doesn't seem to be well normalised ;-) )
            DescContainer dc = ResultSplitter.split(desc);

            // generate the XML for each test executed and add it to a
            // string buffer that we'll later write to a file
            sb.append("   <testcase classname=\"" + dc.getProcedureName() + "\" ");
            sb.append("time=\"0\" ");
            sb.append("name=\"" + dc.getTestName() + "\" ");
            sb.append(">");

            if ("FAILURE".equals(status))
            {
                sb.append("<failure message=\"" + StringEscapeUtils.escapeXml(dc.getTestDescription()) + "\" type=\"utplsql.utAssert\"/>");

                testResult.failureDescriptions.add(dc.getTestDescription());
            }

            sb.append("</testcase>");
            sb.append("\n");

        }

        testResult.successCounter = successCounter;
        testResult.failureCounter = failureCounter;

        // write the entire report to a file. We have to do it this way round
        // in order to know all the failures/sucesses to complete the
        // XML header element. Note that utPLSL does not provide any
        // timing info.

        FileWriter fw = null;
        try
        {

            File f = new File(surefireDir, "utplsql-" + suiteOrPackageName + "-report.xml");
            fw = new FileWriter(f);
            fw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            fw.append("<testsuite errors=\"0\" ");
            fw.append("skipped=\"0\" tests=\"");
            fw.append(Integer.toString((failureCounter + successCounter)));
            fw.append("\" time=\"0\" failures=\"");
            fw.append(Integer.toString(failureCounter));
            fw.append("\" name=\"" + suiteOrPackageName + "\">");
            fw.append(sb.toString());
            fw.append("</testsuite>");
        } finally
        {
            IOUtils.closeQuietly(fw);
        }

        return testResult;
    }

    private String buildPackageStatment()
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

    private String buildSuiteStatement()
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

    /**
     * 
     * Simple class for storing results
     * 
     */
    private static class TestResult
    {

        int successCounter;

        int failureCounter;

        ArrayList failureDescriptions = new ArrayList();

        public void append(TestResult other)
        {
            successCounter += other.successCounter;
            failureCounter += other.failureCounter;
            failureDescriptions.addAll(other.failureDescriptions);
        }

    }

}
