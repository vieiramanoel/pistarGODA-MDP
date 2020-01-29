package br.unb.cic.goda.testutils;

import static org.junit.Assert.assertEquals;

public class AssertUtils {

	/* assert equivalent formulas, ignoring blank spaces */
	public static void assertEq(String str1, String str2) {
		assertEquals(str1.replaceAll("\\s",""), str2.replaceAll("\\s",""));
	}

}
