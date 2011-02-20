package com.theserverlabs.maven.utplsql;

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

import com.theserverlabs.maven.utplsq.DescContainer;
import com.theserverlabs.maven.utplsq.ResultSplitter;

import junit.framework.TestCase;

/**
 * 
 * Unit tests for the ResultSplitter class. 
 *
 */
public class ResultSplitterTest extends TestCase {
	
    ResultSplitter decoder = new ResultSplitter();
    
	public void testEQ() throws Exception {
		DescContainer dc = decoder.split("betwnstr.UT_BETWNSTR_PROC: EQ \"zero start\" Expected \"abc\" and got \"ab\"");
		assertEquals("betwnstr.UT_BETWNSTR_PROC", dc.getProcedureName());
		assertEquals("zero start", dc.getTestName());
		assertEquals("EQ", dc.getType());
		assertEquals("Expected abc and got ab", dc.getResults());
	}
    
    public void testEQQueryValue() throws Exception {
        DescContainer dc = decoder.split("MYBOOKS_PKG.UT_6_DEL: EQQUERYVALUE \"ut_del-1\" Result: Query \"select count(*) from mybooks where book_id=100\" returned value \"0\" that does match \"0\"");
        assertEquals("MYBOOKS_PKG.UT_6_DEL", dc.getProcedureName());
        assertEquals("ut_del-1", dc.getTestName());
        assertEquals("EQQUERYVALUE", dc.getType());
        assertEquals("Query \"select count(*) from mybooks where book_id=100\" returned value \"0\" that does match \"0\"", dc.getResults());
    }
    
    public void testBoolean() throws Exception {
        // One of several no particular format messages that occur during a successful Assert.this check
        // Hopefully one day the utPLSQL will have a standard output format 
        DescContainer dc = decoder.split("PKGUSMMigrateAttributes.UT_NVSWITHNOCHANGE: Teardown complete");
        assertEquals("PKGUSMMigrateAttributes.UT_NVSWITHNOCHANGE", dc.getProcedureName());
        assertEquals("Teardown complete", dc.getTestName());
        assertEquals("Assert Type Unknown", dc.getType());
        assertEquals("", dc.getResults());
    }
    ///////////////////////////////////////////////////////////////////////////
    // Rare Error conditions
    //////////////////////////////////////////////////////////////////////////
    
	public void testUnableToRun() throws Exception {
	    // This message does not follow 
		DescContainer dc = decoder.split(".: Unable to run \"UTIL\".ut_UTIL_CONF.ut_SETUP: ORA-01031: insufficient privileges");
		assertEquals("UTIL\".ut_UTIL_CONF", dc.getProcedureName());
		assertEquals("ut_SETUP", dc.getTestName());
		// You can't make a purse from a sow's ear
		assertEquals("Assert Type Unknown", dc.getType());
		assertEquals("ORA-01031 insufficient privileges", dc.getResults());
	}
	
	public void testUnableToRunTeardown() throws Exception {
		DescContainer dc = decoder.split(".: Unable to run \"UTIL\".ut_UTIL_CONF.ut_TEARDOWN: ORA-06550: line 1, column 90:\nPL/SQL: ORA-00942: table or view does not exist\nORA-06510: PL/SQL: unhandled user-defined exception\nORA-06512: at \"UTP.UTASSERT2\", line 152\nORA-06512: at \"UTP.UTASSERT\", line 52\nORA-06512: at \"UTP.UTPLSQL\", line 446\nORA-01031: insufficient privileges\nORA-06550: line 1, column 46:\nPL/SQL: SQL Statement ignored\nORA-06510: PL/SQL: unhandled user-defined exception\nORA-06512: at \"UTP.UTASSERT2\", line 152\nORA-06512: at \"UTP.UTASSERT\", line 52\nORA-06512: at \"UTP.UTPLSQL\", line 446\nO");
		assertEquals("UTIL\".ut_UTIL_CONF", dc.getProcedureName());
		assertEquals("ut_TEARDOWN", dc.getTestName());
		assertEquals("Assert Type Unknown", dc.getType());
		assertEquals("ORA-06550 line 1, column 90\nPL/SQL ORA-00942 table or view does not exist\nORA-06510 PL/SQL unhandled user-defined exception\nORA-06512 at \"UTP.UTASSERT2\", line 152\nORA-06512 at \"UTP.UTASSERT\", line 52\nORA-06512 at \"UTP.UTPLSQL\", line 446\nORA-01031 insufficient privileges\nORA-06550 line 1, column 46\nPL/SQL SQL Statement ignored\nORA-06510 PL/SQL unhandled user-defined exception\nORA-06512 at \"UTP.UTASSERT2\", line 152\nORA-06512 at \"UTP.UTASSERT\", line 52\nORA-06512 at \"UTP.UTPLSQL\", line 446\nO", dc.getResults());
	}
	public void testUnableToRun2() throws Exception {
        // None standard format again! 
        // You can't make a purse from a sow's ear
        DescContainer dc = decoder.split("PKGUSMMigrateAttributes.UT_MIGDELETEADDSAME: Unable to run ut_PKGUSMMigrateAttributes.UT_MIGDELETEADDSAME: ORA-01403: no data found");
        assertEquals("PKGUSMMigrateAttributes.UT_MIGDELETEADDSAME", dc.getProcedureName());

        assertEquals("Unable to run ut_PKGUSMMigrateAttributes.UT_MIGDELETEADDSAME", dc.getTestName());

        assertEquals("Assert Type Unknown", dc.getType());
        assertEquals("ORA-01403 no data found", dc.getResults());
    }
    public void testEQError() throws Exception {
        DescContainer dc = decoder.split(".: EQ \"Check Migration Outcome An unexpected error occurred. -1 : ORA-00001: unique constraint (CRAMER.UM_USMSERVICE_FN_I) violated\" Expected \"0\" and got \"100\"");
        assertEquals(".ut_setup", dc.getProcedureName());
        // Another none standard condition, live with it!
        assertEquals("Check Migration Outcome An unexpected error occurred. -1", dc.getTestName());
        assertEquals("EQ", dc.getType());
        assertEquals("ORA-00001 unique constraint (CRAMER.UM_USMSERVICE_FN_I) violated\" Expected \"0\" and got \"100\"", dc.getResults());
   }
}
