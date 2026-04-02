package org.gnucash.apiext.trxmgr;

import org.apache.commons.numbers.fraction.BigFraction;
import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashTransactionSplit;
import org.gnucash.api.read.impl.GnuCashTransactionSplitImpl;
import org.gnucash.apiext.Const;
import org.gnucash.base.basetypes.simple.GCshAcctID;

public class TransactionSplitFilter_BF {
	
	// a bit bulky, I admit...
	private static final BigFraction UNSET_VALUE = TransactionSplitFilter_FP.UNSET_VALUE.toBigFraction();

	// ---------------------------------------------------------------

	public GnuCashTransactionSplit.Action     action;
	public GnuCashTransactionSplit.ReconState reconState;
	
	public GCshAcctID       acctID;
	
	public GnuCashAccount.Type acctType;
	
	public BigFraction      valueFrom;
	public BigFraction      valueTo;
	public boolean          valueAbs;
	
	public BigFraction      quantityFrom;
	public BigFraction      quantityTo;
	public boolean          quantityAbs;
	
	public String descrPart;
	
	// ---------------------------------------------------------------
	
	public TransactionSplitFilter_BF() {
		init();
		reset();
	}

	// ---------------------------------------------------------------
	
	private void init() {
		action = null;
		reconState = null;
		
		acctID = new GCshAcctID();
		
		acctType = null;
		
		valueFrom = UNSET_VALUE;
		valueTo   = UNSET_VALUE;
		valueAbs  = false;
		
		quantityFrom = UNSET_VALUE;
		quantityTo   = UNSET_VALUE;
		quantityAbs  = false;
		
		descrPart = "";
	}
	
	public void reset() {
		action = null;
		reconState = null;

		acctID.reset();

		acctType = null;

		valueFrom = UNSET_VALUE;
		valueTo   = UNSET_VALUE;
		valueAbs  = false;

		quantityFrom = UNSET_VALUE;
		quantityTo   = UNSET_VALUE;
		quantityAbs  = false;
		
		descrPart = "";
	}
	
	// ---------------------------------------------------------------
	
	public boolean matchesCriteria(final GnuCashTransactionSplit splt) {
		
		if ( splt == null ) {
			throw new IllegalArgumentException("argument <splt> is null");
		}
		
		// ---
		
		if ( action != null ) {
			// Important pre-check first,
			// as values returned are *not* standardized:
			String actionStr = splt.getActionStr();
			if ( actionStr == null ) {
				return false;
			}

			if ( actionStr.trim().equals("") ) {
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
		
		if ( ! valueFrom.equals(UNSET_VALUE) ) {
			BigFraction val = splt.getValueRat();
			if ( valueAbs && 
				 val.compareTo(BigFraction.ZERO) < 0 ) {
				val = val.negate(); // immutable
			}
			
			// CAUTION: Will not work due to bug in BigFraction.compareTo()
			// if ( val.compareTo(valueFrom) < 0 ) {
			// Instead:
			if ( valueFrom.subtract(val).compareTo(BigFraction.ZERO) > 0 ) {
				return false;
			}
		}
		
		if ( ! valueTo.equals(UNSET_VALUE) ) {
			BigFraction val = splt.getValueRat();
			if ( valueAbs && 
				 val.compareTo(BigFraction.ZERO) < 0 ) {
				val = val.negate(); // immutable
			}
			
			// CAUTION: Will not work due to bug in BigFraction.compareTo()
			// if ( val.compareTo(valueTo) > 0 ) {
			// Instead:
			if ( valueTo.subtract(val).compareTo(BigFraction.ZERO) < 0 ) {
				return false;
			}
		}
		
		// ---
		
		if ( ! quantityFrom.equals(UNSET_VALUE) ) {
			BigFraction qty = splt.getQuantityRat();
			if ( quantityAbs && 
				 qty.compareTo(BigFraction.ZERO) < 0 ) {
				qty = qty.negate(); // immutable
			}
			
			// CAUTION: Will not work due to bug in BigFraction.compareTo()
			// if ( qty.compareTo(quantityFrom) < 0 ) {
			// Instead:
			if ( quantityFrom.subtract(qty).compareTo(BigFraction.ZERO) > 0 ) {
				return false;
			}
		}
		
		if ( ! quantityTo.equals(UNSET_VALUE) ) {
			BigFraction qty = splt.getQuantityRat();
			if ( quantityAbs && 
				 qty.compareTo(BigFraction.ZERO) < 0 ) {
				qty = qty.negate(); // immutable
			}
			
			// CAUTION: Will not work due to bug in BigFraction.compareTo()
			// if ( qty.compareTo(quantityTo) > 0 ) {
			// Instead:
			if ( quantityTo.subtract(qty).compareTo(BigFraction.ZERO) < 0 ) {
				return false;
			}
		}
		
		// ---
		
		if ( ! descrPart.trim().equals("") ) {
			if ( splt.getDescription() != null ) {
				if ( ! splt.getDescription().toLowerCase().contains(descrPart.trim().toLowerCase()) ) {
					return false;
				}
			} else {
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
		         "recon-state=" + reconState + ", " +

				     "acctID=" + acctID + ", " +
				     
	               "acctType=" + acctType + ", " +

				  "valueFrom=" + valueFrom + ( valueFrom.bigDecimalValue().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	                "valueTo=" + valueTo   + ( valueTo  .bigDecimalValue().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	               "valueAbs=" + valueAbs + ", " +

			   "quantityFrom=" + quantityFrom + ( quantityFrom.bigDecimalValue().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " + 
	             "quantityTo=" + quantityTo   + ( quantityTo  .bigDecimalValue().doubleValue() == Const.UNSET_VALUE ? " (unset)" : "" ) + ", " +
	            "quantityAbs=" + quantityAbs+ ", " +

			      "descrPart='" + descrPart + "']";
	}

}
