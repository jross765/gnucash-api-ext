package org.gnucash.apiext.secacct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashFileImpl;
import org.gnucash.api.write.GnuCashWritableTransaction;
import org.gnucash.api.write.impl.GnuCashWritableFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshID;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.gnucash.base.tuples.AcctIDAmountPair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.JUnit4TestAdapter;
import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class TestSecuritiesAccountTransactionManager {

	private static GCshAcctID STOCK_ACCT_ID  = new GCshAcctID("b3741e92e3b9475b9d5a2dc8254a8111");
	private static GCshAcctID INCOME_ACCT_ID = new GCshAcctID("d7c384bfc136464490965f3f254313b1"); // only for dividend, not for
																						           // buy/sell
	private static List<AcctIDAmountPair> EXPENSES_ACCT_AMT_LIST = new ArrayList<AcctIDAmountPair>(); // only for dividend,
																									// not for buy/sell
	private static GCshAcctID OFFSET_ACCT_ID = new GCshAcctID("bbf77a599bd24a3dbfec3dd1d0bb9f5c");

	private static FixedPointNumber NOF_STOCKS = new FixedPointNumber(15);          // only for buy/sell, not for dividend
	private static FixedPointNumber STOCK_PRC  = new FixedPointNumber("23080/100"); // only for buy/sell, not for dividend
	private static FixedPointNumber DIV_GROSS  = new FixedPointNumber("11223/100"); // only for dividend, not for buy/sell

	private static LocalDate DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String DESCR = "Dividend payment";

	// ----------------------------

	private static GCshAcctID STOCK_BUY_EXP_ACCT_1_ID = new GCshAcctID( "7d4b851a3f704c4695d5d466b28cdc55" ); // Bankprovision

	FixedPointNumber STOCK_BUY_EXP_1 = new FixedPointNumber("945/100");
	
	// ----------------------------

	private static GCshAcctID DIVIDEND_EXP_ACCT_1_ID = new GCshAcctID( "2a195872e24048a0a6228107ca8b6a52" ); // Kapitalertragsteuer
	private static GCshAcctID DIVIDEND_EXP_ACCT_2_ID = new GCshAcctID( "41e998de2af144c7a9db5049fb677f8a" ); // Soli

	FixedPointNumber DIVIDEND_EXP_1 = DIV_GROSS.copy().multiply(new FixedPointNumber("25/100"));
	FixedPointNumber DIVIDEND_EXP_2 = STOCK_BUY_EXP_1.copy().multiply(new FixedPointNumber("55/100"));
	
	// -----------------------------------------------------------------

	private GnuCashWritableFileImpl gcshInFile = null;
	private GnuCashFileImpl gcshOutFile = null;

	private GCshTrxID newTrxID = null;

	// https://stackoverflow.com/questions/11884141/deleting-file-and-directory-in-junit
	@SuppressWarnings("exports")
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	// -----------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	@SuppressWarnings("exports")
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestSecuritiesAccountTransactionManager.class);
	}

	@Before
	public void initialize() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		// URL gcshFileURL = classLoader.getResource(Const.GCSH_FILENAME);
		// System.err.println("GnuCash test file resource: '" + gcshFileURL + "'");
		InputStream gcshInFileStream = null;
		try {
			gcshInFileStream = classLoader.getResourceAsStream(ConstTest.GCSH_FILENAME_IN);
		} catch (Exception exc) {
			System.err.println("Cannot generate input stream from resource");
			return;
		}

		try {
			gcshInFile = new GnuCashWritableFileImpl(gcshInFileStream);
		} catch (Exception exc) {
			System.err.println("Cannot parse GnuCash in-file");
			exc.printStackTrace();
		}
		
		// ---
		
		newTrxID = new GCshTrxID();
	}

	@Test
	public void test01() throws Exception {
		test01_initExpAccts();

		GnuCashWritableTransaction trx = 
				SecuritiesAccountTransactionManager
					.genBuyStockTrx(gcshInFile, 
									STOCK_ACCT_ID, EXPENSES_ACCT_AMT_LIST, OFFSET_ACCT_ID,
									NOF_STOCKS, STOCK_PRC, 
									DATE_POSTED, DESCR);
		assertNotEquals(null, trx);
		newTrxID.set(trx.getID());

		// ----------------------------
		// Now, check whether the generated object can be written to the
		// output file, then re-read from it, and whether is is what
		// we expect it is.

		File outFile = folder.newFile(ConstTest.GCSH_FILENAME_OUT);
		// System.err.println("Outfile for TestGnuCashWritableCustomerImpl.test01_1: '"
		// + outFile.getPath() + "'");
		outFile.delete(); // sic, the temp. file is already generated (empty),
							// and the GnuCash file writer does not like that.
		gcshInFile.writeFile(outFile);

		test01_check_persisted(outFile);
	}

	private void test01_check_persisted(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction trx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, trx);

