package org.gnucash.apiext.trxmgr;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;

import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.write.GnuCashWritableFile;
import org.gnucash.apiext.Const;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionFinder {
	
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionFinder.class);
    
    // ---------------------------------------------------------------
    
	private GnuCashWritableFile gcshFile = null;
	
    // ---------------------------------------------------------------
	
	public TransactionFinder(GnuCashWritableFile gcshFile) {
		if ( gcshFile == null ) {
			throw new IllegalArgumentException("null gnucash-file object given");
		}
		
		this.gcshFile = gcshFile;
	}
    
    // ---------------------------------------------------------------
	
	// ::TODO
	// - Have results writable?
    
	public ArrayList<GnuCashTransaction> find(TransactionFilter flt, 
			                                  boolean withSplits,
			                                  TransactionFilter.SplitLogic splitLogic) {
		if ( flt == null ) {
			throw new IllegalArgumentException("null transaction-filter given");
		}
		
		LOGGER.debug("find: Searching for Transactions matching filter: " + flt.toString());
		ArrayList<GnuCashTransaction> result = new ArrayList<GnuCashTransaction>();
		
		Collection<? extends GnuCashTransaction> candList = null;
		if ( flt.isDatePostedFromSet() ||
			 flt.isDatePostedToSet() ) {
			LocalDate fromDate = null;
			LocalDate toDate = null;
			
			if ( flt.isDatePostedFromSet() )
				fromDate = flt.datePostedFrom;
			else
				fromDate = Const.TRX_SUPER_EARLY_DATE;
			
			if ( flt.isDatePostedToSet() )
				toDate = flt.datePostedTo;
			else
				toDate = Const.TRX_SUPER_LATE_DATE;
			
			candList = gcshFile.getTransactions(fromDate, toDate);
		} else {
			candList = gcshFile.getTransactions();
		}
		
		for ( GnuCashTransaction trx : candList ) {
			if ( flt.matchesCriteria(trx, withSplits, splitLogic) ) {
				result.add(trx);
			}
		}
		
		LOGGER.debug("find: Found " + result.size() + " Transactions matching filter");
		return result;
	}

}
