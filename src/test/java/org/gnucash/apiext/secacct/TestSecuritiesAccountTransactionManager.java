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
import org.gnucash.base.tuples.AcctIDAmountFPPair;
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
	private static List<AcctIDAmountFPPair> EXPENSES_ACCT_AMT_FP_LIST = new ArrayList<AcctIDAmountFPPair>(); // only for dividend,
	private static List<AcctIDAmountBFPair> EXPENSES_ACCT_AMT_BF_LIST = new ArrayList<AcctIDAmountBFPair>(); // only for dividend,
      																									     // not for buy/sell
	private static GCshAcctID OFFSET_ACCT_ID = new GCshAcctID("bbf77a599bd24a3dbfec3dd1d0bb9f5c");
	
	// ---

	private static FixedPointNumber BUY_NOF_STOCKS_FP  = new FixedPointNumber(15);
	private static FixedPointNumber BUY_STOCK_PRC_FP   = new FixedPointNumber("23080/100");
	private static FixedPointNumber BUY_NET_PRC_FP     = BUY_STOCK_PRC_FP.copy().multiply(BUY_NOF_STOCKS_FP);
	private static FixedPointNumber BUY_EXP_1_FP       = new FixedPointNumber("945/100");
	private static FixedPointNumber BUY_GROSS_PRC_FP   = BUY_NET_PRC_FP.copy().add(BUY_EXP_1_FP);
	// .
	private static BigFraction      BUY_NOF_STOCKS_BF  = BigFraction.of(15);
	private static BigFraction      BUY_STOCK_PRC_BF   = BigFraction.of(23080, 100);
	private static BigFraction      BUY_NET_PRC_BF     = BUY_STOCK_PRC_BF.multiply(BUY_NOF_STOCKS_BF);
	private static BigFraction      BUY_EXP_1_BF       = BigFraction.of(945, 100);
	private static BigFraction      BUY_GROSS_PRC_BF   = BUY_NET_PRC_BF.add(BUY_EXP_1_BF);
	// .
	private static LocalDate        BUY_DATE_POSTED    = LocalDate.of(2024, 3, 1);
	private static String           BUY_DESCR          = "Buying stocks";

	// ---

	private static FixedPointNumber DIV_GROSS_FP       = new FixedPointNumber("11223/100");
	private static FixedPointNumber DIV_EXP_1_FP       = DIV_GROSS_FP.copy().multiply(new FixedPointNumber("25/100"));
	private static FixedPointNumber DIV_EXP_2_FP       = BUY_EXP_1_FP.copy().multiply(new FixedPointNumber("55/100"));
	private static FixedPointNumber DIV_FEETAX_FP      = DIV_EXP_1_FP.copy().add(DIV_EXP_2_FP);
	private static FixedPointNumber DIV_NET_FP         = DIV_GROSS_FP.copy().subtract(DIV_FEETAX_FP);
	// .
	private static BigFraction      DIV_GROSS_BF       = BigFraction.of(11223, 100);
	private static BigFraction      DIV_EXP_1_BF       = DIV_GROSS_BF.multiply(BigFraction.of(25, 100));
	private static BigFraction      DIV_EXP_2_BF       = BUY_EXP_1_BF.multiply(BigFraction.of(55, 100));
	private static BigFraction      DIV_FEETAX_BF      = DIV_EXP_1_BF.add(DIV_EXP_2_BF);
	private static BigFraction      DIV_NET_BF         = DIV_GROSS_BF.subtract(DIV_FEETAX_BF);
	
	private static LocalDate        DIV_DATE_POSTED = LocalDate.of(2024, 3, 1);
	private static String           DIV_DESCR       = "Dividend payment";

	// ---

	private static FixedPointNumber SPLT_NOF_SHR_BEFORE_FP = new FixedPointNumber("5");
	private static FixedPointNumber SPLT_NOF_SHR_AFTER_FP  = new FixedPointNumber("15");
	private static FixedPointNumber SPLT_FACTOR_FP         = new FixedPointNumber("3");
	private static FixedPointNumber SPLT_NOF_ADD_FP        = SPLT_NOF_SHR_AFTER_FP.copy().subtract(SPLT_NOF_SHR_BEFORE_FP);
	// .
	private static BigFraction      SPLT_NOF_SHR_BEFORE_BF = BigFraction.of(5);
	private static BigFraction      SPLT_NOF_SHR_AFTER_BF  = BigFraction.of(15);
	private static BigFraction      SPLT_FACTOR_BF         = BigFraction.of(3);
	private static BigFraction      SPLT_NOF_ADD_BF        = SPLT_NOF_SHR_AFTER_BF.subtract(SPLT_NOF_SHR_BEFORE_BF);
	// .
	private static LocalDate        SPLT_DATE_POSTED       = LocalDate.of(2026, 3, 1);
	private static String           SPLT_DESCR             = "Stock split";

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
		return new JUnit4TestAdapter(TestSecuritiesAccountTransactionManager.class);
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
									STOCK_ACCT_ID, EXPENSES_ACCT_AMT_FP_LIST, OFFSET_ACCT_ID,
									BUY_NOF_STOCKS_FP, BUY_STOCK_PRC_FP, 
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

		assertEquals(BUY_NOF_STOCKS_FP, specTrxRO.getNofShares());
		assertEquals(BUY_NOF_STOCKS_BF, specTrxRO.getNofSharesRat());
		// .
		assertEquals(BUY_STOCK_PRC_FP,  specTrxRO.getPricePerShare());
		assertEquals(BUY_STOCK_PRC_BF,  specTrxRO.getPricePerShareRat());
		// .
		assertEquals(BUY_NET_PRC_FP,    specTrxRO.getNetPrice());
		assertEquals(BUY_NET_PRC_BF,    specTrxRO.getNetPriceRat());
		// .
		assertEquals(BUY_EXP_1_FP,      specTrxRO.getFeesTaxes());
		assertEquals(BUY_EXP_1_BF,      specTrxRO.getFeesTaxesRat());
		// .
		assertEquals(BUY_EXP_1_FP,      specTrxRO.getFeeTax(BUY_EXP_ACCT_1_ID));
		assertEquals(BUY_EXP_1_BF,      specTrxRO.getFeeTaxRat(BUY_EXP_ACCT_1_ID));
		// .
		assertEquals(BUY_GROSS_PRC_FP,  specTrxRO.getGrossPrice());
		assertEquals(BUY_GROSS_PRC_BF,  specTrxRO.getGrossPriceRat());
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
		BigFraction      feeTaxBF = BigFraction.ZERO;
		for ( AcctIDAmountFPPair elt : EXPENSES_ACCT_AMT_FP_LIST ) {
		    feeTaxFP.add(elt.amount()); // mutable
		}
		for ( AcctIDAmountBFPair elt : EXPENSES_ACCT_AMT_BF_LIST ) {
			feeTaxBF = feeTaxBF.add(elt.amount()); // immutable
		}
    	FixedPointNumber prcGrossFP = BUY_NET_PRC_FP.copy().add(feeTaxFP);
    	BigFraction      prcGrossBF = BUY_NET_PRC_BF.add(feeTaxBF);
		
		assertEquals(BUY_EXP_1_FP, feeTaxFP);
		assertEquals(BUY_EXP_1_BF, feeTaxBF);
		// .
		assertEquals(BUY_GROSS_PRC_FP, prcGrossFP);
		assertEquals(BUY_GROSS_PRC_BF, prcGrossBF);
		
		assertEquals(STOCK_ACCT_ID, splt1.getAccountID());
		assertEquals(GnuCashTransactionSplit.Action.BUY, splt1.getAction());
		assertEquals(GnuCashTransactionSplit.Action.BUY.getLocaleString(), splt1.getActionStr());
		// .
		assertEquals(BUY_NOF_STOCKS_FP, splt1.getQuantity());
		assertEquals(BUY_NOF_STOCKS_BF, splt1.getQuantityRat());
		// .
		assertEquals(BUY_NET_PRC_FP, splt1.getValue());
		assertEquals(BUY_NET_PRC_BF, splt1.getValueRat());
		// .
		assertEquals("", splt1.getDescription());

		assertEquals(OFFSET_ACCT_ID, splt2.getAccountID());
		assertEquals(null, splt2.getAction());
		// .
		assertEquals(BUY_GROSS_PRC_FP.copy().negate(), splt2.getQuantity());
		assertEquals(BUY_GROSS_PRC_BF.negate(),        splt2.getQuantityRat());
		// .
		assertEquals(BUY_GROSS_PRC_FP.copy().negate(), splt2.getValue());
		assertEquals(BUY_GROSS_PRC_BF.negate(),        splt2.getValueRat());
		// .
		assertEquals("", splt2.getDescription());

		assertEquals(BUY_EXP_ACCT_1_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		// .
		assertEquals(BUY_EXP_1_FP, splt3.getQuantity());
		assertEquals(BUY_EXP_1_BF, splt3.getQuantityRat());
		// .
		assertEquals(BUY_EXP_1_FP, splt3.getValue());
		assertEquals(BUY_EXP_1_BF, splt3.getValueRat());
		// .
		assertEquals("", splt3.getDescription());
	}

	@Test
	public void test02() throws Exception {
		test02_initExpAccts();

		GnuCashWritableStockDividendTransaction trx = 
				SecuritiesAccountTransactionManager_FP
					.genDividDistribTrx(gcshInFile, 
									STOCK_ACCT_ID, INCOME_ACCT_ID, EXPENSES_ACCT_AMT_FP_LIST, OFFSET_ACCT_ID,
									GnuCashTransactionSplit.Action.DIVIDEND, DIV_GROSS_FP, 
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

		assertEquals(DIV_GROSS_FP,  specTrxRO.getGrossDividend());
		assertEquals(DIV_GROSS_BF,  specTrxRO.getGrossDividendRat());
		// .
		assertEquals(DIV_FEETAX_FP, specTrxRO.getFeesTaxes());
		assertEquals(DIV_FEETAX_BF, specTrxRO.getFeesTaxesRat());
		// .
		assertEquals(DIV_EXP_1_FP,  specTrxRO.getFeeTax(DIV_EXP_ACCT_1_ID));
		assertEquals(DIV_EXP_1_BF,  specTrxRO.getFeeTaxRat(DIV_EXP_ACCT_1_ID));
		// .
		assertEquals(DIV_EXP_2_FP,  specTrxRO.getFeeTax(DIV_EXP_ACCT_2_ID));
		assertEquals(DIV_EXP_2_BF,  specTrxRO.getFeeTaxRat(DIV_EXP_ACCT_2_ID));
		// .
		assertEquals(DIV_NET_FP,    specTrxRO.getNetDividend());
		assertEquals(DIV_NET_BF,    specTrxRO.getNetDividendRat());
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
    	BigFraction      feeTaxBF = BigFraction.ZERO;
    	for ( AcctIDAmountFPPair elt : EXPENSES_ACCT_AMT_FP_LIST ) {
    	    feeTaxFP.add(elt.amount()); // mutable
    	}
    	for ( AcctIDAmountBFPair elt : EXPENSES_ACCT_AMT_BF_LIST ) {
    	    feeTaxBF = feeTaxBF.add(elt.amount()); // immutable
    	}
    	FixedPointNumber divNetFP = DIV_GROSS_FP.copy().subtract(feeTaxFP);
    	BigFraction      divNetBF = DIV_GROSS_BF.subtract(feeTaxBF);
		
		assertEquals(DIV_FEETAX_FP, feeTaxFP);
		assertEquals(DIV_FEETAX_BF, feeTaxBF);
		// .
		assertEquals(DIV_NET_FP, divNetFP);
		assertEquals(DIV_NET_BF, divNetBF);
		
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
		// .
		assertEquals(DIV_NET_FP, splt2.getQuantity());
		assertEquals(DIV_NET_BF, splt2.getQuantityRat());
		// .
		assertEquals(DIV_NET_FP, splt2.getValue());
		assertEquals(DIV_NET_BF, splt2.getValueRat());
		// .
		assertEquals("", splt2.getDescription());

		assertEquals(INCOME_ACCT_ID, splt3.getAccountID());
		assertEquals(null, splt3.getAction());
		// .
		assertEquals(DIV_GROSS_FP.copy().negate(), splt3.getQuantity());
		assertEquals(DIV_GROSS_BF.negate(),        splt3.getQuantityRat());
		// .
		assertEquals(DIV_GROSS_FP.copy().negate(), splt3.getValue());
		assertEquals(DIV_GROSS_BF.negate(),        splt3.getValueRat());
		// .
		assertEquals("", splt3.getDescription());

		assertEquals(DIV_EXP_ACCT_1_ID, splt4.getAccountID());
		assertEquals(null, splt4.getAction());
		// .
		assertEquals(DIV_EXP_1_FP, splt4.getQuantity());
		assertEquals(DIV_EXP_1_BF, splt4.getQuantityRat());
		// .
		assertEquals(DIV_EXP_1_FP, splt4.getValue());
		assertEquals(DIV_EXP_1_BF, splt4.getValueRat());
		// .
		assertEquals("", splt4.getDescription());

		assertEquals(DIV_EXP_ACCT_2_ID, splt5.getAccountID());
		assertEquals(null, splt5.getAction());
		// .
		assertEquals(DIV_EXP_2_FP, splt5.getQuantity());
		assertEquals(DIV_EXP_2_BF, splt5.getQuantityRat());
		// .
		assertEquals(DIV_EXP_2_FP, splt5.getValue());
		assertEquals(DIV_EXP_2_BF, splt5.getValueRat());

		// .
		assertEquals("", splt5.getDescription());
	}

	@Test
	public void test03_1() throws Exception {
		test03_initExpAccts();

		GnuCashAccount stockAcct = gcshInFile.getAccountByID(STOCK_ACCT_ID);
		assertEquals(SPLT_NOF_SHR_BEFORE_FP, stockAcct.getBalance());
		assertEquals(SPLT_NOF_SHR_BEFORE_BF, stockAcct.getBalanceRat());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_FP
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_FP.StockSplitVar.FACTOR, SPLT_FACTOR_FP, 
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
		assertEquals(SPLT_NOF_SHR_BEFORE_FP, stockAcct.getBalance());
		assertEquals(SPLT_NOF_SHR_BEFORE_BF, stockAcct.getBalanceRat());
		
		GnuCashWritableStockSplitTransaction trx = 
				SecuritiesAccountTransactionManager_FP
					.genStockSplitTrx(gcshInFile, 
									STOCK_ACCT_ID,
									SecuritiesAccountTransactionManager_FP.StockSplitVar.NOF_ADD_SHARES, SPLT_NOF_ADD_FP, 
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

		assertEquals(SPLT_NOF_SHR_BEFORE_FP, specTrxRO.getNofSharesBeforeSplit());
		assertEquals(SPLT_NOF_SHR_BEFORE_BF, specTrxRO.getNofSharesBeforeSplitRat());
		assertEquals(SPLT_NOF_SHR_AFTER_FP,  specTrxRO.getNofSharesAfterSplit());
		assertEquals(SPLT_NOF_SHR_AFTER_BF,  specTrxRO.getNofSharesAfterSplitRat());
		// .
		assertEquals(SPLT_NOF_ADD_FP,        specTrxRO.getNofAddShares());
		assertEquals(SPLT_NOF_ADD_BF,        specTrxRO.getNofAddSharesRat());
		// .
		assertEquals(SPLT_FACTOR_FP,         specTrxRO.getSplitFactor());
		assertEquals(SPLT_FACTOR_BF,         specTrxRO.getSplitFactorRat());
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
		// .
		assertEquals(SPLT_NOF_ADD_FP, splt1.getQuantity());
		assertEquals(SPLT_NOF_ADD_BF, splt1.getQuantityRat());
		assertEquals(SPLT_NOF_SHR_AFTER_FP, splt1.getAccount().getBalance());
		assertEquals(SPLT_NOF_SHR_AFTER_BF, splt1.getAccount().getBalanceRat());
		// .
		assertEquals(0.0, splt1.getValue().doubleValue(), ConstTest.DIFF_TOLERANCE);
		assertEquals(0.0, splt1.getValueRat().doubleValue(), ConstTest.DIFF_TOLERANCE);
		// .
		assertEquals(true, splt1.getDescription().startsWith("Generated by SecuritiesAccountTransactionManager"));
	}

	// ---------------------------------------------------------------
	
	// 
	private void test01_initExpAccts() {
		EXPENSES_ACCT_AMT_FP_LIST.clear();
		EXPENSES_ACCT_AMT_BF_LIST.clear();
		
		AcctIDAmountFPPair acctAmtPr1FP = new AcctIDAmountFPPair(BUY_EXP_ACCT_1_ID, BUY_EXP_1_FP);
		AcctIDAmountBFPair acctAmtPr1BF = new AcctIDAmountBFPair(BUY_EXP_ACCT_1_ID, BUY_EXP_1_BF);
		EXPENSES_ACCT_AMT_FP_LIST.add(acctAmtPr1FP);
		EXPENSES_ACCT_AMT_BF_LIST.add(acctAmtPr1BF);
	}

	// Example for a dividend payment in Germany (domestic share).
	// If we had a foreign share (e.g. US), we would have to add a 
	// third entry to the list: "Auslaend. Quellensteuer" (that 
	// account is not in the test file yet).
	private void test02_initExpAccts() {
		EXPENSES_ACCT_AMT_FP_LIST.clear();
		EXPENSES_ACCT_AMT_BF_LIST.clear();
		
		AcctIDAmountFPPair acctAmtPr1FP = new AcctIDAmountFPPair(DIV_EXP_ACCT_1_ID, DIV_EXP_1_FP);
		AcctIDAmountBFPair acctAmtPr1BF = new AcctIDAmountBFPair(DIV_EXP_ACCT_1_ID, DIV_EXP_1_BF);
		EXPENSES_ACCT_AMT_FP_LIST.add(acctAmtPr1FP);
		EXPENSES_ACCT_AMT_BF_LIST.add(acctAmtPr1BF);
		
		AcctIDAmountFPPair acctAmtPr2FP = new AcctIDAmountFPPair(DIV_EXP_ACCT_2_ID, DIV_EXP_2_FP);
		AcctIDAmountBFPair acctAmtPr2BF = new AcctIDAmountBFPair(DIV_EXP_ACCT_2_ID, DIV_EXP_2_BF);
		EXPENSES_ACCT_AMT_FP_LIST.add(acctAmtPr2FP);
		EXPENSES_ACCT_AMT_BF_LIST.add(acctAmtPr2BF);
	}

	private void test03_initExpAccts() {
		EXPENSES_ACCT_AMT_FP_LIST.clear();
		EXPENSES_ACCT_AMT_BF_LIST.clear();
	}

}
