package org.gnucash.apiext.trxmgr;

import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.write.GnuCashWritableFile;
import org.gnucash.api.write.GnuCashWritableTransaction;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionMergerVar1 extends TransactionMergerBase
								   implements IFTransactionMerger 
{
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMergerVar1.class);
    
    // ---------------------------------------------------------------
    
	public TransactionMergerVar1(GnuCashWritableFile gcshFile) {
		super(gcshFile);
		setVar(Var.VAR_1);
	}
    
    // ---------------------------------------------------------------
    
	public void merge(GCshTrxID survivorID, GCshTrxID dierID) throws MergePlausiCheckException {
		GnuCashTransaction survivor = gcshFile.getTransactionByID(survivorID);
		GnuCashWritableTransaction dier = gcshFile.getWritableTransactionByID(dierID);
		merge(survivor, dier);
	}

	public void merge(GnuCashTransaction survivor, GnuCashWritableTransaction dier) throws MergePlausiCheckException {
		// 1) Perform plausi checks
		if ( ! plausiCheck(survivor, dier) ) {
			LOGGER.error("merge: survivor-dier-pair did not pass plausi check: " + survivor.getID() + "/" + dier.getID());
			throw new MergePlausiCheckException();
		}
		
		// 2) If OK, remove dier
		GCshTrxID dierID = dier.getID();
		gcshFile.removeTransaction(dier);
		LOGGER.info("merge: Transaction " + dierID + " (dier) removed");
	}

}