//		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), ZoneId.systemDefault()), 
//					 trx.getDatePosted());
//		assertEquals(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
//					 trx.getDatePosted());
		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
									  ZoneId.ofOffset("", ZoneOffset.ofHours(1))), 
					 trx.getDatePosted());
		assertEquals(0, trx.getBalance().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(3, trx.getSplits().size());
		assertEquals(DESCR, trx.getDescription());

		// ---

		GnuCashTransactionSplit splt1 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(STOCK_ACCT_ID) ) {
				splt1 = splt;
				break;
			}
		}
		assertNotEquals(null, splt1);
		
		GnuCashTransactionSplit splt2 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(OFFSET_ACCT_ID) ) {
				splt2 = splt;
				break;
			}
		}
		assertNotEquals(null, splt2);
		
		GnuCashTransactionSplit splt3 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(STOCK_BUY_EXP_ACCT_1_ID) ) {
				splt3 = splt;
				break;
			}
		}
		assertNotEquals(null, splt3);
		
		// ---

		FixedPointNumber amtNet   = NOF_STOCKS.copy().multiply(STOCK_PRC);
		FixedPointNumber amtGross = amtNet.copy();
		for ( AcctIDAmountPair elt : EXPENSES_ACCT_AMT_LIST ) {
		    amtGross.add(elt.amount());
		}
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.BUY, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.BUY.getLocaleString(), splt1.getActionStr());
		assertEquals(NOF_STOCKS.doubleValue(), splt1.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(amtNet.doubleValue(), splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		assertEquals(amtGross.copy().negate().doubleValue(), splt2.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(amtGross.copy().negate().doubleValue(), splt2.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt2.getDescription());

		assertEquals(STOCK_BUY_EXP_ACCT_1_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(STOCK_BUY_EXP_1.doubleValue(), splt3.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(STOCK_BUY_EXP_1.doubleValue(), splt3.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt3.getDescription());
	}

	@Test
	public void test02() throws Exception {
		test02_initExpAccts();

		GnuCashWritableTransaction trx = 
				SecuritiesAccountTransactionManager
					.genDividDistribTrx(gcshInFile, 
									STOCK_ACCT_ID, INCOME_ACCT_ID, EXPENSES_ACCT_AMT_LIST, OFFSET_ACCT_ID,
									GnuCashTransactionSplit.Action.DIVIDEND, DIV_GROSS, 
									DATE_POSTED, DESCR);
		assertNotEquals(null, trx);
		newTrxID.set(trx.getID());

		// ----------------------------
		// Now, check whether the generated object can be written to the
		// output file, then re-read from it, and whether is is what
		// we expect it is.

		File outFile = folder.newFile(ConstTest.GCSH_FILENAME_OUT);
		// System.err.println("Outfile for TestGnuCashWritableCustomerImpl.test01_1: '"
		// + outFile.getPath() + "'");
		outFile.delete(); // sic, the temp. file is already generated (empty),
							// and the GnuCash file writer does not like that.
		gcshInFile.writeFile(outFile);

		test02_check_persisted(outFile);
	}

	private void test02_check_persisted(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction trx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, trx);

//		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), ZoneId.systemDefault()), 
//		 trx.getDatePosted());
//		assertEquals(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
//		 trx.getDatePosted());
		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
					 				  ZoneId.ofOffset("", ZoneOffset.ofHours(1))), 
					 trx.getDatePosted());
		assertEquals(0, trx.getBalance().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(5, trx.getSplits().size());
		assertEquals(DESCR, trx.getDescription());

		// ---

		GnuCashTransactionSplit splt1 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(STOCK_ACCT_ID) ) {
				splt1 = splt;
				break;
			}
		}
		assertNotEquals(null, splt1);
		
		GnuCashTransactionSplit splt2 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(OFFSET_ACCT_ID) ) {
				splt2 = splt;
				break;
			}
		}
		assertNotEquals(null, splt2);
		
		GnuCashTransactionSplit splt3 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(INCOME_ACCT_ID) ) {
				splt3 = splt;
				break;
			}
		}
		assertNotEquals(null, splt3);
		
		GnuCashTransactionSplit splt4 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(DIVIDEND_EXP_ACCT_1_ID) ) {
				splt4 = splt;
				break;
			}
		}
		assertNotEquals(null, splt4);
		
		GnuCashTransactionSplit splt5 = null;
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccountID().equals(DIVIDEND_EXP_ACCT_2_ID) ) {
				splt5 = splt;
				break;
			}
		}
		assertNotEquals(null, splt5);
		
		// ---

    	FixedPointNumber expensesSum = new FixedPointNumber();
    	for ( AcctIDAmountPair elt : EXPENSES_ACCT_AMT_LIST ) {
    	    expensesSum.add(elt.amount());
    	}
    	FixedPointNumber divNet = DIV_GROSS.copy().subtract(expensesSum);
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.DIVIDEND, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.DIVIDEND.getLocaleString(), splt1.getActionStr());
		assertEquals(0.0, splt1.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		assertEquals(divNet.doubleValue(), splt2.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(divNet.doubleValue(), splt2.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt2.getDescription());

		assertEquals(INCOME_ACCT_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(DIV_GROSS.copy().negate().doubleValue(), splt3.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(DIV_GROSS.copy().negate().doubleValue(), splt3.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt3.getDescription());

		assertEquals(DIVIDEND_EXP_ACCT_1_ID, splt4.getAccountID());
		assertEquals(null, splt4.getAction());
		assertEquals(DIVIDEND_EXP_1.doubleValue(), splt4.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(DIVIDEND_EXP_1.doubleValue(), splt4.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt4.getDescription());

		assertEquals(DIVIDEND_EXP_ACCT_2_ID, splt5.getAccountID());
		assertEquals(null, splt5.getAction());
		assertEquals(DIVIDEND_EXP_2.doubleValue(), splt5.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(DIVIDEND_EXP_2.doubleValue(), splt5.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals("", splt5.getDescription());
	}

	// ---------------------------------------------------------------
	
	// 
	private void test01_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountPair acctAmtPr1 = new AcctIDAmountPair(STOCK_BUY_EXP_ACCT_1_ID, STOCK_BUY_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1);
	}

	// Example for a dividend payment in Germany (domestic share).
	// If we had a foreign share (e.g. US), we would have to add a 
	// third entry to the list: "Auslaend. Quellensteuer" (that 
	// account is not in the test file yet).
	private void test02_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountPair acctAmtPr1 = new AcctIDAmountPair(DIVIDEND_EXP_ACCT_1_ID, DIVIDEND_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1);
		
		AcctIDAmountPair acctAmtPr2 = new AcctIDAmountPair(DIVIDEND_EXP_ACCT_2_ID, DIVIDEND_EXP_2);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr2);
	}

}
