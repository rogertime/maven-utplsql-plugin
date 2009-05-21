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

import junit.framework.TestCase;

import com.theserverlabs.maven.utplsq.DescContainer;
import com.theserverlabs.maven.utplsq.ResultSplitter;

/**
 * 
 * Unit tests for the ResultSplitter class. 
 *
 */
public class ResultSplitterTest extends TestCase {
	
	public void testSplit() throws Exception {
		DescContainer dc = ResultSplitter.split("betwnstr.UT_BETWNSTR_PROC: EQ \"zero start\" Expected \"abc\" and got \"ab\"");
		assertEquals("betwnstr", dc.getProcedureName());
		assertEquals("UT_BETWNSTR_PROC", dc.getTestName());
		assertEquals(" EQ \"zero start\" Expected \"abc\" and got \"ab\"", dc.getTestDescription());
	}
	
	public void testSplit2() throws Exception {
		DescContainer dc = ResultSplitter.split("MYBOOKS_PKG.UT_6_DEL: EQQUERYVALUE \"ut_del-1\" Result: Query \"select count(*) from mybooks where book_id=100\" returned value \"0\" that does match \"0\"");
		assertEquals("MYBOOKS_PKG", dc.getProcedureName());
		assertEquals("UT_6_DEL", dc.getTestName());
		assertEquals(" EQQUERYVALUE \"ut_del-1\" Result: Query \"select count(*) from mybooks where book_id=100\" returned value \"0\" that does match \"0\"", dc.getTestDescription());
	}
	

}
