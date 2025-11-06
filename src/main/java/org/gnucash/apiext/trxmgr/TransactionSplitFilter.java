package org.gnucash.apiext.trxmgr;

import java.math.BigDecimal;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashTransactionSplitImpl;
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
	public GnuCashTransactionSplit.ReconStatus reconState;
	
	public GCshAcctID          acctID;
	
	public GnuCashAccount.Type acctType;
	
	public FixedPointNumber valueFrom;
	public FixedPointNumber valueTo;
	public boolean          valueAbs;
	
	public FixedPointNumber quantityFrom;
	public FixedPointNumber quantityTo;
	public boolean          quantityAbs;
	
	public String descrPart;
	
	// ---------------------------------------------------------------
	
	public TransactionSplitFilter() {
		init();
		reset();
	}

	// ---------------------------------------------------------------
	
	private void init() {
		action = null;
		reconState = null;
		
		acctID = new GCshAcctID();
		
		acctType = null;
		
		valueFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueTo   = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueAbs  = false;
		
		quantityFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityTo   = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityAbs  = false;
		
		descrPart = "";
	}
	
	public void reset() {
		action = null;
		reconState = null;
		
		acctID.reset();
		
		acctType = null;
		
		valueFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		valueAbs  = false;

		quantityFrom = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityTo = new FixedPointNumber(BigDecimal.valueOf(Const.UNSET_VALUE));
		quantityAbs  = false;
		
		descrPart = "";
	}
	
	// ---------------------------------------------------------------
	
	public boolean matchesCriteria(final GnuCashTransactionSplit splt) {
		
		if ( splt == null ) {
			throw new IllegalArgumentException("null transaction-split given");
		}
		
		// ---
		
		if ( action != null ) {
			// Important pre-check first,
			// as values returned are *not* standardized:
			String actionStr = splt.getActionStr();
			if ( actionStr == null ) {
				return false;
			}

			// Core check
			if ( splt.getAction() != action ) {
				return false;
			}
		}
		
		if ( reconState != null ) {
			// Pre-check here not really important (as opposed to action above),
			// as values returned are standardized:
			String reconStateStr = ((GnuCashTransactionSplitImpl) splt).getReconStateStr();
			if ( reconStateStr == null ) {
				return false;
			}

			// Core check
			if ( splt.getReconState() != reconState ) {
				return false;
			}
		}
		
		// ---
		
		if ( acctID.isSet() ) {
			if ( splt.getAccountID() != null ) { // not important
				if ( ! splt.getAccount().getID().equals(acctID) ) {
					return false;
				}
			}
		}
		
		// ---
		
		if ( acctType != null ) {
			if ( splt.getAccount().getType() != acctType ) {
				return false;
			}
		}
		
		// ---
		
		if ( valueFrom.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			FixedPointNumber val = splt.getValue();
			if ( valueAbs && 
				 val.isNegative() ) {
				val.negate();
			}
			
			if ( val.isLessThan(valueFrom, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( valueTo.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			FixedPointNumber val = splt.getValue();
			if ( valueAbs && 
				 val.isNegative() ) {
				val.negate();
			}
			
			if ( val.isGreaterThan(valueTo, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		// ---
		
		if ( quantityFrom.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			FixedPointNumber qty = splt.getQuantity();
			if ( quantityAbs && 
				 qty.isNegative() ) {
				qty.negate();
			}
			
			if ( qty.isLessThan(quantityFrom, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		if ( quantityTo.getBigDecimal().doubleValue() != Const.UNSET_VALUE ) {
			FixedPointNumber qty = splt.getQuantity();
			if ( quantityAbs && 
				 qty.isNegative() ) {
				qty.negate();
			}
			
			if ( qty.isGreaterThan(quantityTo, Const.DIFF_TOLERANCE_VALUE ) ) {
				return false;
			}
		}
		
		// ---
		
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
		       "recon-status=" + reconState + ", " +

				     "acctID=" + acctID + ", " +
				     
	               "acctType=" + acctType + ", " +

				  "valueFrom=" + valueFrom + ( valueFrom.getBigDecimal().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	                "valueTo=" + valueTo   + ( valueTo  .getBigDecimal().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	               "valueAbs=" + valueAbs + ", " +

			   "quantityFrom=" + quantityFrom + ( quantityFrom.getBigDecimal().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " + 
	             "quantityTo=" + quantityTo   + ( quantityTo  .getBigDecimal().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	            "quantityAbs=" + quantityAbs+ ", " +

			      "descrPart='" + descrPart + "']";
	}

}
