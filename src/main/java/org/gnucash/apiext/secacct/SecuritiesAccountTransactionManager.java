package org.gnucash.apiext.secacct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.write.GnuCashWritableTransaction;
import org.gnucash.api.write.GnuCashWritableTransactionSplit;
import org.gnucash.api.write.impl.GnuCashWritableFileImpl;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.gnucash.base.basetypes.simple.GCshID;
import org.gnucash.base.tuples.AcctIDAmountPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

/**
 * Collection of simplified, high-level access functions to a GnuCash file for
 * managing securities accounts (brokerage accounts).
 * <br>
 * These methods are sort of "macros" for the low-level access functions
 * in the "API" module.
 */
public class SecuritiesAccountTransactionManager {
    
    public enum Type {
    	BUY_STOCK,
    	DIVIDEND,
    	DISTRIBUTION,
    	STOCK_SPLIT
    }
    
    public enum StockSplitVar {
    	FACTOR,
    	NOF_ADD_SHARES
    }
    
    // ---------------------------------------------------------------
    
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritiesAccountTransactionManager.class);
    
    // ----------------------------

    // ::TODO These numbers should be extracted into a config. file. 
    // ::MAGIC
    private static FixedPointNumber SPLIT_FACTOR_MIN = new FixedPointNumber("1/20"); 
    	// anything below that value is technically OK,
        // but unplausible and thus forbidden.
    private static FixedPointNumber SPLIT_FACTOR_MAX = new FixedPointNumber("20");
    	// accordingly
    
    // Notes: 
    //  - It is common to specify stock (reverse) splits by a factor (e.g., 2 for a 2-for-1 split,
    //    or 1/4 for 1-for-4 reverse split). So why use the number of add. shares? Because that
    //    is how GnuCash handles things, as opposed to KMyMoney (cf. the sister project) , both 
    //    on the data and the GUI level, and given that we want to have both projects as symmetrical 
    //    as possible, we copy that logic here, so that the user can choose between both methods.
    //    Besides, the author has witnessed cases where the bank's statements provide wrong 
    //    values for the factor (yes, a bank's software also has bugs), whereas the number of add. 
    //    shares is practically always correct, given the usual bank-internal processes which
    //    the author happens to know a thing or two about.
    //  - As opposed to the factor above, a plausible range for the (abs.) number of additional 
    //    (to be subtracted) shares cannot as generally be specified. 
    //    E.g., European/US stocks tend to be priced above 1 EUR/USD, else they are considered penny 
    //    stocks (both literally and figuratively) and thus deemed uninvestable for the average Joe, 
    //    whereas in Singapore, e.g., it is deemed absolutely normal for a stock to be priced by just 
    //    a few cents or even less. Conversely, it is not uncommon for Japanese stocks to be priced
    //    very highly by European/US standards. Thus, the number of shares in a typical retail portfolio 
    //    will vary accordingly.
    //    Moreover, we of course know absolutely nothing about the entity/the individual that/who 
    //    will use this lib. A "regular" individual investor might have, say, 100 to 500 or so shares of 
    //    a European/US stock in his/her portfolio (and possibly 50-times that number of shares of a 
    //    Singaporean stock, and maybe just one single share of a Japanese stock), whereas a wealthy 
    //    individual might have 100-times as much or even more (never mind institutional investors, but 
    //    these entities will very probably use different software...)
    //    ==> ::TODO These numbers *must* be extracted into a config. file ASAP, whereas the above 
    //    factor *should* (but in fact can wait a little). 
    // ::MAGIC
    private static FixedPointNumber SPLIT_NOF_ADD_SHARES_MIN = new FixedPointNumber("1");
    private static FixedPointNumber SPLIT_NOF_ADD_SHARES_MAX = new FixedPointNumber("99999");

    // ---------------------------------------------------------------
    
    /**
     * Generates a transaction that buys a given number of stocks  
     * for a specific security's stock account at a given price, 
     * and generates additional splits for taxes/fees
     * (simple variant).
     * 
     * @param gcshFile KMyMoney file
     * @param stockAcctID ID the the stock account
     * @param taxFeeAcctID ID of the expenses account for the taxes/fees
     * @param offsetAcctID ID of the offsetting account
     * (the account that the gross amount will be debited to).
     * @param nofStocks no. of stocks bought
     * @param stockPrc stock price (net)
     * @param taxesFees taxes/fees
     * @param postDate post date for transaction
     * @param descr description of the transaction
     * @return a newly generated, modifiable transaction object
     * 
     * @see #genBuyStockTrx(GnuCashWritableFileImpl, GCshID, Collection, GCshID, FixedPointNumber, FixedPointNumber, LocalDate, String)
     */
    public static GnuCashWritableTransaction genBuyStockTrx(
    		final GnuCashWritableFileImpl gcshFile,
    		final GCshAcctID stockAcctID,
    		final GCshAcctID taxFeeAcctID,
    		final GCshAcctID offsetAcctID,
    		final FixedPointNumber nofStocks,
    		final FixedPointNumber stockPrc,
    		final FixedPointNumber taxesFees,
    		final LocalDate postDate,
    		final String descr) {
    	Collection<AcctIDAmountPair> expensesAcctAmtList = new ArrayList<AcctIDAmountPair>();
	
    	AcctIDAmountPair newPair = new AcctIDAmountPair(taxFeeAcctID, taxesFees);
    	expensesAcctAmtList.add(newPair);
    	
    	return genBuyStockTrx(gcshFile, 
    			      stockAcctID, expensesAcctAmtList, offsetAcctID, 
    			      nofStocks, stockPrc, 
    			      postDate, descr);	
    }
    
    /**
     * Generates a transaction that buys a given number of stocks
     * for a specific security's stock account at a given price, 
     * and generates additional splits for taxes/fees
     * (general variant).
     * 
     * @param gcshFile KMyMoney file
     * @param stockAcctID ID the the stock account
     * @param expensesAcctAmtList list of pairs (acctID/amount)
     * that represents all taxes / fees for this transaction
     * (the account-IDs being the IDs of the according expenses
     * accounts)  
     * @param offsetAcctID ID of the offsetting account
     * (the account that the gross amount will be debited to).
     * @param nofStocks no. of stocks bought
     * @param stockPrc stock price (net)
     * @param postDate post date for transaction
     * @param descr description of the transaction
     * @return a newly generated, modifiable transaction object
     * 
     * @see #genBuyStockTrx(GnuCashWritableFileImpl, GCshAcctID, GCshAcctID, GCshAcctID, FixedPointNumber, FixedPointNumber, FixedPointNumber, LocalDate, String)
     */
    public static GnuCashWritableTransaction genBuyStockTrx(
    		final GnuCashWritableFileImpl gcshFile,
    		final GCshAcctID stockAcctID,
    		final Collection<AcctIDAmountPair> expensesAcctAmtList,
    		final GCshAcctID offsetAcctID,
    		final FixedPointNumber nofStocks,
    		final FixedPointNumber stockPrc,
    		final LocalDate postDate,
    		final String descr) {
    	if ( gcshFile == null ) {
    		throw new IllegalArgumentException("null GnuCash file given");
    	}
		
    	if ( stockAcctID == null ||
    		 offsetAcctID == null ) {
    		throw new IllegalArgumentException("null account ID given");
    	}
	
    	if ( ! ( stockAcctID.isSet()  ) ||
    		 ! ( offsetAcctID.isSet() ) ) {
    		throw new IllegalArgumentException("unset account ID given");
    	}
		
    	if ( expensesAcctAmtList == null ) {
    		throw new IllegalArgumentException("null expenses account list given");
    	}
			
    	if ( expensesAcctAmtList.isEmpty() ) {
    		throw new IllegalArgumentException("empty expenses account list given");
    	}
			
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		if ( ! elt.isNotNull() ) {
    			throw new IllegalArgumentException("null expenses account list element given");
    		}
    		if ( ! elt.isSet() ) {
    			throw new IllegalArgumentException("unset expenses account list element given");
    		}
    	}

    	if ( nofStocks == null ||
    		 stockPrc == null ) {
    		throw new IllegalArgumentException("null amount given");
    	}
		
    	if ( nofStocks.doubleValue() <= 0.0 ) {
    		throw new IllegalArgumentException("number of stocks <= 0.0 given");
    	}
			
    	if ( stockPrc.doubleValue() <= 0.0 ) {
    		throw new IllegalArgumentException("stock price <= 0.0 given");
    	}
	
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		if ( elt.amount().doubleValue() <= 0.0 ) {
    			throw new IllegalArgumentException("expense <= 0.0 given");
    		}
    	}

    	LOGGER.debug("genBuyStockTrx: Account 1 name (stock):      '" + gcshFile.getAccountByID(stockAcctID).getQualifiedName() + "'");
    	int counter = 1;
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		LOGGER.debug("genBuyStockTrx: Account 2." + counter + " name (expenses): '" + gcshFile.getAccountByID(elt.accountID()).getQualifiedName() + "'");
    		counter++;
    	}
    	LOGGER.debug("genBuyStockTrx: Account 3 name (offsetting): '" + gcshFile.getAccountByID(offsetAcctID).getQualifiedName() + "'");

    	// ---
    	// Check account types
    	GnuCashAccount stockAcct  = gcshFile.getAccountByID(stockAcctID);
    	if ( stockAcct.getType() != GnuCashAccount.Type.STOCK ) {
    		throw new IllegalArgumentException("Account with ID " + stockAcctID + " is not of type " + GnuCashAccount.Type.STOCK);
    	}

    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		GnuCashAccount expensesAcct = gcshFile.getAccountByID(elt.accountID());
    		if ( expensesAcct.getType() != GnuCashAccount.Type.EXPENSE ) {
    			throw new IllegalArgumentException("Account with ID " + elt.accountID() + " is not of type " + GnuCashAccount.Type.EXPENSE);
    		}
    	}

    	GnuCashAccount offsetAcct = gcshFile.getAccountByID(offsetAcctID);
    	if ( offsetAcct.getType() != GnuCashAccount.Type.BANK ) {
    		throw new IllegalArgumentException("Account with ID " + offsetAcctID + " is not of type " + GnuCashAccount.Type.BANK);
    	}

    	// ---

    	FixedPointNumber amtNet   = nofStocks.copy().multiply(stockPrc);
    	LOGGER.debug("genBuyStockTrx: Net amount: " + amtNet);

    	FixedPointNumber amtGross = amtNet.copy();
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		amtGross.add(elt.amount());
    	}
    	LOGGER.debug("genBuyStockTrx: Gross amount: " + amtGross);

    	// ---

    	GnuCashWritableTransaction trx = gcshFile.createWritableTransaction();
    	trx.setDescription(descr);

    	// ---

    	GnuCashWritableTransactionSplit splt1 = trx.createWritableSplit(offsetAcct);
    	splt1.setValue(amtGross.copy().negate());
    	splt1.setQuantity(amtGross.copy().negate());
    	LOGGER.debug("genBuyStockTrx: Split 1 to write: " + splt1.toString());

    	// ---
	
    	GnuCashWritableTransactionSplit splt2 = trx.createWritableSplit(stockAcct);
    	splt2.setValue(amtNet);
    	splt2.setQuantity(nofStocks);
    	splt2.setAction(GnuCashTransactionSplit.Action.BUY);
    	LOGGER.debug("genBuyStockTrx: Split 2 to write: " + splt2.toString());

    	// ---

    	counter = 1;
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		GnuCashAccount expensesAcct = gcshFile.getAccountByID(elt.accountID());
    		GnuCashWritableTransactionSplit splt3 = trx.createWritableSplit(expensesAcct);
    		splt3.setValue(elt.amount());
    		splt3.setQuantity(elt.amount());
    		LOGGER.debug("genBuyStockTrx: Split 3." + counter + " to write: " + splt3.toString());
    		counter++;
    	}

    	// ---

    	trx.setDatePosted(postDate);
    	trx.setDateEntered(LocalDateTime.now());

    	// ---

    	LOGGER.info("genBuyStockTrx: Generated new Transaction: " + trx.getID());
    	return trx;
    }
    
    // ---------------------------------------------------------------
    
    /**
     * Generates a transaction for a dividend or distribution
     * from a specific security's stock account, and generates additional 
     * splits for taxes/fees.
     * (simple variant).
     * 
     * @param gcshFile KMyMoney file
     * @param stockAcctID ID of the stock account
     * @param incomeAcctID ID of the income account
     * @param taxFeeAcctID ID of the expenses account for the taxes/fees
     * @param offsetAcctID ID of the offsetting account (the one that
     * the net amount will be credited to)
     * @param spltAct action type of the split that will point to the 
     * stock account (dividend or distribution)
     * @param divDistrGross gross dividend / distribution
     * @param taxesFees taxes/fees
     * @param postDate post date of the transaction
     * @param descr description of the transaction
     * @return a newly generated, modifiable transaction object
     */
    public static GnuCashWritableTransaction genDividDistribTrx(
    	    final GnuCashWritableFileImpl gcshFile,
    	    final GCshAcctID stockAcctID,
    	    final GCshAcctID incomeAcctID,
    	    final GCshAcctID taxFeeAcctID,
    	    final GCshAcctID offsetAcctID,
    	    final GnuCashTransactionSplit.Action spltAct,
    	    final FixedPointNumber divDistrGross,
    	    final FixedPointNumber taxesFees,
    	    final LocalDate postDate,
    	    final String descr) {
    	Collection<AcctIDAmountPair> expensesAcctAmtList = new ArrayList<AcctIDAmountPair>();
	
    	AcctIDAmountPair newPair = new AcctIDAmountPair(taxFeeAcctID, taxesFees);
    	expensesAcctAmtList.add(newPair);

    	return genDividDistribTrx(gcshFile,
    			      stockAcctID, incomeAcctID, expensesAcctAmtList, offsetAcctID, 
    			      spltAct, divDistrGross,
    			      postDate, descr);
    }
    
    /**
     * Generates a transaction for a dividend or distribution
     * from a specific security's stock account, and generates additional 
     * splits for taxes/fees.
     * (general variant).
     * 
     * @param gcshFile KMyMoney file
     * @param stockAcctID ID of the stock account
     * @param incomeAcctID ID of the income account
     * @param expensesAcctAmtList list of pairs (acctID/amount) 
     * that represents all taxes / fees for this transaction
     * (the account-IDs being the IDs of the according expenses
     * accounts)  
     * @param offsetAcctID ID of the offsetting account (the one that
     * the net amount will be credited to)
     * @param spltAct action type of the split that will point to the 
     * stock account (dividend or distribution)
     * @param divDistrGross gross dividend / distribution
     * @param postDate post date of the transaction
     * @param descr description of the transaction
     * @return a newly generated, modifiable transaction object
     */
    public static GnuCashWritableTransaction genDividDistribTrx(
    	    final GnuCashWritableFileImpl gcshFile,
    	    final GCshAcctID stockAcctID,
    	    final GCshAcctID incomeAcctID,
    	    final Collection<AcctIDAmountPair> expensesAcctAmtList,
    	    final GCshAcctID offsetAcctID,
    	    final GnuCashTransactionSplit.Action spltAct,
    	    final FixedPointNumber divDistrGross,
    	    final LocalDate postDate,
    	    final String descr) {
    	if ( gcshFile == null ) {
    	    throw new IllegalArgumentException("null GnuCash file given");
    	}

    	if ( stockAcctID == null ||
    	     incomeAcctID == null ||
    	     offsetAcctID == null ) {
    	    throw new IllegalArgumentException("null account ID given");
    	}

    	if ( ! ( stockAcctID.isSet() ) ||
    	     ! ( incomeAcctID.isSet() ) ||
    	     ! ( offsetAcctID.isSet() ) ) {
    	    throw new IllegalArgumentException("unset account ID given");
    	}

    	if ( expensesAcctAmtList == null ) {
    	    throw new IllegalArgumentException("null expenses account list given");
    	}

    	// CAUTION: Yes, this actually happens in real life, e.g. with specifics 
    	// of German tax law (Freibetrag, Kapitalausschuettung).
    	// ==> The following check is commented out on purpose.
//    	if ( expensesAcctAmtList.isEmpty() ) {
//    	    throw new IllegalArgumentException("empty expenses account list given");
//    	}
    			
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    	    if ( ! elt.isNotNull() ) {
    		throw new IllegalArgumentException("null expenses account list element given");
    	    }
    	    if ( ! elt.isSet() ) {
    		throw new IllegalArgumentException("unset expenses account list element given");
    	    }
    	}

    	if ( divDistrGross == null ) {
    	    throw new IllegalArgumentException("null gross dividend given");
    	}

    	// CAUTION: The following two: In fact, this can happen
    	// (negative booking after cancellation / Stornobuchung)
