package com.theserverlabs.maven.utplsq;

import org.apache.commons.lang.StringUtils;

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


/**
 * Utility class that splits the utPLSQL test outcome description into 
 * the various constituent elements using the JDK String.split() method. 
 * 
 * This is not the ideal solution but it is better than having to change
 * the utPLSQL schema which seems to be the alternative. 
 *
 */
public class ResultSplitter {

    private static final String RESULT_BLOCK_1 = "\" Result";
    private static final String RESULT_BLOCK_2 = "\" Expected \"";
    private static final String UNABLE_TO_RUN  = "Unable to run \"";
    private static final String ASSERT_TYPE_UNKNOWN = "Assert Type Unknown";
    
    /**
     * Take a description and split it into its various constituent elements. The 
     * description should be in the following format (without the square brackets):
     * [procedureName].[testName]:[testDescription] 
     * 
     *  
     * @param description the description input 
     * @return a DescContainer object that contains the description info 
     * @throws SplitterException if the description wasn't in the expected format. 
     */
    public static DescContainer split(String description) throws SplitterException {
        
        DescContainer dc = new DescContainer();
        
		String[] descComponents = description.split(":");
		
		// first part is the package and function name or suite
		String[] nameComponents = descComponents[0].split("\\.");

		// FIXME if the utPLSQL itself fails to run the suite then the component will be called .
		switch (nameComponents.length) {
		case 0:
		case 1:
			// single value suggest suite itself or setup/tear down tests
		    
		    
		    int errorMsgIndex = descComponents[1].indexOf(UNABLE_TO_RUN);
		    
		    if (errorMsgIndex<0)
		    {
		       dc.setProcedureName(descComponents[0]+"ut_setup");
		       String postAssertStr = extractAndSetAssertTest(dc,descComponents[1]);
		    
		       String postTestDesc = extractAndSetTestName(dc,postAssertStr);
		    			 
		       dc.setResults(getResults(postTestDesc,descComponents,2));
		    }
		    else // This message follows its own unique form!
		    {
		        int lastDot = descComponents[1].lastIndexOf('.');
		        dc.setProcedureName(descComponents[1].substring(errorMsgIndex+UNABLE_TO_RUN.length(),lastDot));
		        dc.setTestName(descComponents[1].substring(lastDot+1));
		        dc.setType(ASSERT_TYPE_UNKNOWN);
		        dc.setResults(fetchDesc(descComponents,2));
		    }
		    			
			break;
			
		case 2:
		    // Add the method name to the package so we can use the name for the test assert description 
			dc.setProcedureName(nameComponents[0]+"."+nameComponents[1]);
							
	         // Lets start after where we removed the Assert type
            String postAssertStr2 = extractAndSetAssertTest(dc,descComponents[1]);
            
            String postTestDesc2 = extractAndSetTestName(dc,postAssertStr2);
            	
            dc.setResults(getResults(postTestDesc2,descComponents,2));
            	
			break;
			
		default:
			throw new SplitterException("utPLSQL results not in the expected format! Please check the utPLSQL outcome table.");
		}
		
		return dc;
	}
    /**
     * Merge remaining description together
     * 
     * @param desc
     * @return
     */
    private static String fetchDesc(String desc[],int startIndex)
    {
        StringBuilder sb = new StringBuilder();
        
        // We have already processed desc[1] and [2]
        for (int i = startIndex; i < desc.length; i++) {
    
            sb.append(desc[i]);
        }
        
        return sb.toString().trim();
    }
    /**
     * Sets the Assert Test type  
     * 
     * @param typeStr and string containing the Assert Test
     * @return typeStr if not found or post Assert Test if found
     */
    private static String extractAndSetAssertTest(DescContainer dc,String typeStr)
    {                
        // Lets extract the assert test string which shall be added to the failure reports.
        int quotePos = typeStr.indexOf('"');
        
        if (quotePos > 0)
        {    
            dc.setType(typeStr.substring(0, quotePos).trim());
            return typeStr.substring(quotePos+1);
        }
        else
        {    
            dc.setType(ASSERT_TYPE_UNKNOWN);
            return typeStr;
        }

    }
    private static String extractAndSetTestName(DescContainer dc,String testStr)
    {                
        int resultPos = testStr.indexOf(RESULT_BLOCK_1);
        
        String testName,resultsStr = null;
        
        if (resultPos > 0)
        {
            testName = testStr.substring(0,resultPos);
        }
        else // Unable to identify a results section of maybe it the EQ form?
        {
            int expectedPos = testStr.indexOf(RESULT_BLOCK_2);
            
            if (expectedPos > 0)
            {     
                testName   = testStr.substring(0,expectedPos);   
                resultsStr = StringUtils.remove(testStr.substring(expectedPos+1).trim(),'"');
            }
            else
            {
               testName = testStr;
            }
        }
                            
        dc.setTestName(testName.trim());  
        
        return resultsStr;

    }
    /**
     * Selects the correct result set
     *  
     * @param testStr if none null this forms the results
     * @param desc the utPLSQL desc split by :
     * @param startIndex the start merge index for desc
     * @return
     */
    private static String getResults(String testStr,String desc[],int startIndex)
    {
        String resultsStr = testStr;
        // Now lets place the debug output/outcome into Results section
        
        if (resultsStr == null)
        {
            resultsStr = fetchDesc(desc,startIndex);
        }
            
        return resultsStr;
    }
 

}
