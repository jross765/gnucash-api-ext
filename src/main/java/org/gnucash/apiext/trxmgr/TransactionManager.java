package org.gnucash.apiext.trxmgr;

import java.util.ArrayList;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.write.GnuCashWritableFile;
import org.gnucash.apiext.Const;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class TransactionManager {
	
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionManager.class);
    
    // ---------------------------------------------------------------
    
	private GnuCashWritableFile gcshFile = null;
	
    // ---------------------------------------------------------------
	
	public TransactionManager(GnuCashWritableFile gcshFile) {
		this.gcshFile = gcshFile;
	}
    
    // ---------------------------------------------------------------
    
	public boolean isSane(GCshTrxID trxID) {
		GnuCashTransaction trx = gcshFile.getTransactionByID(trxID);
		return isSane(trx);
	}

	public boolean isSane(GnuCashTransaction trx) {
		if ( trx.getSplits().size() == 0 )
			return false;
		
		FixedPointNumber sum = new FixedPointNumber();
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			sum.add(splt.getValue());
		}
		
		if ( sum.abs().doubleValue() > Const.DIFF_TOLERANCE_VALUE ) {
			LOGGER.warn("isSane: abs. value of sum greater than tolerance: " + sum);
			return false;
		}
		
		return true;
	}
	
	public boolean hasSplitBoundToAccounttType(GnuCashTransaction trx, GnuCashAccount.Type acctType) {
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccount().getType() == acctType )
				return true;
		}
		
		return false;
	}

	public ArrayList<GnuCashTransactionSplit> getSplitsBoundToAccounttType(GnuCashTransaction trx, GnuCashAccount.Type acctType) {
		ArrayList<GnuCashTransactionSplit> result = new ArrayList<GnuCashTransactionSplit>();
		
		for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
			if ( splt.getAccount().getType() == acctType )
				result.add(splt);
		}
		
		return result;
	}
}
