package org.gnucash.apiext.trxmgr;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.apache.commons.numbers.fraction.BigFraction;
import org.gnucash.api.read.GnuCashFile;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshSpltID;
import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;

public class TestTransactionSplitFilter_BF {
	
	public static final GCshSpltID TRXSPLT_1_ID = new GCshSpltID("b6a88c1d918e465892488c561e02831a");
	public static final GCshSpltID TRXSPLT_2_ID = new GCshSpltID("980706f1ead64460b8205f093472c855");
//	public static final GCshSpltID TRXSPLT_3_ID = new GCshSpltID("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

    private static final GCshAcctID ACCT_1_ID = TestTransactionFilter.ACCT_1_ID;
    private static final GCshAcctID ACCT_2_ID = TestTransactionFilter.ACCT_2_ID;
    private static final GCshAcctID ACCT_7_ID = TestTransactionFilter.ACCT_7_ID;
    private static final GCshAcctID ACCT_8_ID = TestTransactionFilter.ACCT_8_ID;

	// -----------------------------------------------------------------

	private GnuCashFile gcshFile = null;
	private TransactionSplitFilter_BF flt = null;
	private GnuCashTransactionSplit splt = null;

	// -----------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	@SuppressWarnings("exports")
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestTransactionSplitFilter_BF.class);
	}

	@Before
	public void initialize() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		// URL gcshFileURL = classLoader.getResource(Const.GCSH_FILENAME);
		// System.err.println("GnuCash test file resource: '" + gcshFileURL + "'");
		URL gcshFileURL = null;
		File gcshFileRaw = null;
		try {
			gcshFileURL = classLoader.getResource(ConstTest.GCSH_FILENAME);
			gcshFileRaw = new File(gcshFileURL.getFile());
		} catch (Exception exc) {
			System.err.println("Cannot generate input stream from resource");
			return;
		}

		try {
			gcshFile = new GnuCashFileImpl(gcshFileRaw);
		} catch (Exception exc) {
			System.err.println("Cannot parse GnuCash file");
			exc.printStackTrace();
		}
	}

	// -----------------------------------------------------------------

	@Test
	public void test01() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_1_ID);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.acctID.set(ACCT_2_ID);
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test02_1() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_1_ID);
		flt.valueFrom = BigFraction.of(-2253);
		flt.valueTo = BigFraction.of(-2253);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(-225301, 100);
		flt.valueTo = BigFraction.of(-225301, 100);
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(-2254);
		flt.valueTo = BigFraction.of(-2252);
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(-2252);
		flt.valueTo = BigFraction.of(-2254);
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test02_2() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_7_ID);
		flt.valueFrom = BigFraction.of(-2253);
		flt.valueTo = BigFraction.of(2253);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(225301, 100);
		flt.valueTo = BigFraction.of(225301, 100);
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(2252);
		flt.valueTo = BigFraction.of(2254);
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.valueFrom = BigFraction.of(2255);
		flt.valueTo = BigFraction.of(2252);
		assertEquals(false, flt.matchesCriteria(splt));

		// CAUTION: No tolerance here, as opposed to FP variant
		flt.valueFrom = BigFraction.of(225299, 100);
		flt.valueTo = BigFraction.of(22529999, 10000);
		assertEquals(false, flt.matchesCriteria(splt));

		// CAUTION: No tolerance here, as opposed to FP variant
		flt.valueFrom = BigFraction.of(22530001, 10000);
		flt.valueTo = BigFraction.of(2254);
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test03_1() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_1_ID);
		flt.quantityFrom = BigFraction.of(-2253);
		flt.quantityTo = BigFraction.of(-2253);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_1_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(-225301, 100);
		flt.quantityTo = BigFraction.of(-225301, 100);
		assertEquals(false, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(-2254);
		flt.quantityTo = BigFraction.of(-2252);
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(-2252);
		flt.quantityTo = BigFraction.of(-2254);
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test03_2() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_7_ID);
		flt.quantityFrom = BigFraction.of(100);
		flt.quantityTo = BigFraction.of(100);
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(100);
		flt.quantityTo = BigFraction.of(10001, 100);
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(9999, 100);
		flt.quantityTo = BigFraction.of(100);
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.quantityFrom = BigFraction.of(100);
		flt.quantityTo = BigFraction.of(9999, 100);
		assertEquals(false, flt.matchesCriteria(splt));
		
		// CAUTION: No tolerance here, as opposed to FP variant
		flt.quantityFrom = BigFraction.of(9999, 100);
		flt.quantityTo = BigFraction.of(999999, 10000);
		assertEquals(false, flt.matchesCriteria(splt));
		
		// CAUTION: No tolerance here, as opposed to FP variant
		flt.quantityFrom = BigFraction.of(1000001, 10000);
		flt.quantityTo = BigFraction.of(101);
		assertEquals(false, flt.matchesCriteria(splt));
	}
	
	@Test
	public void test04() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_7_ID);
		flt.action = GnuCashTransactionSplit.Action.BUY;
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.action = GnuCashTransactionSplit.Action.SELL;
		assertEquals(false, flt.matchesCriteria(splt));
	}

	@Test
	public void test05() throws Exception {
		flt = new TransactionSplitFilter_BF();
		flt.acctID.set(ACCT_7_ID);
		flt.descrPart = ""; // sic, the TRANSACTION's description is set, not the SPLIT's one
		splt = gcshFile.getTransactionSplitByID(TRXSPLT_2_ID);
		
		assertEquals(true, flt.matchesCriteria(splt));
		
		flt.descrPart = "Poop";
		assertEquals(false, flt.matchesCriteria(splt));
	}
}
