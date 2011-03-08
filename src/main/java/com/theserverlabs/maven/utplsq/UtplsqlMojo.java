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
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Mojo which contains a goal that executes a UTPLSQL unit test, obtaining the results from the database and presenting them in an XML report
 * compatible with the Maven Surefire report standards.
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
     * The name of the package to test. 
     * @deprecated Please use <packages><param>mypkg1</param><param>mypkg2</param></packages>
     * @parameter
     */
    private String packageName;
    
    /**
     * The name of all the packages to test. 
     * 
     * @parameter
     */
    private String[] packages;

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
     * Location to which we will write the report file. Defaults to the Maven /target directory of the project.
     * 
     * @parameter expression="${project.build.directory}"
     */
    private File outputDirectory;

    /**
     * If there is a connection problem or DB PLSQL installation error the plugin reports 0 tests run. This is an error condition which should be
     * flagged, by default an error exception shall be raised if 0 tests are run. This state can be unset in the maven plugin configuration if desired
     * 
     * @parameter expression=true
     */
    private Boolean failOnNoTests;

    /**
     * If errors occur the failure message can be written to the console, this was active by
     * default in version before 1.31, its now switched off by default. 
     * NB/ This information is always included in the surefire xml report.
     * 
     * @parameter expression=false
     */
    private Boolean writeFailuresToConsole;
    
    /**
     * Do the main work of the plugin here.
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        Connection conn = null;

        final String TEST_PKG   = "utplsql:Testing package ";
        final String TEST_SUITE = "utplsql:Testing suite  ";
        
        try
        {
            getLog().debug("using JDBC driver : " + driver);
            Class.forName(driver);

            conn = DriverManager.getConnection(url, username, password);

            TestResults testResults = new TestResults();

            String testTitle = null, testName = null;
            UtplsqlRunner runner = new UtplsqlRunner(getSurefireDir(), getLog());

            // Run testSuite
            if (!StringUtils.isEmpty(testSuiteName))
            {
                testResults = runner.runTestSuite(conn,testSuiteName, testMethod, setupMethod);
                testName = testSuiteName;
                testTitle = TEST_SUITE + testName;

            }
            // Run packageName
            else if (!StringUtils.isEmpty(packageName))
            {
                testResults = runner.runPackage(conn, packageName, testMethod, setupMethod);
                testName = packageName;
                testTitle = TEST_PKG + testName;

            }
            // Run packages
            else 
            {
                TestResults pkgTestResults;
                
                for (int index = 0; index < packages.length;index++)
                {  
                    pkgTestResults = runner.runPackage(conn, packages[index], testMethod, setupMethod);
                    
                    // We need to check for 0 tests run on each package otherwise we may miss an error
                    checkForNoTests(packages[index],pkgTestResults);
                                        
                    testResults.append(pkgTestResults);
                    
                }
                
                testName = mergePackageNames(packages);
                testTitle = TEST_PKG + testName;
               
            }
                              
            reportAndJudge(testResults, testTitle, testName);            
            

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
     * Inform user of testing outcome
     * 
     * @param testResults
     * @param testTitle the title of the test
     * @param testName the name of the test
     * @throws MojoFailureException if any tests fail or 0 tests were run
     */
    protected void reportAndJudge(TestResults testResults, String testTitle, String testName) throws MojoFailureException
    {

        getLog().info("\n------------------------------------\n" + "TESTS\n" + "------------------------------------\n" + testTitle + "\n"
                        + "Successes: " + testResults.getSuccesses() + ", Failures: " + testResults.getFailures() + "\n\n" + "Results:\n"
                        + "Tests run: " + testResults.getTestsRun() + ", Failures: " + testResults.getFailures() + "\n");

        if (writeFailuresToConsole.booleanValue())
        {
            for (Iterator i = testResults.getFailureDescriptions().iterator(); i.hasNext();)
            {
                getLog().info("------------------------------------");
                getLog().info((String) i.next());
            }
        }

        // Lets warn if any failure conditions occured
        if (testResults.getFailures() > 0)
        {
            throw new MojoFailureException("utPLSQL tests failed");
        }

        checkForNoTests(testName,testResults);
        
    }
    /**
     * Flags Potential Error In utPLSQL
     * 
     * @param testName name of the package being tested
     * @param testsRun the number of tests run
     * @throws MojoFailureException if no tests have been run.
     */
    protected void checkForNoTests(String testName,TestResults testResults) throws MojoFailureException
    {
        getLog().info(testName+
                        ", failed="+testResults.getFailures()+
                        ", passed=" + testResults.getSuccesses()+
                        ", total="+testResults.getTestsRun());
        
        if (testResults.getTestsRun() == 0 && failOnNoTests.booleanValue())
        {
            String noTestErrorMsg = "\n\n-------------------------\nutPLSQL Fail On No Tests"+
                                    "\n-------------------------\n" +
                                    "Please check plugin configuration and database for successful installation\n"+
                                    "of pkg 'ut_"+testName + "' and pkg|func|proc '"+ testName+
                                    "'\n";

            throw new MojoFailureException(noTestErrorMsg);

        }
    }
    /**
     * Merges package names together
     * 
     * @param packages
     * @return a comma separated list of packages
     */
    protected String mergePackageNames(String packages[])
    {
        StringBuffer buf = new StringBuffer();
        
        for (int index = 0; index < packages.length; index++)
        {
            buf.append(packages[index]);
            if (index<packages.length-1)
            {
                buf.append(',');
            }
        }
        return buf.toString();
    }
    /**
     * Clears any old reports and sets up new path
     * @return
     * @throws IOException 
     */
    protected File getSurefireDir() throws IOException
    {        
        File surefireDir = new File(outputDirectory,"surefire-reports");
    
        FileUtils.forceMkdir(surefireDir);
        FileUtils.cleanDirectory(surefireDir);
        
        return surefireDir;
    }
}
