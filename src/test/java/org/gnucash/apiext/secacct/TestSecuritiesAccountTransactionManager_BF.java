package org.gnucash.apiext.secacct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.numbers.fraction.BigFraction;
import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashFileImpl;
import org.gnucash.api.read.impl.GnuCashTransactionImpl;
import org.gnucash.api.write.impl.GnuCashWritableFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.apispec.read.GnuCashStockBuyTransaction;
import org.gnucash.apispec.read.GnuCashStockDividendTransaction;
import org.gnucash.apispec.read.GnuCashStockSplitTransaction;
import org.gnucash.apispec.read.impl.GnuCashStockBuyTransactionImpl;
import org.gnucash.apispec.read.impl.GnuCashStockDividendTransactionImpl;
import org.gnucash.apispec.read.impl.GnuCashStockSplitTransactionImpl;
import org.gnucash.apispec.write.GnuCashWritableStockBuyTransaction;
import org.gnucash.apispec.write.GnuCashWritableStockDividendTransaction;
import org.gnucash.apispec.write.GnuCashWritableStockSplitTransaction;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.gnucash.base.tuples.AcctIDAmountBFPair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.JUnit4TestAdapter;

public class TestSecuritiesAccountTransactionManager_BF {

	private static GCshAcctID STOCK_ACCT_ID  = new GCshAcctID("b3741e92e3b9475b9d5a2dc8254a8111");
	private static GCshAcctID INCOME_ACCT_ID = new GCshAcctID("d7c384bfc136464490965f3f254313b1"); // only for dividend, not for
																						           // buy/sell
	private static List<AcctIDAmountBFPair> EXPENSES_ACCT_AMT_LIST = new ArrayList<AcctIDAmountBFPair>(); // only for dividend,
      																									     // not for buy/sell
	private static GCshAcctID OFFSET_ACCT_ID = new GCshAcctID("bbf77a599bd24a3dbfec3dd1d0bb9f5c");
	
	// ---

	private static BigFraction      BUY_NOF_STOCKS  = BigFraction.of(15);
	private static BigFraction      BUY_STOCK_PRC   = BigFraction.of(23080, 100);
	private static BigFraction      BUY_NET_PRC     = BUY_STOCK_PRC.multiply(BUY_NOF_STOCKS);
	private static BigFraction      BUY_EXP_1       = BigFraction.of(945, 100);
	private static BigFraction      BUY_GROSS_PRC   = BUY_NET_PRC.add(BUY_EXP_1);
	// .
	private static LocalDate        BUY_DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String           BUY_DESCR       = "Buying stocks";

	// ---

	private static BigFraction      DIV_GROSS       = BigFraction.of(11223, 100);
	private static BigFraction      DIV_EXP_1       = DIV_GROSS.multiply(BigFraction.of(25, 100));
	private static BigFraction      DIV_EXP_2       = BUY_EXP_1.multiply(BigFraction.of(55, 100));
	private static BigFraction      DIV_FEETAX      = DIV_EXP_1.add(DIV_EXP_2);
	private static BigFraction      DIV_NET         = DIV_GROSS.subtract(DIV_FEETAX);
	
	private static LocalDate        DIV_DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String           DIV_DESCR       = "Dividend payment";

	// ---

	private static BigFraction      SPLT_NOF_SHR_BEFORE = BigFraction.of(5);
	private static BigFraction      SPLT_NOF_SHR_AFTER  = BigFraction.of(15);
	private static BigFraction      SPLT_FACTOR         = BigFraction.of(3);
	private static BigFraction      SPLT_NOF_ADD        = SPLT_NOF_SHR_AFTER.subtract(SPLT_NOF_SHR_BEFORE);
	// .
	private static LocalDate        SPLT_DATE_POSTED    = LocalDate.of(2026, 3, 1);
	private static String           SPLT_DESCR          = "Stock split";

	// ----------------------------

	private static GCshAcctID BUY_EXP_ACCT_1_ID = new GCshAcctID( "7d4b851a3f704c4695d5d466b28cdc55" ); // Bankprovision

