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
 * Class that contains the various different elements of the description
 * held by utPLSQL as the result of a unit test.  
 */
public class DescContainer {

	private String procedureName;
	private String testName;
	private String type;
	private String results;
	private String duration = "0";
	
	public String getProcedureName() {
		return procedureName;
	}
	public void setProcedureName(String procedureName) {
		this.procedureName = procedureName;
	}
	public String getTestName() {
		return testName;
	}
	public void setTestName(String testName) {
		this.testName = testName;
	}
    public String getType()
    {
        return type;
    }
    public void setType(String type)
    {
        this.type = type;
    }
    public String getResults()
    {
        return results;
    }
    public void setResults(String results)
    {
        this.results = results;
    }
    public String getDuration()
    {
        return duration;
    }
    public void setDuration(String duration)
    {
        this.duration = duration;
    }
	
}
