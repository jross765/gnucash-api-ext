package org.gnucash.apiext.secacct;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.aux.GCshAcctLot;
import org.gnucash.apiext.Const;
import org.gnucash.base.basetypes.simple.GCshIDNotSetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

/**
 * Collection of simplified, high-level access functions to a GnuCash file for
 * managing buy/sell lots in a securities account (brokerage account).
 * <br>
 * These methods are sort of "macros" for the low-level access functions
 * in the "API" module.
 */
public class SecuritiesAccountLotManager {
    
    // ---------------------------------------------------------------
    
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritiesAccountLotManager.class);
    
    // ----------------------------
    
    // ::EMPTY

    // ---------------------------------------------------------------

    public static boolean areLotsOK(final GnuCashAccount acct) throws GCshIDNotSetException {
    	if ( acct == null ) {
    		throw new IllegalArgumentException("argument <acct> is null");
    	}

    	if ( acct.getType() != GnuCashAccount.Type.STOCK ) {
    		throw new IllegalArgumentException("given account is not of type '" + GnuCashAccount.Type.STOCK + "'");
    	}
    	
    	boolean result = true;
    	LOGGER.debug("No. of lots to check for account " + acct.getID() + ": " + acct.getLots().size()); 
    	for ( GCshAcctLot lot : acct.getLots() ) {
    		LOGGER.debug("Lot: ID " + lot.getID() + ", title: '" + lot.getTitle() + "'");
    		if ( isLotOK(lot) ) {
    			result = false;
    		}
    	}
    	
		LOGGER.warn("One or more lots of account " + acct.getID() + " are not OK");
    	return result;
    }

	public static boolean isLotOK(final GCshAcctLot lot) throws GCshIDNotSetException {
    	if ( lot == null ) {
    		throw new IllegalArgumentException("argument <lot> is null");
    	}

    	if ( lot.getTransactionSplits().size() == 0 ) {
			LOGGER.warn("Lot ID " + lot.getID() + ", title: '" + lot.getTitle() + "' does not contain transaction splits");
			return false;
    	}
    	
		FixedPointNumber spltSum = new FixedPointNumber("0");
		for ( GnuCashTransactionSplit splt : lot.getTransactionSplits() ) {
			LOGGER.debug("Split: ID " + splt.getID() + ", value: '" + splt.getValueFormatted() + "'");
			spltSum = splt.getValue();
		}
		
		if ( Math.abs( spltSum.getBigDecimal().doubleValue() ) <= Const.DIFF_TOLERANCE_VALUE ) {
			LOGGER.debug("Lot ID " + lot.getID() + ", title: '" + lot.getTitle() + "' is OK");
			return true;
		} else {
			LOGGER.warn("Lot ID " + lot.getID() + ", title: '" + lot.getTitle() + "' is not OK");
			return false;
		}
	}
        
}
