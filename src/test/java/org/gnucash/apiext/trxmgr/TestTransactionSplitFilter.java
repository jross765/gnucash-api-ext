package org.gnucash.apiext.trxmgr;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;

import org.gnucash.api.read.GnuCashFile;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshSpltID;
import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;
import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class TestTransactionSplitFilter {
	
	public static final GCshSpltID TRXSPLT_1_ID = new GCshSpltID("b6a88c1d918e465892488c561e02831a");
	public static final GCshSpltID TRXSPLT_2_ID = new GCshSpltID("980706f1ead64460b8205f093472c855");
//	public static final GCshSpltID TRXSPLT_3_ID = new GCshSpltID("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

    private static final GCshAcctID ACCT_1_ID = TestTransactionFilter.ACCT_1_ID;
    private static final GCshAcctID ACCT_2_ID = TestTransactionFilter.ACCT_2_ID;
    private static final GCshAcctID ACCT_7_ID = TestTransactionFilter.ACCT_7_ID;
    private static final GCshAcctID ACCT_8_ID = TestTransactionFilter.ACCT_8_ID;

	// -----------------------------------------------------------------

	private GnuCashFile gcshFile = null;
	private TransactionSplitFilter flt = null;
	private GnuCashTransactionSplit splt = null;

	// -----------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	@SuppressWarnings("exports")
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestTransactionSplitFilter.class);
	}

	@Before
	public void initialize() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		// URL gcshFileURL = classLoader.getResource(Const.GCSH_FILENAME);
		// System.err.println("GnuCash test file resource: '" + gcshFileURL + "'");
		InputStream gcshFileStream = null;
		try {
			gcshFileStream = classLoader.getResourceAsStream(ConstTest.GCSH_FILENAME);
		} catch (Exception exc) {
			System.err.println("Cannot generate input stream from resource");
			return;
		}

		try {
			gcshFile = new GnuCashFileImpl(gcshFileStream);
		} catch (Exception exc) {
			System.err.println("Cannot parse GnuCash file");
			exc.printStackTrace();
		}
	}

	// -----------------------------------------------------------------

	@Test
	public void test01() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_1_ID);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.acctID.set(ACCT_2_ID);
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test02_1() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_1_ID);
		flt.valueFrom = new FixedPointNumber("-2253.00");
		flt.valueTo = new FixedPointNumber("-2253.00");
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("-2253.01");
		flt.valueTo = new FixedPointNumber("-2253.01");
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("-2254.00");
		flt.valueTo = new FixedPointNumber("-2252.00");
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("-2252.00");
		flt.valueTo = new FixedPointNumber("-2254.00");
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test02_2() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_7_ID);
		flt.valueFrom = new FixedPointNumber("-2253.00");
		flt.valueTo = new FixedPointNumber("2253.00");
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("2253.01");
		flt.valueTo = new FixedPointNumber("2253.01");
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("2252.00");
		flt.valueTo = new FixedPointNumber("2254.00");
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = new FixedPointNumber("2255.00");
		flt.valueTo = new FixedPointNumber("2252.00");
		assertEquals(false, flt.matchesCriteria(splt));

		// CAUTION: tolerance
		flt.valueFrom = new FixedPointNumber("2252.99");
		flt.valueTo = new FixedPointNumber("2252.9999");
		assertEquals(true, flt.matchesCriteria(splt)); // sic

		// CAUTION: tolerance
		flt.valueFrom = new FixedPointNumber("2253.0001");
		flt.valueTo = new FixedPointNumber("2254");
		assertEquals(true, flt.matchesCriteria(splt)); // sic
	}

	@Test
	public void test03_1() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_1_ID);
		flt.quantityFrom = new FixedPointNumber("-2253.00");
		flt.quantityTo = new FixedPointNumber("-2253.00");
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("-2253.01");
		flt.quantityTo = new FixedPointNumber("-2253.01");
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("-2254.00");
		flt.quantityTo = new FixedPointNumber("-2252.00");
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("-2252.00");
		flt.quantityTo = new FixedPointNumber("-2254.00");
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test03_2() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_7_ID);
		flt.quantityFrom = new FixedPointNumber("100.0000");
		flt.quantityTo = new FixedPointNumber("100.0000");
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("100.00");
		flt.quantityTo = new FixedPointNumber("100.01");
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("99.99");
		flt.quantityTo = new FixedPointNumber("100.00");
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = new FixedPointNumber("100.00");
		flt.quantityTo = new FixedPointNumber("99.99");
		assertEquals(false, flt.matchesCriteria(splt));
		
		// CAUTION: tolerance
		flt.quantityFrom = new FixedPointNumber("99.99");
		flt.quantityTo = new FixedPointNumber("99.9999");
		assertEquals(true, flt.matchesCriteria(splt)); // sic
		
		// CAUTION: tolerance
		flt.quantityFrom = new FixedPointNumber("100.0001");
		flt.quantityTo = new FixedPointNumber("101");
		assertEquals(true, flt.matchesCriteria(splt)); // sic
	}
	
	@Test
	public void test04() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_7_ID);
		flt.action = GnuCashTransactionSplit.Action.BUY;
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.action = GnuCashTransactionSplit.Action.SELL;
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test05() throws Exception {
		flt = new TransactionSplitFilter();
		flt.acctID.set(ACCT_7_ID);
		flt.descrPart = ""; // sic, the TRANSACTION's description is set, not the SPLIT's one
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.descrPart = "Poop";
		assertEquals(false, flt.matchesCriteria(splt));
	}
}