	// ----------------------------

	private static GCshAcctID DIV_EXP_ACCT_1_ID = new GCshAcctID( "2a195872e24048a0a6228107ca8b6a52" ); // Kapitalertragsteuer
	private static GCshAcctID DIV_EXP_ACCT_2_ID = new GCshAcctID( "41e998de2af144c7a9db5049fb677f8a" ); // Soli

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
		return new JUnit4TestAdapter(TestSecuritiesAccountTransactionManager_BF.class);
	}

	@Before
	public void initialize() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		// URL gcshFileURL = classLoader.getResource(Const.GCSH_FILENAME);
		// System.err.println("GnuCash test file resource: '" + gcshFileURL + "'");
		URL gcshInFileURL = null;
		File gcshInFileRaw = null;
		try {
			gcshInFileURL = classLoader.getResource(ConstTest.GCSH_FILENAME);
			gcshInFileRaw = new File(gcshInFileURL.getFile());
		} catch (Exception exc) {
			System.err.println("Cannot generate input stream from resource");
			return;
		}

		try {
			gcshInFile = new GnuCashWritableFileImpl(gcshInFileRaw);
		} catch (Exception exc) {
			System.err.println("Cannot parse GnuCash in-file");
			exc.printStackTrace();
		}
		
		// ---
		
		newTrxID = new GCshTrxID();
	}

	// -----------------------------------------------------------------

	@Test
	public void test01() throws Exception {
		test01_initExpAccts();

		GnuCashWritableStockBuyTransaction trx = 
				SecuritiesAccountTransactionManager_BF
					.genBuyStockTrx(gcshInFile, 
									STOCK_ACCT_ID, EXPENSES_ACCT_AMT_LIST, OFFSET_ACCT_ID,
									BUY_NOF_STOCKS, BUY_STOCK_PRC, 
									BUY_DATE_POSTED, BUY_DESCR);
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

		test01_check_persisted_hl(outFile);
		test01_check_persisted_ml(outFile);
	}

	// High-level checks 
	private void test01_check_persisted_hl(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

		GnuCashStockBuyTransaction specTrxRO = new GnuCashStockBuyTransactionImpl((GnuCashTransactionImpl) genTrx);
		assertNotEquals(null, specTrxRO);
		assertEquals(newTrxID, specTrxRO.getID());

		// ---

		GnuCashTransactionSplit splt1 = specTrxRO.getStockAccountSplit();
		assertNotEquals(null, splt1);
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		
		GnuCashTransactionSplit splt2 = specTrxRO.getOffsettingAccountSplit();
		assertNotEquals(null, splt1);
		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		
		assertNotEquals(null, specTrxRO.getExpensesSplits());
		assertEquals(1, specTrxRO.getExpensesSplits().size());

		// ---

		assertEquals(BUY_NOF_STOCKS, specTrxRO.getNofSharesRat());
		assertEquals(BUY_STOCK_PRC,  specTrxRO.getPricePerShareRat());
		assertEquals(BUY_NET_PRC,    specTrxRO.getNetPriceRat());
		assertEquals(BUY_EXP_1,      specTrxRO.getFeesTaxesRat());
		assertEquals(BUY_EXP_1,      specTrxRO.getFeeTaxRat(BUY_EXP_ACCT_1_ID));
		assertEquals(BUY_GROSS_PRC,  specTrxRO.getGrossPriceRat());
	}

	// Mid-level checks (i.e., "manually") 
	private void test01_check_persisted_ml(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

//		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), ZoneId.systemDefault()), 
//					 specTrxRO.getDatePosted());
//		assertEquals(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
//					 specTrxRO.getDatePosted());
		assertEquals(ZonedDateTime.of(LocalDateTime.of(BUY_DATE_POSTED, LocalTime.MIDNIGHT), 
									  ZoneId.ofOffset("", ZoneOffset.ofHours(1))), 
				     genTrx.getDatePosted());
		// .
		assertEquals(0.0, genTrx.getBalance().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, genTrx.getBalanceRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals(3, genTrx.getSplits().size());
		assertEquals(BUY_DESCR, genTrx.getDescription());

		// ---

		GnuCashTransactionSplit splt1 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(STOCK_ACCT_ID) ) {
				splt1 = splt;
				break;
			}
		}
		assertNotEquals(null, splt1);
		
		GnuCashTransactionSplit splt2 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(OFFSET_ACCT_ID) ) {
				splt2 = splt;
				break;
			}
		}
		assertNotEquals(null, splt2);
		
		GnuCashTransactionSplit splt3 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(BUY_EXP_ACCT_1_ID) ) {
				splt3 = splt;
				break;
			}
		}
		assertNotEquals(null, splt3);
		
		// ---

		BigFraction feeTaxBF = BigFraction.ZERO;
		for ( AcctIDAmountBFPair elt : EXPENSES_ACCT_AMT_LIST ) {
			feeTaxBF = feeTaxBF.add(elt.amount()); // immutable
		}
    	BigFraction prcGrossBF = BUY_NET_PRC.add(feeTaxBF);
		
		assertEquals(BUY_EXP_1, feeTaxBF);
		assertEquals(BUY_GROSS_PRC, prcGrossBF);
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.BUY, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.BUY.getLocaleString(), splt1.getActionStr());
		assertEquals(BUY_NOF_STOCKS, splt1.getQuantityRat());
		assertEquals(BUY_NET_PRC, splt1.getValueRat());
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		assertEquals(BUY_GROSS_PRC.negate(), splt2.getQuantityRat());
		assertEquals(BUY_GROSS_PRC.negate(), splt2.getValueRat());
		assertEquals("", splt2.getDescription());

		assertEquals(BUY_EXP_ACCT_1_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(BUY_EXP_1, splt3.getQuantityRat());
		assertEquals(BUY_EXP_1, splt3.getValueRat());
		assertEquals("", splt3.getDescription());
	}

	@Test
	public void test02() throws Exception {
		test02_initExpAccts();

		GnuCashWritableStockDividendTransaction trx = 
				SecuritiesAccountTransactionManager_BF
					.genDividDistribTrx(gcshInFile, 
									STOCK_ACCT_ID, INCOME_ACCT_ID, EXPENSES_ACCT_AMT_LIST, OFFSET_ACCT_ID,
									GnuCashTransactionSplit.Action.DIVIDEND, DIV_GROSS, 
									DIV_DATE_POSTED, DIV_DESCR);
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

		test02_check_persisted_hl(outFile);
		test02_check_persisted_ml(outFile);
	}

	// High-level checks
	private void test02_check_persisted_hl(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

		GnuCashStockDividendTransaction specTrxRO = new GnuCashStockDividendTransactionImpl((GnuCashTransactionImpl) genTrx);
		assertNotEquals(null, specTrxRO);
		assertEquals(newTrxID, specTrxRO.getID());

		// ---

		GnuCashTransactionSplit splt1 = specTrxRO.getStockAccountSplit();
		assertNotEquals(null, splt1);
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		
		GnuCashTransactionSplit splt2 = specTrxRO.getOffsettingAccountSplit();
		assertNotEquals(null, splt1);
		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		
		assertNotEquals(null, specTrxRO.getExpensesSplits());
		assertEquals(2, specTrxRO.getExpensesSplits().size());

		// ---

		assertEquals(DIV_GROSS,  specTrxRO.getGrossDividendRat());
		assertEquals(DIV_FEETAX, specTrxRO.getFeesTaxesRat());
		assertEquals(DIV_EXP_1,  specTrxRO.getFeeTaxRat(DIV_EXP_ACCT_1_ID));
		assertEquals(DIV_EXP_2,  specTrxRO.getFeeTaxRat(DIV_EXP_ACCT_2_ID));
		assertEquals(DIV_NET,    specTrxRO.getNetDividendRat());
	}

	// Mid-level checks (i.e., "manually") 
	private void test02_check_persisted_ml(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

//		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), ZoneId.systemDefault()), 
//		 specTrxRO.getDatePosted());
//		assertEquals(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
//		 specTrxRO.getDatePosted());
		assertEquals(ZonedDateTime.of(LocalDateTime.of(DIV_DATE_POSTED, LocalTime.MIDNIGHT), 
					 				  ZoneId.ofOffset("", ZoneOffset.ofHours(1))), 
				     genTrx.getDatePosted());
		// .
		assertEquals(0.0, genTrx.getBalance().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, genTrx.getBalanceRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals(5, genTrx.getSplits().size());
		assertEquals(DIV_DESCR, genTrx.getDescription());

		// ---

		GnuCashTransactionSplit splt1 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(STOCK_ACCT_ID) ) {
				splt1 = splt;
				break;
			}
		}
		assertNotEquals(null, splt1);
		
		GnuCashTransactionSplit splt2 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(OFFSET_ACCT_ID) ) {
				splt2 = splt;
				break;
			}
		}
		assertNotEquals(null, splt2);
		
		GnuCashTransactionSplit splt3 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(INCOME_ACCT_ID) ) {
				splt3 = splt;
				break;
			}
		}
		assertNotEquals(null, splt3);
		
		GnuCashTransactionSplit splt4 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(DIV_EXP_ACCT_1_ID) ) {
				splt4 = splt;
				break;
			}
		}
		assertNotEquals(null, splt4);
		
		GnuCashTransactionSplit splt5 = null;
		for ( GnuCashTransactionSplit splt : genTrx.getSplits() ) {
			if ( splt.getAccountID().equals(DIV_EXP_ACCT_2_ID) ) {
				splt5 = splt;
				break;
			}
		}
		assertNotEquals(null, splt5);
		
		// ---

    	BigFraction feeTaxBF = BigFraction.ZERO;
    	for ( AcctIDAmountBFPair elt : EXPENSES_ACCT_AMT_LIST ) {
    	    feeTaxBF = feeTaxBF.add(elt.amount()); // immutable
    	}
    	BigFraction divNetBF = DIV_GROSS.subtract(feeTaxBF);
		
		assertEquals(DIV_FEETAX, feeTaxBF);
		// .
		assertEquals(DIV_NET, divNetBF);
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.DIVIDEND, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.DIVIDEND.getLocaleString(), splt1.getActionStr());
		// .
		assertEquals(0.0, splt1.getQuantity().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getQuantityRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals(0.0, splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getValueRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		assertEquals(DIV_NET, splt2.getQuantityRat());
		assertEquals(DIV_NET, splt2.getValueRat());
		assertEquals("", splt2.getDescription());

		assertEquals(INCOME_ACCT_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(DIV_GROSS.negate(), splt3.getQuantityRat());
		assertEquals(DIV_GROSS.negate(), splt3.getValueRat());
		assertEquals("", splt3.getDescription());

		assertEquals(DIV_EXP_ACCT_1_ID, splt4.getAccountID());
		assertEquals(null, splt4.getAction());
		assertEquals(DIV_EXP_1, splt4.getQuantityRat());
		assertEquals(DIV_EXP_1, splt4.getValueRat());
		assertEquals("", splt4.getDescription());

		assertEquals(DIV_EXP_ACCT_2_ID, splt5.getAccountID());
		assertEquals(null, splt5.getAction());
		assertEquals(DIV_EXP_2, splt5.getQuantityRat());
		assertEquals(DIV_EXP_2, splt5.getValueRat());

		assertEquals("", splt5.getDescription());
	}

	@Test
	public void test03_1() throws Exception {
		test03_initExpAccts();

		GnuCashAccount stockAcct = gcshInFile.getAccountByID(STOCK_ACCT_ID);
		assertEquals(SPLT_NOF_SHR_BEFORE, stockAcct.getBalanceRat());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_BF
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_BF.StockSplitVar.FACTOR, SPLT_FACTOR, 
									SPLT_DATE_POSTED, SPLT_DESCR);
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

		test03_check_persisted_hl(outFile);
		test03_check_persisted_ml(outFile);
	}

	@Test
	public void test03_2() throws Exception {
		test03_initExpAccts();

		GnuCashAccount stockAcct = gcshInFile.getAccountByID(STOCK_ACCT_ID);
		assertEquals(SPLT_NOF_SHR_BEFORE, stockAcct.getBalanceRat());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_BF
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_BF.StockSplitVar.NOF_ADD_SHARES, SPLT_NOF_ADD, 
									SPLT_DATE_POSTED, SPLT_DESCR);
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

		test03_check_persisted_hl(outFile);
		test03_check_persisted_ml(outFile);
	}
	
	// High-level checks 
	private void test03_check_persisted_hl(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

		GnuCashStockSplitTransaction specTrxRO = new GnuCashStockSplitTransactionImpl((GnuCashTransactionImpl) genTrx);
		assertNotEquals(null, specTrxRO);
		assertEquals(newTrxID, specTrxRO.getID());

		// ---

		GnuCashTransactionSplit splt1 = specTrxRO.getSplit();
		assertNotEquals(null, splt1);
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		
		// ---

		assertEquals(SPLT_NOF_SHR_BEFORE, specTrxRO.getNofSharesBeforeSplitRat());
		assertEquals(SPLT_NOF_SHR_AFTER,  specTrxRO.getNofSharesAfterSplitRat());
		assertEquals(SPLT_NOF_ADD,        specTrxRO.getNofAddSharesRat());
		assertEquals(SPLT_FACTOR,         specTrxRO.getSplitFactorRat());
	}

	// Mid-level checks (i.e., "manually") 
	private void test03_check_persisted_ml(File outFile) throws Exception {
		gcshOutFile = new GnuCashFileImpl(outFile);

		GnuCashTransaction genTrx = gcshOutFile.getTransactionByID(newTrxID);
		assertNotEquals(null, genTrx);

//		assertEquals(ZonedDateTime.of(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), ZoneId.systemDefault()), 
//		 specTrxRO.getDatePosted());
//		assertEquals(LocalDateTime.of(DATE_POSTED, LocalTime.MIDNIGHT), 
//		 specTrxRO.getDatePosted());
		assertEquals(ZonedDateTime.of(LocalDateTime.of(SPLT_DATE_POSTED, LocalTime.MIDNIGHT), 
					 				  ZoneId.ofOffset("", ZoneOffset.ofHours(1))), 
				     genTrx.getDatePosted());
		// .
		assertEquals(0.0, genTrx.getBalance().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, genTrx.getBalanceRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals(1, genTrx.getSplits().size());
		assertEquals(SPLT_DESCR, genTrx.getDescription());

		// ---

		GnuCashTransactionSplit splt1 = genTrx.getSplits().get(0);
		assertNotEquals(null, splt1);
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		
		// ---

		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.SPLIT, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.SPLIT.getLocaleString(), splt1.getActionStr());
		assertEquals(SPLT_NOF_ADD, splt1.getQuantityRat());
		assertEquals(SPLT_NOF_SHR_AFTER, splt1.getAccount().getBalanceRat());
		assertEquals(0.0, splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getValueRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(true, splt1.getDescription().startsWith("Generated by SecuritiesAccountTransactionManager"));
	}

	// ---------------------------------------------------------------
	
	// 
	private void test01_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountBFPair acctAmtPr1BF = new AcctIDAmountBFPair(BUY_EXP_ACCT_1_ID, BUY_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1BF);
	}

	// Example for a dividend payment in Germany (domestic share).
	// If we had a foreign share (e.g. US), we would have to add a 
	// third entry to the list: "Auslaend. Quellensteuer" (that 
	// account is not in the test file yet).
	private void test02_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountBFPair acctAmtPr1BF = new AcctIDAmountBFPair(DIV_EXP_ACCT_1_ID, DIV_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1BF);
		
		AcctIDAmountBFPair acctAmtPr2BF = new AcctIDAmountBFPair(DIV_EXP_ACCT_2_ID, DIV_EXP_2);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr2BF);
	}

	private void test03_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
	}

}
