package org.gnucash.apiext.trxmgr;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.time.LocalDate;

import org.gnucash.api.read.GnuCashFile;
import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.impl.GnuCashFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.apiext.trxmgr.TransactionFilter.SplitLogic;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;

public class TestTransactionFilter {
	
	public static final GCshTrxID TRX_1_ID = new GCshTrxID("cc9fe6a245df45ba9b494660732a7755");
	// public static final GCshTrxID TRX_2_ID = new GCshTrxID("xxx");

    public static final GCshAcctID ACCT_1_ID = new GCshAcctID("bbf77a599bd24a3dbfec3dd1d0bb9f5c"); // Root Account:Aktiva:Sichteinlagen:KK:Giro RaiBa
    public static final GCshAcctID ACCT_2_ID = new GCshAcctID("cc2c4709633943c39293bfd73de88c9b"); // Root Account:Aktiva:Depots:Depot RaiBa
    public static final GCshAcctID ACCT_7_ID = new GCshAcctID("d49554f33a0340bdb6611a1ab5575998"); // Root Account:Aktiva:Depots:Depot RaiBa:DE0007100000 Mercedes-Benz
    public static final GCshAcctID ACCT_8_ID = new GCshAcctID("b3741e92e3b9475b9d5a2dc8254a8111"); // Root Account:Aktiva:Depots:Depot RaiBa:DE0007164600 SAP

	// -----------------------------------------------------------------

	private GnuCashFile gcshFile = null;
	private TransactionFilter flt = null;
	private GnuCashTransaction trx = null;

	// -----------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	@SuppressWarnings("exports")
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestTransactionFilter.class);
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
		flt = new TransactionFilter();
		flt.spltFilt.acctID.set(ACCT_1_ID);
		trx = gcshFile.getTransactionByID(TRX_1_ID);
		
		assertEquals(true, flt.matchesCriteria(trx, false, SplitLogic.OR)); // AND works as well, because of 2nd arg.
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.AND));
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.spltFilt.acctID.set(ACCT_2_ID);
		assertEquals(true, flt.matchesCriteria(trx, false, SplitLogic.OR)); // sic, splits not checked, thus acct-ID not checked
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.AND));
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.OR));
	}

	@Test
	public void test02() throws Exception {
		flt = new TransactionFilter();
		flt.spltFilt.acctID.set(ACCT_1_ID);
		flt.datePostedFrom = LocalDate.of(2023, 7, 1);
		flt.datePostedTo = LocalDate.of(2023, 7, 1);
		trx = gcshFile.getTransactionByID(TRX_1_ID);
		
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.datePostedFrom = LocalDate.of(2023, 6, 20);
		flt.datePostedTo = LocalDate.of(2023, 7, 1);
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.datePostedFrom = LocalDate.of(2023, 7, 1);
		flt.datePostedTo = LocalDate.of(2023, 7, 20);
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.datePostedFrom = LocalDate.of(2023, 7, 20);
		flt.datePostedTo = LocalDate.of(2023, 7, 1);
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.OR));
	}

	@Test
	public void test03() throws Exception {
		flt = new TransactionFilter();
		flt.spltFilt.acctID.set(ACCT_1_ID);
		flt.descrPart = "MBG";
		trx = gcshFile.getTransactionByID(TRX_1_ID);
		
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.descrPart = "SAP";
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.OR));
	}

	@Test
	public void test04() throws Exception {
		flt = new TransactionFilter();
		flt.spltFilt.acctID.set(ACCT_1_ID);
		flt.nofSpltFrom = 1;
		flt.nofSpltTo = 10;
		trx = gcshFile.getTransactionByID(TRX_1_ID);
		
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.nofSpltFrom = 10;
		flt.nofSpltTo = 1;
		assertEquals(false, flt.matchesCriteria(trx, true, SplitLogic.OR));
		
		flt.nofSpltFrom = 3;
		flt.nofSpltTo = 3;
		assertEquals(true, flt.matchesCriteria(trx, true, SplitLogic.OR));
	}
}
