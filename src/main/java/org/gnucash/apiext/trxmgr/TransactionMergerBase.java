package org.gnucash.apiext.trxmgr;

import java.util.ArrayList;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.write.GnuCashWritableFile;
import org.gnucash.apiext.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.schnorxoborx.base.dateutils.JulianDate;
import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public abstract class TransactionMergerBase {
	
	public enum Var {
		VAR_1,
		VAR_2
	}
	
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMergerBase.class);
    
    // ---------------------------------------------------------------
    
	protected GnuCashWritableFile gcshFile = null;
	private   Var                 var      = null;
	
    // ---------------------------------------------------------------
	
	public TransactionMergerBase(GnuCashWritableFile gcshFile) {
		this.gcshFile = gcshFile;
	}
    
    // ---------------------------------------------------------------
	
	public Var getVar() {
		return var;
	}
	
	public void setVar(Var var) {
		this.var = var;
	}
	
    // ---------------------------------------------------------------
	
	public boolean plausiCheck(GnuCashTransaction survivor, GnuCashTransaction dier) {
		// Level 1:
		double survDateFromJul = 0.0;
		double dierDateToJul   = 0.0;
		try {
			survDateFromJul = JulianDate.toJulian(survivor.getDatePosted().toLocalDate());
			dierDateToJul   = JulianDate.toJulian(dier.getDatePosted().toLocalDate());
		} catch ( Exception exc ) {
			// pro forma
			exc.printStackTrace();
		}
		
		if ( Math.abs( survDateFromJul - dierDateToJul ) > Const.DIFF_TOLERANCE_DAYS ) {
			LOGGER.warn("plausiCheck: Survivor- and dier-transaction do not have the same post-date");
			LOGGER.debug("plausiCheck: Survivor-date: " + survivor.getDatePosted());
			LOGGER.debug("plausiCheck: Dier-date: " + dier.getDatePosted());
			return false;
		}

		TransactionManager trxMgr = new TransactionManager(gcshFile);
		if ( ! trxMgr.isSane(survivor) ) {
			LOGGER.warn("plausiCheck: Survivor-transaction is not sane");
			return false;
		}
		
		if ( ! trxMgr.isSane(dier) ) {
			LOGGER.warn("plausiCheck: Dier-transaction is not sane");
			return false;
		}
		
		if ( ! ( trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.BANK) &&
			     trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.BANK) 
			     ||
			     trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.CASH) &&
			     trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.CASH)
			     ||
			     trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.STOCK) &&
			     trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.STOCK) 
			   ) ) {
			LOGGER.warn("plausiCheck: One or both transactions has/have no split belonging to bank/cash/stock account");
			return false;
		}
		
		// Level 2:
		// Splits belong to the same accounts -- per account type
		ArrayList<GnuCashTransactionSplit> spltListSurvBank = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.BANK);
		ArrayList<GnuCashTransactionSplit> spltListDierBank = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.BANK);
		if ( trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.BANK) &&
			 trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.BANK) ) {
			spltListSurvBank = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.BANK);
			spltListDierBank = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.BANK);
			
			for ( GnuCashTransactionSplit spltSurv : spltListSurvBank ) {
				boolean accountInBothLists = false;
				
				for ( GnuCashTransactionSplit spltDier : spltListDierBank ) {
					if ( spltSurv.getAccount().getID().equals(spltDier.getAccount().getID() ) ) {
						accountInBothLists = true;
					}
				}
				
				if ( ! accountInBothLists ) {
					LOGGER.warn("plausiCheck: Survivor-split " + spltSurv.getID() + " has no according dier-split sibling (bank accounts)");
					return false;
				}
			}
		} 

		// sic, no else-if!
		ArrayList<GnuCashTransactionSplit> spltListSurvCash = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.CASH);
		ArrayList<GnuCashTransactionSplit> spltListDierCash = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.CASH);
		if ( trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.CASH) &&
			 trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.CASH) ) {
			spltListSurvCash = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.CASH);
			spltListDierCash = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.CASH);
			
			for ( GnuCashTransactionSplit spltSurv : spltListSurvCash ) {
				boolean accountInBothLists = false;
				
				for ( GnuCashTransactionSplit spltDier : spltListDierCash ) {
					if ( spltSurv.getAccount().getID().equals(spltDier.getAccount().getID() ) ) {
						accountInBothLists = true;
					}
				}
				
				if ( ! accountInBothLists ) {
					LOGGER.warn("plausiCheck: Survivor-split " + spltSurv.getID() + " has no according dier-split sibling (cash accounts)");
					return false;
				}
			}
		}
		
		// sic, no else-if!
		ArrayList<GnuCashTransactionSplit> spltListSurvStock = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.STOCK);
		ArrayList<GnuCashTransactionSplit> spltListDierStock = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.STOCK);
		if ( trxMgr.hasSplitBoundToAccounttType(survivor, GnuCashAccount.Type.STOCK) &&
			 trxMgr.hasSplitBoundToAccounttType(dier, GnuCashAccount.Type.STOCK) ) {
			spltListSurvStock = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.STOCK);
			spltListDierStock = trxMgr.getSplitsBoundToAccounttType(dier, GnuCashAccount.Type.STOCK);
			
			for ( GnuCashTransactionSplit spltSurv : spltListSurvStock ) {
				boolean accountInBothLists = false;
				
				for ( GnuCashTransactionSplit spltDier : spltListDierStock ) {
					if ( spltSurv.getAccount().getID().equals(spltDier.getAccount().getID() ) ) {
						accountInBothLists = true;
					}
				}
				
				if ( ! accountInBothLists ) {
					LOGGER.warn("plausiCheck: Survivor-split " + spltSurv.getID() + " has no according dier-split sibling (stock accounts)");
					return false;
				}
			}
		}
		
		// Level 3:
		// Split values are identical
		FixedPointNumber sumSurv = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListSurvBank ) {
			sumSurv = sumSurv.add(elt.getValue());
		}
		
		FixedPointNumber sumDier = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListDierBank ) {
			sumDier = sumDier.add(elt.getValue());
		}
		
		if ( Math.abs( sumSurv.getBigDecimal().doubleValue() - 
				       sumDier.getBigDecimal().doubleValue() ) > Const.DIFF_TOLERANCE_VALUE ) {
			LOGGER.warn("plausiCheck: Split-sums over survivor- and dier-splits are unequal (bank accounts)");
			LOGGER.debug("plausiCheck: sumSurv: " + sumSurv.getBigDecimal());
			LOGGER.debug("plausiCheck: sumDier: " + sumDier.getBigDecimal());
			return false;
		}
		
		// ---
		
		sumSurv = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListSurvCash ) {
			sumSurv = sumSurv.add(elt.getValue());
		}
		
		sumDier = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListDierCash ) {
			sumDier = sumDier.add(elt.getValue());
		}
		
		if ( Math.abs( sumSurv.getBigDecimal().doubleValue() - 
				       sumDier.getBigDecimal().doubleValue() ) > Const.DIFF_TOLERANCE_VALUE ) {
			LOGGER.warn("plausiCheck: Split-sums over survivor- and dier-splits are unequal (cash accounts)");
			LOGGER.debug("plausiCheck: sumSurv: " + sumSurv.getBigDecimal());
			LOGGER.debug("plausiCheck: sumDier: " + sumDier.getBigDecimal());
			return false;
		}
		
		// ---
		
		sumSurv = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListSurvStock ) {
			sumSurv = sumSurv.add(elt.getValue());
		}
		
		sumDier = new FixedPointNumber();
		for ( GnuCashTransactionSplit elt : spltListDierStock ) {
			sumDier = sumDier.add(elt.getValue());
		}
		
		if ( Math.abs( sumSurv.getBigDecimal().doubleValue() - 
				       sumDier.getBigDecimal().doubleValue() ) > Const.DIFF_TOLERANCE_VALUE ) {
			LOGGER.warn("plausiCheck: Split-sums over survivor- and dier-splits are unequal (stock accounts)");
			LOGGER.debug("plausiCheck: sumSurv: " + sumSurv.getBigDecimal());
			LOGGER.debug("plausiCheck: sumDier: " + sumDier.getBigDecimal());
			return false;
		}
		
		return true;
	}
    
}