//    	if ( divGross.doubleValue() <= 0.0 ) {
//    	    throw new IllegalArgumentException("gross dividend <= 0.0 given");
//    	}
//    				
//    	if ( taxes.doubleValue() <= 0.0 ) {
//    	    throw new IllegalArgumentException("taxes <= 0.0 given");
//    	}
    				
    	LOGGER.debug("genDividDistribTrx: Account 1 name (stock):      '" + gcshFile.getAccountByID(stockAcctID).getQualifiedName() + "'");
    	LOGGER.debug("genDividDistribTrx: Account 2 name (income):     '" + gcshFile.getAccountByID(incomeAcctID).getQualifiedName() + "'");
    	int counter = 1;
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    	    LOGGER.debug("genDividDistribTrx: Account 3." + counter + " name (expenses): '" + gcshFile.getAccountByID(elt.accountID()).getQualifiedName() + "'");
    	    counter++;
    	}
    	LOGGER.debug("genDividDistribTrx: Account 4 name (offsetting): '" + gcshFile.getAccountByID(offsetAcctID).getQualifiedName() + "'");

    	// ---
    	// Check account types
    	GnuCashAccount stockAcct  = gcshFile.getAccountByID(stockAcctID);
    	if ( stockAcct.getType() != GnuCashAccount.Type.STOCK ) {
    	    throw new IllegalArgumentException("Account with ID " + stockAcctID + " is not of type " + GnuCashAccount.Type.STOCK);
    	}

    	GnuCashAccount incomeAcct = gcshFile.getAccountByID(incomeAcctID);
    	if ( incomeAcct.getType() != GnuCashAccount.Type.INCOME ) {
    	    throw new IllegalArgumentException("Account with ID " + incomeAcct + " is not of type " + GnuCashAccount.Type.INCOME);
    	}

    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    		GnuCashAccount expensesAcct = gcshFile.getAccountByID(elt.accountID());
    	    if ( expensesAcct.getType() != GnuCashAccount.Type.EXPENSE ) {
    		throw new IllegalArgumentException("Account with ID " + elt.accountID() + " is not of type " + GnuCashAccount.Type.EXPENSE);
    	    }
    	}
    	
    	GnuCashAccount offsetAcct = gcshFile.getAccountByID(offsetAcctID);
    	if ( offsetAcct.getType() != GnuCashAccount.Type.BANK ) {
    	    throw new IllegalArgumentException("Account with ID " + offsetAcctID + " is not of type " + GnuCashAccount.Type.BANK);
    	}

    	// ---

    	FixedPointNumber expensesSum = new FixedPointNumber();
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    	    expensesSum.add(elt.amount());
    	}
    	LOGGER.debug("genDividDistribTrx: Sum of all expenses: " + expensesSum);

    	FixedPointNumber divDistrNet = divDistrGross.copy().subtract(expensesSum);
    	LOGGER.debug("genDividDistribTrx: Net dividend: " + divDistrNet);

    	// ---

    	GnuCashWritableTransaction trx = gcshFile.createWritableTransaction();
    	trx.setDescription(descr);

    	// ---
    	
    	GnuCashWritableTransactionSplit splt1 = trx.createWritableSplit(stockAcct);
    	splt1.setValue(new FixedPointNumber());
    	splt1.setQuantity(new FixedPointNumber());
    	splt1.setAction(spltAct);
    	LOGGER.debug("genDividDistribTrx: Split 1 to write: " + splt1.toString());

    	// ---

    	GnuCashWritableTransactionSplit splt2 = trx.createWritableSplit(offsetAcct);
    	splt2.setValue(divDistrNet);
    	splt2.setQuantity(divDistrNet);
    	LOGGER.debug("genDividDistribTrx: Split 2 to write: " + splt2.toString());

    	// ---

    	GnuCashWritableTransactionSplit splt3 = trx.createWritableSplit(incomeAcct);
    	splt3.setValue(divDistrGross.copy().negate());
    	splt3.setQuantity(divDistrGross.copy().negate());
    	LOGGER.debug("genDividDistribTrx: Split 3 to write: " + splt3.toString());

    	// ---

    	counter = 1;
    	for ( AcctIDAmountPair elt : expensesAcctAmtList ) {
    	    GnuCashAccount expensesAcct = gcshFile.getAccountByID(elt.accountID());
    	    GnuCashWritableTransactionSplit splt4 = trx.createWritableSplit(expensesAcct);
    	    splt4.setValue(elt.amount());
    	    splt4.setQuantity(elt.amount());
    	    LOGGER.debug("genDividDistribTrx: Split 4." + counter + " to write: " + splt4.toString());
    	    counter++;
    	}

    	// ---

    	trx.setDatePosted(postDate);
    	trx.setDateEntered(LocalDateTime.now());

    	// ---

    	LOGGER.info("genDividDistribTrx: Generated new Transaction: " + trx.getID());
    	return trx;
    }

    // ---------------------------------------------------------------
    
    public static GnuCashWritableTransaction genStockSplitTrx(
    		final GnuCashWritableFileImpl gcshFile,
    		final GCshAcctID stockAcctID,
    		final StockSplitVar var,
    		final FixedPointNumber factorOfNofAddShares,
    		final LocalDate postDate,
    		final String descr) {
    	if ( var == StockSplitVar.FACTOR ) {
    		return genStockSplitTrx_factor(gcshFile, 
    									   stockAcctID, factorOfNofAddShares, 
    									   postDate, descr);
    	} else if ( var == StockSplitVar.NOF_ADD_SHARES ) {
    		return genStockSplitTrx_nofShares(gcshFile,
    										  stockAcctID, factorOfNofAddShares,
    										  postDate, descr);
    	}

    	return null; // Compiler happy
    }
    
    /**
     * 
     * @param gcshFile
     * @param stockAcctID
     * @param factor E.g., the number 3.0 for a 3-for-1 split (a threefold increase of the number of shares), 
     * or the number 1/3 (0.333...) for a 1-for-3 reverse stock-split (the number of shares is decreased to a third).
     * 
     * <em>Caution:</em> The wording is not standardized, at least not internationally: 
     * In english-speaking countries, people tend to say "3-for-1" ("3 new shares for 1 old share") 
     * when they mean a threefold-increase of the stocks, whereas in Germany, e.g., it tends
     * to be the other way round, i.e. "Aktiensplit 1:4" ("eine alte zu 4 neuen Aktien") is a 
     * "4-for-1" split).
     * 
     * Also, please be aware that GnuCash does not use the factor-logic, neither internally
     * nor on the GUI, but instead only shows and stores the number of additional shares.
     * @param postDate
     * @param descr
     * @return a new share-(reverse-)split transaction
     * 
     * @see #genStockSplitTrx_nofShares(GnuCashWritableFileImpl, GCshAcctID, FixedPointNumber, LocalDate, String)
     * @see #genStockSplitTrx(GnuCashWritableFileImpl, GCshAcctID, StockSplitVar, FixedPointNumber, LocalDate, String)
     */
    public static GnuCashWritableTransaction genStockSplitTrx_factor(
    		final GnuCashWritableFileImpl gcshFile,
    		final GCshAcctID stockAcctID,
    		final FixedPointNumber factor,
    		final LocalDate postDate,
    		final String descr) {
    	if ( gcshFile == null ) {
    		throw new IllegalArgumentException("null GnuCash file given");
    	}
		
    	if ( stockAcctID == null  ) {
    		throw new IllegalArgumentException("null stock account ID given");
    	}
	
    	if ( ! ( stockAcctID.isSet() ) ) {
    		throw new IllegalArgumentException("unset stock account ID given");
    	}
		
    	if ( factor == null ) {
    		throw new IllegalArgumentException("null factor given");
    	}

    	if ( factor.isNegative() ) {
    		throw new IllegalArgumentException("negative factor given");
    	}

    	if ( factor.getBigDecimal().equals(BigDecimal.ZERO) ) {
    		throw new IllegalArgumentException("zero-value factor given");
    	}

    	// ::TODO: Reconsider: Should we really reject the input and throw an exception 
    	// (which is kind of overly strict), or shouldn't we rather just issue a warning?
    	if ( factor.isLessThan(SPLIT_FACTOR_MIN) ) {
    		throw new IllegalArgumentException("unplausible factor given (smaller than " + SPLIT_FACTOR_MIN + ")");
    	}

    	// ::TODO: cf. above
    	if ( factor.isGreaterThan(SPLIT_FACTOR_MAX) ) {
    		throw new IllegalArgumentException("unplausible factor given (greater than " + SPLIT_FACTOR_MAX + ")");
    	}

    	// ---
    	// Check account type
    	GnuCashAccount stockAcct  = gcshFile.getAccountByID(stockAcctID);
    	if ( stockAcct == null ) {
    		throw new IllegalStateException("Could not find account with that ID");
    	}

    	LOGGER.debug("genStockSplitTrx_factor: Stock account name: '" + stockAcct.getQualifiedName() + "'");
    	if ( stockAcct.getType() != GnuCashAccount.Type.STOCK ) {
    		throw new IllegalArgumentException("Account with ID " + stockAcctID + " is not of type " + GnuCashAccount.Type.STOCK);
    	}

    	// ---
    	
    	FixedPointNumber nofSharesOld = stockAcct.getBalance();
    	LOGGER.debug("genStockSplitTrx_factor: Old no. of shares: " + nofSharesOld);
    	if ( nofSharesOld.equals(BigDecimal.ZERO) ) {
    		throw new IllegalStateException("No. of old shares is zero. Cannot carry out a split.");
    	}
    	FixedPointNumber nofSharesNew = nofSharesOld.copy().multiply(factor);
    	LOGGER.debug("genStockSplitTrx_factor: New no. of shares: " + nofSharesNew);
    	FixedPointNumber nofAddShares = nofSharesNew.copy().subtract(nofSharesOld);
    	LOGGER.debug("genStockSplitTrx_factor: No. of add. shares: " + nofAddShares);
    	
    	// ---

    	return genStockSplitTrx_nofShares(gcshFile,
    									  stockAcctID, nofAddShares,
    									  postDate, descr);
    }
    
    /**
     * 
     * @param gcshFile
     * @param stockAcctID
     * @param nofAddShares The number of additional shares to be added to the stock account.
     * E.g., when you have 100 shares and you add 200 more, then you have 300 shares, 
     * i.e. the number has increased by a factor of 3 (a 3-for-1 split).
     * Likewise, if you have 100 shares and you take away 75 of them (neg. no. of add. shares),
     * then you have 25 shares left, i.e. the number of shares as decreased by a factor
     * of 1/4 (1-for-4). 
     * 
     * Also, please be aware that GnuCash does not use the factor-logic, neither internally
     * nor on the GUI, but instead only shows and stores the number of additional shares.
     * @param postDate
     * @param descr
     * @return a new share-(reverse-)split transaction
     * 
     * @see #genStockSplitTrx_factor(GnuCashWritableFileImpl, GCshAcctID, FixedPointNumber, LocalDate, String)
     * @see #genStockSplitTrx(GnuCashWritableFileImpl, GCshAcctID, StockSplitVar, FixedPointNumber, LocalDate, String)
     */
    public static GnuCashWritableTransaction genStockSplitTrx_nofShares(
    	    final GnuCashWritableFileImpl gcshFile,
    	    final GCshAcctID stockAcctID,
    	    final FixedPointNumber nofAddShares, // use neg. number in case of reverse stock-split
    	    final LocalDate postDate,
    	    final String descr) {
    	if ( gcshFile == null ) {
    		throw new IllegalArgumentException("null GnuCash file given");
    	}
		
    	if ( stockAcctID == null  ) {
    		throw new IllegalArgumentException("null stock account ID given");
    	}
	
    	if ( ! ( stockAcctID.isSet() ) ) {
    		throw new IllegalArgumentException("unset stock account ID given");
    	}
		
    	if ( nofAddShares == null ) {
    		throw new IllegalArgumentException("null no. of add. shares given");
    	}

    	// CAUTION: Neg. no. of add. shares is allowed!
//    	if ( nofAddShares.isNegative() ) {
//    		throw new IllegalArgumentException("negative no. of add. shares given");
//    	}

    	if ( nofAddShares.getBigDecimal().equals(BigDecimal.ZERO) ) {
    		throw new IllegalArgumentException("zero-value no. of add. shares given");
    	}

    	FixedPointNumber nofAddSharesAbs = new FixedPointNumber( nofAddShares.copy().abs() );
    	
    	// ::TODO: Reconsider: Should we really reject the input and throw an exception 
    	// (which is kind of overly strict), or shouldn't we rather just issue a warning?
    	if ( nofAddSharesAbs.isLessThan(SPLIT_NOF_ADD_SHARES_MIN) ) {
    		throw new IllegalArgumentException("unplausible no. of add. shares given (abs. smaller than " + SPLIT_NOF_ADD_SHARES_MIN + ")");
    	}

    	// ::TODO: Cf. above
    	if ( nofAddSharesAbs.isGreaterThan(SPLIT_NOF_ADD_SHARES_MAX) ) {
    		throw new IllegalArgumentException("unplausible no. of add. shares (abs. greater than " + SPLIT_NOF_ADD_SHARES_MAX + ")");
    	}

    	// CAUTION: Yes, it actually *is* possible that the no. of add. shares
    	// is not an integer: If the old no. of shares is non-int as well (and yes,
    	// that can actually be the case, not just theoretically, but in practice!)
//    	// Check if no. of add. shares is integer
//    	// https://stackoverflow.com/questions/1078953/check-if-bigdecimal-is-an-integer-in-java
//    	if ( nofAddShares.stripTrailingZeros().scale() <= 0 ) {
//    		throw new IllegalArgumentException("no. of add. shares given is not integer value");
//    	}

    	// ---
    	// Check account type
    	GnuCashAccount stockAcct  = gcshFile.getAccountByID(stockAcctID);
    	if ( stockAcct == null ) {
    		throw new IllegalStateException("Could not find account with that ID");
    	}

    	LOGGER.debug("genStockSplitTrx_nofShares: Stock account name: '" + stockAcct.getQualifiedName() + "'");
    	if ( stockAcct.getType() != GnuCashAccount.Type.STOCK ) {
    		throw new IllegalArgumentException("Account with ID " + stockAcctID + " is not of type " + GnuCashAccount.Type.STOCK);
    	}

    	// ---
    	
    	FixedPointNumber nofSharesOld = stockAcct.getBalance();
    	LOGGER.debug("genStockSplitTrx_nofShares: Old no. of shares: " + nofSharesOld);
    	if ( nofSharesOld.equals(BigDecimal.ZERO) ) {
    		throw new IllegalStateException("No. of old shares is zero. Cannot carry out a split.");
    	}
    	FixedPointNumber nofSharesNew = nofSharesOld.copy().add(nofAddShares);
    	LOGGER.debug("genStockSplitTrx_nofShares: New no. of shares: " + nofSharesNew);
    	FixedPointNumber factor = nofSharesNew.copy().divide(nofSharesOld);
    	LOGGER.debug("genStockSplitTrx_nofShares: Factor: " + factor);
    	
    	// ---
    	
    	GnuCashWritableTransaction trx = gcshFile.createWritableTransaction();
    	trx.setDescription(descr);

    	// ---
    	
    	// ::TODO ::CHECK
    	// It seems that a slot also should be generated (by comparison
    	// with a share split transaction generated with the GUI).
    	// However, it also seems that it's optional.
    	
    	// CAUTION: One single split
		GnuCashWritableTransactionSplit splt = trx.createWritableSplit(stockAcct);
    	splt.setValue(new FixedPointNumber());
    	splt.setQuantity(new FixedPointNumber(nofAddShares));
    	splt.setAction(GnuCashTransactionSplit.Action.SPLIT);
    	splt.setDescription("Generated by SecuritiesAccountTransactionManager, " + LocalDateTime.now());
    	LOGGER.debug("genStockSplitTrx_nofShares: Split 1 to write: " + splt.toString());

    	// ---

    	trx.setDatePosted(postDate);
    	trx.setDateEntered(LocalDateTime.now());

    	// ---

    	LOGGER.info("genStockSplitTrx_factor: Generated new Transaction: " + trx.getID());
    	return trx;
    }
    
}
