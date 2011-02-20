package com.theserverlabs.maven.utplsq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
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
 * Fetches utPLSQL test results from the database and converts them to a SureFire compatible
 * form.
 */

public class SureFireReport
{
    File surefireDir;
    Log log;
    
    /**
     * Formats utplsql results into a surefire xml report
     * 
     * @param outputDir where the sure fire report shall be written
     * @param log
     * @throws IOException
     */
    public SureFireReport(File       outputDir,
                          Log        log) throws IOException
    {       
        this.log = log;   
        surefireDir = outputDir;
    }
      
    /**
     * Given the run_id of a test package run, create a report in surefire XML format.
     * 
     * @param conn
     *            the connection to the database
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
    public  TestResults build(Connection conn, 
                              int    runId, 
                              String suiteOrPackageName,
                              long   duration) throws SQLException,IOException, SplitterException
    {
        // Fetch the utPLSQL results from the db
        // test run.
        PreparedStatement stmt = conn.prepareStatement("select status, description from utr_outcome where run_id = ? order by outcome_id DESC");
        stmt.setInt(1, runId);
        ResultSet rs = stmt.executeQuery();
        DescContainer dc;
        
        TestResults testResult = new TestResults();
          
        while (rs.next())
        {
            // The description is a miss mash of output separated by ':' which we attempt to format
            // into a logical format!
            
            String desc = rs.getString("description");
            String status = rs.getString("status");
            
            // use the ResultSpitter to get the various data elements
            // contained in the description
            // (the utPLSQL schema doesn't seem to be well normalised ;-) )
            
            dc = ResultSplitter.split(desc);
            
            addTestResults(status,dc,testResult);
             
        }

        writeXML(testResult,suiteOrPackageName,duration);
        
        return testResult;
    }
    /**
     * Converts test results into a Surefire test node
     * 
     * @param status
     * @param dc a decoded utplsql description
     * @param testResult populated with failures and latest success/failure count
     * @throws SplitterException
     */
    protected void addTestResults(String status,DescContainer dc,TestResults tr) throws SplitterException
    {
        // work out if the test was a success or failure
        // (only statuses possible in utPLSQL)
        if ("SUCCESS".equals(status))
        {
            tr.incSuccessCounter();
        }
        else
        {
            tr.incFailureCounter();
        }

        // generate the XML for each test executed and add it to a
        // string buffer that we'll later write to a file
        tr.getTestXML().append("\n   <testcase classname=\"" + 
                                  dc.getProcedureName()+ "\" ");
        
        // Remove Quotes to make text more readable
        tr.getTestXML().append("name=\"" + StringEscapeUtils.escapeXml(dc.getTestName()) + "\" ");
        tr.getTestXML().append("time=\""+dc.getDuration()+"\"");
        tr.getTestXML().append(">");

        if ("FAILURE".equals(status))
        {
            tr.getTestXML().append("\n       <failure type=\""+dc.getType()+"\"");
            
            tr.getTestXML().append(" message=\"");
            
            String resultsMinusQuotes = StringUtils.remove(dc.getResults(),'"');
            tr.getTestXML().append(StringEscapeUtils.escapeXml(resultsMinusQuotes));
            
            tr.getTestXML().append("\"/>\n   ");

            tr.getFailureDescriptions().add(
                            dc.getProcedureName()+"\n"+
                            dc.getTestName()+"\n"+
                            dc.getResults());
        }

        tr.getTestXML().append("</testcase>");
    }
    /**
     * Writes the test xml with a header to the surefire report directory
     * 
     * @param testXML
     * @throws IOException
     */
    protected void writeXML(TestResults testResults,String suiteOrPackageName,long duration) throws IOException
    {
        // write the entire report to a file. We have to do it this way round
        // in order to know all the failures/sucesses to complete the
        // XML header element. Note that utPLSL does not provide any
        // timing info.
        
        String reportFile = "utplsql-" + suiteOrPackageName + "-report.xml";
        
        FileWriter fw = null;
        try
        {
            File f = new File(surefireDir, reportFile);
            fw = new FileWriter(f);
            fw.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            fw.append("\n<testsuite name=\"" + suiteOrPackageName + "\" "); 
            fw.append("tests=\"");
            fw.append(Integer.toString(testResults.getTestsRun()));
            fw.append("\" failures=\"");
            fw.append(Integer.toString(testResults.getFailures()));
            fw.append("\" skipped=\"0\" errors=\"0\" time=\""+duration+"msec\">");
            fw.append(testResults.getTestXML().toString());
            fw.append("\n</testsuite>");
            
        } finally
        {
            IOUtils.closeQuietly(fw);
        }
        
        log.debug("Writing Surefire file "+reportFile+" tests run "+testResults.getTestsRun());
        
    }
}
