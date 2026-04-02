package org.gnucash.apiext.trxmgr;

import static org.junit.Assert.assertEquals;

import org.apache.commons.numbers.fraction.BigFraction;
import org.junit.Test;

// This test class proves that BigFraction.compareTo() is wrongly implemented.
// Therefore it must not used.
// Apr. 2nd, 2026 / Apache Commons Numbers V. 1.2
public class TestBF_Hlp {
	
	@Test
	public void test01() throws Exception {
		BigFraction val1 = BigFraction.of(2254);
		BigFraction val2 = BigFraction.of(2252);
		
		assertEquals(1, val1.compareTo(BigFraction.ZERO));
		assertEquals(1, val2.compareTo(BigFraction.ZERO));

		assertEquals(1, val1.compareTo(val2));  // <-- DOES NOT FAIL (CORRECT)
		assertEquals(-1, val2.compareTo(val1)); // dto.

		BigFraction diff = val2.subtract(val1);
		assertEquals(BigFraction.of(-2), diff);
		assertEquals(-1, diff.compareTo(BigFraction.ZERO)); // <-- DOES NOT FAIL (CORRECT)
		assertEquals(-1, diff.signum());                    // <-- dto.

		assertEquals(1,  val1.bigDecimalValue().compareTo(val2.bigDecimalValue()));
		assertEquals(-1, val2.bigDecimalValue().compareTo(val1.bigDecimalValue()));
	}
	
	@Test
	public void test02() throws Exception {
		BigFraction val1 = BigFraction.of(-2254);
		BigFraction val2 = BigFraction.of(-2252);
		
		assertEquals(-1, val1.compareTo(BigFraction.ZERO));
		assertEquals(-1, val2.compareTo(BigFraction.ZERO));

		// assertEquals(-1, val1.compareTo(val2)); // <-- WILL FAIL (WRONG)
		// assertEquals(1, val2.compareTo(val1));  // <-- dto.

		BigFraction diff = val2.subtract(val1);
		assertEquals(BigFraction.of(2), diff);
		assertEquals(1, diff.compareTo(BigFraction.ZERO)); // <-- DOES NOT FAIL (CORRECT)
		assertEquals(1, diff.signum());                    // dto.

		assertEquals(-1, val1.bigDecimalValue().compareTo(val2.bigDecimalValue()));
		assertEquals(1,  val2.bigDecimalValue().compareTo(val1.bigDecimalValue()));
	}
	
}