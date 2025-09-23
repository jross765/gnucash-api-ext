package org.gnucash.apiext.trxmgr;

import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.write.GnuCashWritableFile;
import org.gnucash.api.write.GnuCashWritableTransaction;
import org.gnucash.api.write.GnuCashWritableTransactionSplit;
import org.gnucash.base.basetypes.simple.GCshSpltID;
import org.gnucash.base.basetypes.simple.GCshTrxID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionMergerVar2 extends TransactionMergerBase
								   implements IFTransactionMerger 
{
    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionMergerVar2.class);
    
    // ---------------------------------------------------------------
    
	// CAUTION: 
	// In this merger variant, the survivor trx's bank split will be deleted
	// and vice versa.
	// ==> The zdTrxBankSpltID (ZD Splt), part of the dier trx, will die, 
	//     but a copy of it will survive as part of the survivor trx, 
	//     called ZS Splt/after.
	// Analogously, the dierBankTrxSplt (ZS Splt/before), part of the survivor trx, 
	// will die / be replaced by the above-mentioned copy ZS Splt/after.
	// 
	// Visualization of Typical Example:
	// ---------------------------------
	//
	// Dier Trx                            Survivor Trx
	// +-- XD Splt                         +-- XS Splt (to stock account)
	// |                                   +-- YS.a Splt (to expenses account)
	// |                                   +-- YS.b Splt (to expenses account)
	// |                                   +-- YS.c Splt (to income account)
	// +-- ZD Splt (to bank acct)          +-- ZS Splt/before (to bank account)
	//     ^                               |   ^
	//     +-- will be copied to           |   +-- Will be replaced by ZS Splt/after
	//         survivor trx -------------> +-- ZS Splt/after (to bank account)
	//                                         ^
	//                                         +-- Copy of ZD Splt
    //
	// Let that sink in for a moment before you review the code in this class.

	private GnuCashWritableTransaction survTrx = null;
	private GnuCashTransactionSplit zDierTrxBankSplt = null; // "ZS Split/before"
	private GnuCashTransactionSplit zSurvTrxBankSpltBefore = null; // "ZS Split/before"
	
	private GCshSpltID zDierTrxBankSpltID = null;       // cf. above
	private GCshSpltID zSurvTrxBankSpltBeforeID = null; // dto.

    // ---------------------------------------------------------------
	
	public TransactionMergerVar2(GnuCashWritableFile gcshFile) {
		super(gcshFile);
		setVar(Var.VAR_2);
	}
    
    // ---------------------------------------------------------------
	
	public GCshSpltID getZDierTrxBankSpltID() {
		return zDierTrxBankSpltID;
	}
    
	public void setZDierTrxBankSpltID(GCshSpltID spltID) {
		this.zDierTrxBankSpltID = spltID;
		
		zDierTrxBankSplt = gcshFile.getTransactionSplitByID(spltID);
	}
	
	// ---
    
	public GCshSpltID getZSurvTrxBankSpltBeforeID() {
		return zSurvTrxBankSpltBeforeID;
	}
    
	public void setZSurvTrxBankSpltBeforeID(GCshSpltID spltID) {
		this.zSurvTrxBankSpltBeforeID = spltID;
		
		zSurvTrxBankSpltBefore = gcshFile.getTransactionSplitByID(spltID);
	}
    
	// ---
    
	public GnuCashWritableTransaction getSurvTrx() {
		return survTrx;
	}
    
	public void setSurvTrx(GnuCashWritableTransaction trx) {
		this.survTrx = trx;
	}
    
    // ---------------------------------------------------------------
    
	public void merge(GCshTrxID survivorID, GCshTrxID dierID) throws MergePlausiCheckException {
		GnuCashTransaction survivor = gcshFile.getTransactionByID(survivorID);
		GnuCashWritableTransaction dier = gcshFile.getWritableTransactionByID(dierID);
		merge(survivor, dier);
	}

	public void merge(GnuCashTransaction survivor, GnuCashWritableTransaction dier) throws MergePlausiCheckException {
		if ( zDierTrxBankSpltID == null ) {
			throw new IllegalStateException("Z dier Trx bank Split ID is null");
		}
		
		if ( zSurvTrxBankSpltBeforeID == null ) {
			throw new IllegalStateException("Z survivor Trx bank Split (before) ID is null");
		}
		
		if ( survTrx == null ) {
			throw new IllegalStateException("Survivor Trx is null");
		}
		
		if ( ! zDierTrxBankSpltID.isSet() ) {
			throw new IllegalStateException("Z dier Trx bank Split ID is not set");
		}
		
		if ( ! zSurvTrxBankSpltBeforeID.isSet() ) {
			throw new IllegalStateException("Z survivor Trx bank Split (before) ID is not set");
		}
		
		if ( ! survTrx.getID().isSet() ) {
			throw new IllegalStateException("New bank Trx's ID is not set");
		}
		
		if ( zDierTrxBankSpltID.equals(zSurvTrxBankSpltBeforeID) ) {
			throw new IllegalStateException("IDs of Z dier Trx bank Split and Z survivor Trx bank Split (before) are identical");
		}
		
		// ---
		
		// 1) Perform plausi checks
		if ( ! plausiCheck(survivor, dier) ) {
			LOGGER.error("merge: survivor-dier-pair did not pass plausi check: " + survivor.getID() + "/" + dier.getID());
			throw new MergePlausiCheckException();
		}

		GnuCashWritableTransactionSplit zSurvBankTrxSpltAfter = copyBankTrxSplt();
		LOGGER.info("merge: Transaction Split " + zDierTrxBankSpltID + " copied to new Splt " + zSurvBankTrxSpltAfter.getID());
		
		GnuCashWritableTransactionSplit zSurvBankTrxSpltBefore = gcshFile.getWritableTransactionSplitByID(zSurvTrxBankSpltBeforeID);
		survTrx.remove(zSurvBankTrxSpltBefore);
		LOGGER.info("merge: Removed Transaction Split " + zSurvTrxBankSpltBeforeID);

		GCshTrxID dierID = dier.getID();
		gcshFile.removeTransaction(dier);
		LOGGER.info("merge: Transaction " + dierID + " (dier) removed");
	}

    // ---------------------------------------------------------------
	
	private GnuCashWritableTransactionSplit copyBankTrxSplt() {
		GnuCashWritableTransactionSplit copy = survTrx.createWritableSplit(zDierTrxBankSplt.getAccount());

		if ( zDierTrxBankSplt.getAction() != null )
			copy.setAction(zDierTrxBankSplt.getAction());
		copy.setAccountID(zSurvTrxBankSpltBefore.getAccountID());
		copy.setValue(zDierTrxBankSplt.getValue().negate());
		copy.setQuantity(zDierTrxBankSplt.getQuantity().negate());
		copy.setDescription(zDierTrxBankSplt.getDescription());
		
		// User-defined attributes
		// ::TODO
//		for ( String attrKey : zdTrxBankSplt.getUserDefinedAttributeKeys() ) {
//			newBankTrxSplt.addUserDefinedAttribute( zdTrxBankSplt.getUserDefinedAttribute(attrKey) );
//		}
		
		return copy;
	}
}
