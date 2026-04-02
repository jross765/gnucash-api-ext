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
import org.gnucash.base.tuples.AcctIDAmountFPPair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.JUnit4TestAdapter;
import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class TestSecuritiesAccountTransactionManager_FP {

	private static GCshAcctID STOCK_ACCT_ID  = new GCshAcctID("b3741e92e3b9475b9d5a2dc8254a8111");
	private static GCshAcctID INCOME_ACCT_ID = new GCshAcctID("d7c384bfc136464490965f3f254313b1"); // only for dividend, not for
																						           // buy/sell
	private static List<AcctIDAmountFPPair> EXPENSES_ACCT_AMT_LIST = new ArrayList<AcctIDAmountFPPair>(); // only for dividend,
      																									     // not for buy/sell
	private static GCshAcctID OFFSET_ACCT_ID = new GCshAcctID("bbf77a599bd24a3dbfec3dd1d0bb9f5c");
	
	// ---

	private static FixedPointNumber BUY_NOF_STOCKS  = new FixedPointNumber(15);
	private static FixedPointNumber BUY_STOCK_PRC   = new FixedPointNumber("23080/100");
	private static FixedPointNumber BUY_NET_PRC     = BUY_STOCK_PRC.copy().multiply(BUY_NOF_STOCKS);
	private static FixedPointNumber BUY_EXP_1       = new FixedPointNumber("945/100");
	private static FixedPointNumber BUY_GROSS_PRC   = BUY_NET_PRC.copy().add(BUY_EXP_1);
	// .
	private static LocalDate        BUY_DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String           BUY_DESCR       = "Buying stocks";

	// ---

	private static FixedPointNumber DIV_GROSS       = new FixedPointNumber("11223/100");
	private static FixedPointNumber DIV_EXP_1       = DIV_GROSS.copy().multiply(new FixedPointNumber("25/100"));
	private static FixedPointNumber DIV_EXP_2       = BUY_EXP_1.copy().multiply(new FixedPointNumber("55/100"));
	private static FixedPointNumber DIV_FEETAX      = DIV_EXP_1.copy().add(DIV_EXP_2);
	private static FixedPointNumber DIV_NET         = DIV_GROSS.copy().subtract(DIV_FEETAX);
	
	private static LocalDate        DIV_DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String           DIV_DESCR       = "Dividend payment";

	// ---

	private static FixedPointNumber SPLT_NOF_SHR_BEFORE = new FixedPointNumber("5");
	private static FixedPointNumber SPLT_NOF_SHR_AFTER  = new FixedPointNumber("15");
	private static FixedPointNumber SPLT_FACTOR         = new FixedPointNumber("3");
	private static FixedPointNumber SPLT_NOF_ADD        = SPLT_NOF_SHR_AFTER.copy().subtract(SPLT_NOF_SHR_BEFORE);
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
		return new JUnit4TestAdapter(TestSecuritiesAccountTransactionManager_FP.class);
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
				SecuritiesAccountTransactionManager_FP
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

		assertEquals(BUY_NOF_STOCKS, specTrxRO.getNofShares());
		assertEquals(BUY_STOCK_PRC,  specTrxRO.getPricePerShare());
		assertEquals(BUY_NET_PRC,    specTrxRO.getNetPrice());
		assertEquals(BUY_EXP_1,      specTrxRO.getFeesTaxes());
		assertEquals(BUY_EXP_1,      specTrxRO.getFeeTax(BUY_EXP_ACCT_1_ID));
		assertEquals(BUY_GROSS_PRC,  specTrxRO.getGrossPrice());
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

		FixedPointNumber feeTaxFP = FixedPointNumber.ZERO.copy();
		for ( AcctIDAmountFPPair elt : EXPENSES_ACCT_AMT_LIST ) {
		    feeTaxFP.add(elt.amount()); // mutable
		}
    	FixedPointNumber prcGrossFP = BUY_NET_PRC.copy().add(feeTaxFP);
		
		assertEquals(BUY_EXP_1, feeTaxFP);
		assertEquals(BUY_GROSS_PRC, prcGrossFP);
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.BUY, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.BUY.getLocaleString(), splt1.getActionStr());
		assertEquals(BUY_NOF_STOCKS, splt1.getQuantity());
		assertEquals(BUY_NET_PRC, splt1.getValue());
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		assertEquals(BUY_GROSS_PRC.copy().negate(), splt2.getQuantity());
		assertEquals(BUY_GROSS_PRC.copy().negate(), splt2.getValue());
		assertEquals("", splt2.getDescription());

		assertEquals(BUY_EXP_ACCT_1_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(BUY_EXP_1, splt3.getQuantity());
		assertEquals(BUY_EXP_1, splt3.getValue());
		assertEquals("", splt3.getDescription());
	}

	@Test
	public void test02() throws Exception {
		test02_initExpAccts();

		GnuCashWritableStockDividendTransaction trx = 
				SecuritiesAccountTransactionManager_FP
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

		assertEquals(DIV_GROSS,  specTrxRO.getGrossDividend());
		assertEquals(DIV_FEETAX, specTrxRO.getFeesTaxes());
		assertEquals(DIV_EXP_1,  specTrxRO.getFeeTax(DIV_EXP_ACCT_1_ID));
		assertEquals(DIV_EXP_2,  specTrxRO.getFeeTax(DIV_EXP_ACCT_2_ID));
		assertEquals(DIV_NET,    specTrxRO.getNetDividend());
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

    	FixedPointNumber feeTaxFP = FixedPointNumber.ZERO.copy();
    	for ( AcctIDAmountFPPair elt : EXPENSES_ACCT_AMT_LIST ) {
    	    feeTaxFP.add(elt.amount()); // mutable
    	}
    	FixedPointNumber divNetFP = DIV_GROSS.copy().subtract(feeTaxFP);
		
		assertEquals(DIV_FEETAX, feeTaxFP);
		// .
		assertEquals(DIV_NET, divNetFP);
		
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
		assertEquals(DIV_NET, splt2.getQuantity());
		assertEquals(DIV_NET, splt2.getValue());
		assertEquals("", splt2.getDescription());

		assertEquals(INCOME_ACCT_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		assertEquals(DIV_GROSS.copy().negate(), splt3.getQuantity());
		assertEquals(DIV_GROSS.copy().negate(), splt3.getValue());
		assertEquals("", splt3.getDescription());

		assertEquals(DIV_EXP_ACCT_1_ID, splt4.getAccountID());
		assertEquals(null, splt4.getAction());
		assertEquals(DIV_EXP_1, splt4.getQuantity());
		assertEquals(DIV_EXP_1, splt4.getValue());
		assertEquals("", splt4.getDescription());

		assertEquals(DIV_EXP_ACCT_2_ID, splt5.getAccountID());
		assertEquals(null, splt5.getAction());
		assertEquals(DIV_EXP_2, splt5.getQuantity());
		assertEquals(DIV_EXP_2, splt5.getValue());

		assertEquals("", splt5.getDescription());
	}

	@Test
	public void test03_1() throws Exception {
		test03_initExpAccts();

		GnuCashAccount stockAcct = gcshInFile.getAccountByID(STOCK_ACCT_ID);
		assertEquals(SPLT_NOF_SHR_BEFORE, stockAcct.getBalance());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_FP
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_FP.StockSplitVar.FACTOR, SPLT_FACTOR, 
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
		assertEquals(SPLT_NOF_SHR_BEFORE, stockAcct.getBalance());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_FP
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_FP.StockSplitVar.NOF_ADD_SHARES, SPLT_NOF_ADD, 
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

		assertEquals(SPLT_NOF_SHR_BEFORE, specTrxRO.getNofSharesBeforeSplit());
		assertEquals(SPLT_NOF_SHR_AFTER,  specTrxRO.getNofSharesAfterSplit());
		assertEquals(SPLT_NOF_ADD,        specTrxRO.getNofAddShares());
		assertEquals(SPLT_FACTOR,         specTrxRO.getSplitFactor());
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
		assertEquals(SPLT_NOF_ADD, splt1.getQuantity());
		assertEquals(SPLT_NOF_SHR_AFTER, splt1.getAccount().getBalance());
		assertEquals(0.0, splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getValueRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(true, splt1.getDescription().startsWith("Generated by SecuritiesAccountTransactionManager"));
	}

	// ---------------------------------------------------------------
	
	// 
	private void test01_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountFPPair acctAmtPr1FP = new AcctIDAmountFPPair(BUY_EXP_ACCT_1_ID, BUY_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1FP);
	}

	// Example for a dividend payment in Germany (domestic share).
	// If we had a foreign share (e.g. US), we would have to add a 
	// third entry to the list: "Auslaend. Quellensteuer" (that 
	// account is not in the test file yet).
	private void test02_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
		
		AcctIDAmountFPPair acctAmtPr1FP = new AcctIDAmountFPPair(DIV_EXP_ACCT_1_ID, DIV_EXP_1);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr1FP);
		
		AcctIDAmountFPPair acctAmtPr2FP = new AcctIDAmountFPPair(DIV_EXP_ACCT_2_ID, DIV_EXP_2);
		EXPENSES_ACCT_AMT_LIST.add(acctAmtPr2FP);
	}

	private void test03_initExpAccts() {
		EXPENSES_ACCT_AMT_LIST.clear();
	}

}
