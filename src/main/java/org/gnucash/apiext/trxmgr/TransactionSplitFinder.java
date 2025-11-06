package org.gnucash.apiext.trxmgr;

import java.util.ArrayList;
import java.util.Collection;

import org.gnucash.api.read.GnuCashFile;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionSplitFinder {
	
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionSplitFinder.class);
    
    // ---------------------------------------------------------------
    
	private GnuCashFile gcshFile = null;
	
    // ---------------------------------------------------------------
	
	public TransactionSplitFinder(GnuCashFile gcshFile) {
		if ( gcshFile == null ) {
			throw new IllegalArgumentException("null gnucash-file object given");
		}
		
		this.gcshFile = gcshFile;
	}
    
    // ---------------------------------------------------------------
	
	// ::TODO
	// - Have results writable?
    
	public ArrayList<GnuCashTransactionSplit> find(TransactionSplitFilter flt) {
		if ( flt == null ) {
			throw new IllegalArgumentException("null transaction-split-filter given");
		}
		
		LOGGER.debug("find: Searching for Transaction-Splits matching filter: " + flt.toString());
		ArrayList<GnuCashTransactionSplit> result = new ArrayList<GnuCashTransactionSplit>();
		
		Collection<GnuCashTransactionSplit> candList = gcshFile.getTransactionSplits();
		
		for ( GnuCashTransactionSplit splt : candList ) {
			if ( flt.matchesCriteria(splt) ) {
				result.add(splt);
			}
		}

		LOGGER.debug("find: Found " + result.size() + " Transaction-Splits matching filter");
		return result;
	}

}
