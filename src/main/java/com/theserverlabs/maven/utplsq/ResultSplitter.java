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


/**
 * Utility class that splits the utPLSQL test outcome description into 
 * the various constituent elements using the JDK String.split() method. 
 * 
 * This is not the ideal solution but it is better than having to change
 * the utPLSQL schema which seems to be the alternative. 
 *
 */
public class ResultSplitter {

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
		String [] nameComponents = descComponents[0].split("\\.");
		if (descComponents.length < 2 || nameComponents.length < 2) {
			throw new SplitterException("utPLSQL results not in the expected format!" +
					" Please check the utPLSQL outcome table.");
		}
		dc.setProcedureName(nameComponents[0]);
		dc.setTestName(nameComponents[1]);
		String desc = "";
		for (int i=1; i<descComponents.length; i++) {
			if (i > 1) {
				desc = desc + ":" + descComponents[i];
			} else {
				desc = desc + descComponents[i];
			}
		}
		dc.setTestDescription(desc);
		return dc;
	}
	
	
}
