package com.theserverlabs.maven.utplsq;

import java.util.ArrayList;

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
 * 
 * Simple class for storing results and the sure fire xml format
 * 
 */
public class TestResults
{
        private int successCounter = 0;
        private int failureCounter = 0;

        private ArrayList failureDescriptions = new ArrayList();
        
        private StringBuffer testXML = new StringBuffer();
 
        public void incSuccessCounter()
        {
            successCounter++;
        }
        public void incFailureCounter()
        {
            failureCounter++;
        }
    
        public int getTestsRun()
        {
            return successCounter+failureCounter;
        }
        
        public int getFailures()
        {
            return failureCounter;
        }
        
        public int getSuccesses()
        { 
            return successCounter;
        }

        public StringBuffer getTestXML()
        {
            return testXML;
        }

        public void setTestXML(StringBuffer resultsXML)
        {
            this.testXML = resultsXML;
        }   
        public ArrayList getFailureDescriptions()
        {
            return failureDescriptions;
        }
        public void setFailureDescriptions(ArrayList failureDescriptions)
        {
            this.failureDescriptions = failureDescriptions;
        }        
        
        /**
         * Merges several test results together to provide an overview
         * 
         * @param other
         */
        public void append(TestResults other)
        {
            successCounter += other.successCounter;
            failureCounter += other.failureCounter;
            failureDescriptions.addAll(other.failureDescriptions);
        }

}
