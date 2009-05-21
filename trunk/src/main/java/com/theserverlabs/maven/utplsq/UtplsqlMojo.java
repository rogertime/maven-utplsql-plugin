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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Mojo which contains a goal that executes a UTPLSQL unit test, obtaining the
 * results from the database and presenting them in an XML report compatible
 * with the Maven Surefire report standards.
 * 
 * @goal execute
 * 
 * @phase process-test-resources
 */
public class UtplsqlMojo extends AbstractMojo {

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
     * The username to connect to the database.
     * 
     * @parameter
     */
    private String password;

    /**
     * The name of the package to test. You must specify either this or the
     * testSuiteName parameter.
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
     * Location to which we will write the report file. Defaults to the Maven
     * /target directory of the project.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * Do the main work of the plugin here.
     */
    public void execute() throws MojoExecutionException {
        Connection conn = null;

        try {
            getLog().info("using JDBC driver : " + driver);
            Class.forName(driver);

            conn = DriverManager.getConnection(url, username, password);

            if (!StringUtils.isEmpty(packageName)) {
                runPackage(conn);
            } else {
                runTestSuite(conn);
            }

        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("JDBC Driver class not found", e);
        } catch (SQLException e) {
            throw new MojoExecutionException(
                    "Problem connecting to DB or executing SQL", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not build report", e);
        } catch (SplitterException e) {
            throw new MojoExecutionException(
                    "utPLSQL results not in expected format", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    /**
     * Run the utPLSQL tests in a single package. This method calls the 
     * relevant utPLSQL schema stored procedure and obtains the results,
     * exporting them in a Maven Surefire report. 
     * 
     * @param conn the database connection to use
     * @throws SQLException if there is a problem communicating with the database
     * @throws IOException if there is a problem writing the report file
     * @throws SplitterException if there is a problem generating the report
     */
    private void runPackage(Connection conn) throws SQLException, IOException,
            SplitterException {
        CallableStatement stmt = null;
        try {
            getLog().info("Running UTPLSQL tests for package " + packageName);
            
            // Call the utPLSQL test() method in PL/SQL, binding in the name
            // of the package that we want to execute and expecting the run_id
            // to be passed as an out parameter. We use the run_id to later 
            // look up the results of the test. 
            stmt = conn.prepareCall(PACKAGE_STMT);
            stmt.setString(1, packageName);
            stmt.registerOutParameter(2, Types.NUMERIC);
            stmt.execute();
            int runId = stmt.getInt(2);
            getLog().info("Creating test report for run " + runId);
            
            // build the report for this run ID
            File surefireDir = createAndCleanOutputDir();
            buildSurefirePackageReport(conn, surefireDir, runId, packageName);
            
        } finally {
            stmt.close();
        }
    }

    
    /**
     * Run the utPLSQL tests in a test suite. This method calls the 
     * relevant utPLSQL schema stored procedure and obtains the results,
     * exporting them in a Maven Surefire report.
     * 
     * Note that this method calculates the expected run_id for each package
     * within the test suite which may not always be accurate. 
     * 
     * @param conn the database connection to use
     * @throws SQLException if there is a problem communicating with the database
     * @throws IOException if there is a problem writing the report file
     * @throws SplitterException if there is a problem generating the report
     */
    private void runTestSuite(Connection conn) throws SQLException,
            IOException, SplitterException {
        CallableStatement stmt = null;
        try {
            getLog().info("Running UTPLSQL test suite " + testSuiteName);
            
            // execute the test suite, binding two output parameters - the 
            // run_id that corresponds to the test suite execution and the 
            // number of test packages in the test suite. We'll use these
            // values later to construct a report for each package. 
            stmt = conn.prepareCall(SUITE_STMT);
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
            for (int i = 1; i <= packageCount; i++) {
                int pkgRunId = runId - i;
                getLog().info("Creating test report for run " + pkgRunId);
                buildSurefirePackageReport(conn, surefireDir, pkgRunId,
                        testSuiteName + "-" + pkgRunId);
            }

        } finally {
            stmt.close();
        }
    }

    /**
     * Creates the output directory (if necessary) and removes any old reports
     * that are there. The removal may not be necessary. 
     * 
     * @return the File object that corresponds to the output directory
     * @throws IOException if there was a problem creating the directory or 
     * removing old contents. 
     */
    private File createAndCleanOutputDir() throws IOException {
        File surefireDir = new File(outputDirectory + "/surefire-reports");
        FileUtils.forceMkdir(surefireDir);
        FileUtils.cleanDirectory(surefireDir);
        return surefireDir;
    }

    /**
     * Given the run_id of a test package run, create a report in surefire
     * XML format. 
     * 
     * @param conn the connection to the database
     * @param surefireDir the directory to which we will write the surefire report
     * @param runId the test run ID
     * @param suiteOrPackageName the suite or package name
     * @throws SQLException if there was a problem getting the report data from
     * the database 
     * @throws IOException if there was a problem outputting the report to the 
     * filesystem
     * @throws SplitterException if there was a problem generating the report
     */
    private void buildSurefirePackageReport(Connection conn, File surefireDir,
            int runId, String suiteOrPackageName) throws SQLException,
            IOException, SplitterException {

        // get the status and description for each test that was run in the 
        // test run. 
        PreparedStatement stmt = conn
                .prepareStatement("select status, description from utr_outcome where run_id = ?");
        stmt.setInt(1, runId);
        ResultSet rs = stmt.executeQuery();

        int failureCounter = 0;
        int successCounter = 0;
        StringBuffer sb = new StringBuffer();

        while (rs.next()) {
            String status = rs.getString("status");
            String desc = rs.getString("description");
            getLog().debug("processing description: " + desc);
            
            // work out if the test was a success or failure
            // (only statuses possible in utPLSQL)
            if ("SUCCESS".equals(status)) {
                successCounter++;
            } else {
                failureCounter++;
            }

            // use the ResultSpitter to get the varios data elements 
            // contained in the description 
            // (the utPLSQL schema doesn't seem to be well normalised ;-) )
            DescContainer dc = ResultSplitter.split(desc);

            // generate the XML for each test executed and add it to a 
            // string buffer that we'll later write to a file
            sb.append("   <testcase classname=\"" + dc.getProcedureName()
                    + "\" ");
            sb.append("time=\"0\" ");
            sb.append("name=\"" + dc.getTestName() + "\" ");
            sb.append(">");

            if ("FAILURE".equals(status)) {
                sb.append("<failure message=\""
                        + StringEscapeUtils.escapeXml(dc.getTestDescription())
                        + "\"/>");
            }

            sb.append("</testcase>");
            sb.append("\n");

        }

        // write the entire report to a file. We have to do it this way round
        // in order to know all the failures/sucesses to complete the 
        // XML header element. Note that utPLSL does not provide any 
        // timing info. 
        
        FileWriter fw = null;
        try {

            File f = new File(surefireDir, "utplsql-" + suiteOrPackageName
                    + "-report.xml");
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
        } finally {
            IOUtils.closeQuietly(fw);
        }

    }

    private static final String PACKAGE_STMT = "begin "
            + "  utplsql.test (?, recompile_in => FALSE); "
            + "  ? := utplsql2.runnum; " + "end; ";

    private static final String SUITE_STMT = "declare  "
            + "  v_suite_pkg_count PLS_INTEGER; " + "begin   "
            + "  utplsql.testsuite (?, recompile_in => FALSE);  "
            + "  select count(*) suite_package_count "
            + "  into v_suite_pkg_count " + "  from ut_suite s, ut_package p  "
            + "  where s.id = p.suite_id  " + "  and s.name = Upper(?); "
            + "  ? := utplsql2.runnum;  " + "  ? := v_suite_pkg_count;  "
            + "end; ";

}
