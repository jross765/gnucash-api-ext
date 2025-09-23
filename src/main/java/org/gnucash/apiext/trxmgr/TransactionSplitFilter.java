package org.gnucash.apiext.trxmgr;

import java.math.BigDecimal;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.apiext.Const;
import org.gnucash.base.basetypes.simple.GCshAcctID;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class TransactionSplitFilter {

	// ::TODO
//	public enum DebitCredit {
//		DEBIT,
//		CREDIT,
//		UNDEFINED
//	}
	
	// ---------------------------------------------------------------

	public GnuCashTransactionSplit.Action      action;
	// ::TODO -- not supported yet
	// public GnuCashTransactionSplit.ReconStatus reconStatus;
	
	public GCshAcctID          acctID;
	public GnuCashAccount.Type acctType;
	
	public FixedPointNumber valueFrom;
	public FixedPointNumber valueTo;
	
	public FixedPointNumber quantityFrom;
	public FixedPointNumber quantityTo;
	
	public String descrPart;
	
	// ---------------------------------------------------------------
	
	public TransactionSplitFilter() {
		init();
		reset();
	}

	// ---------------------------------------------------------------
	
	private void init() {
		action = null;
		// reconStatus = null;
		
		acctID = new GCshAcctID();
		acctType = null;
		
		valueFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		
		quantityFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		
		descrPart = "";
	}
	
	public void reset() {
		action = null;
		// reconStatus = null;
		
		acctID.reset();
		acctType = null;
		
		valueFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		
		quantityFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		
		descrPart = "";
	}
	
	// ---------------------------------------------------------------
	
	public boolean matchesCriteria(final GnuCashTransactionSplit splt) {
		
		if ( splt == null ) {
			throw new IllegalArgumentException("null transaction-split given");
		}
		
		if ( action != null ) {
			if ( splt.getAction() != action ) {
				return false;
			}
		}
		
//		if ( reconStatus != null ) {
//			if ( ! splt.getReconStatus().getID().equals(acctID) ) {
//				return false;
//			}
//		}
		
		if ( acctID.isSet() ) {
			if ( ! splt.getAccount().getID().equals(acctID) ) {
				return false;
			}
		}
		
		if ( acctType != null ) {
			if ( splt.getAccount().getType() != acctType ) {
				return false;
			}
		}
		
		if ( valueFrom.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			if ( splt.getValue().isLessThan(valueFrom, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( valueTo.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			if ( splt.getValue().isGreaterThan(valueTo, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( quantityFrom.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			if ( splt.getQuantity().isLessThan(quantityFrom, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( quantityTo.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			if ( splt.getQuantity().isGreaterThan(quantityTo, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( ! descrPart.trim().equals("") ) {
			if ( ! splt.getDescription().contains(descrPart.trim()) ) {
				return false;
			}
		}
		
		return true;
	}
	
	// ---------------------------------------------------------------
	
	@Override
	public String toString() {
		return "TransactionSplitFilter [" + 
	                 "action=" + action + ", " +
				     "acctID=" + acctID + ", " +
	               "acctType=" + acctType + ", " +
				  "valueFrom=" + valueFrom + ", " +
	                "valueTo=" + valueTo + ", " +
			   "quantityFrom=" + quantityFrom + ", " + 
	             "quantityTo=" + quantityTo + ", " +
			      "descrPart='" + descrPart + "']";
	}

}
