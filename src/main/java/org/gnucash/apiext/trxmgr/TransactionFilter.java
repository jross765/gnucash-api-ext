package org.gnucash.apiext.trxmgr;

import java.time.LocalDate;

import org.gnucash.api.read.GnuCashTransaction;
import org.gnucash.api.read.GnuCashTransactionSplit;

public class TransactionFilter {
	
	public enum SplitLogic {
		AND, // split criteria have to apply to every single split
		OR   // it's enough if split criteria apply to one or just a few splits
	}
	
	// ---------------------------------------------------------------

	public static final LocalDate DATE_UNSET     = LocalDate.of(1900, 1, 1); // LocalDateHelpers.DATE_UNSET
	public static final int       NOF_SPLT_UNSET = 0;
	
	// ---------------------------------------------------------------
	// Transaction Level

	// ::TODO -- not supported yet
	// public GnuCashTransaction.Type type;
	
	public LocalDate datePostedFrom;
	public LocalDate datePostedTo;
	
	public LocalDate dateEnteredFrom;
	public LocalDate dateEnteredTo;
	
	public int nofSpltFrom;
	public int nofSpltTo;

	public String descrPart;
	
	// ----------------------------
	// Split Level

	public TransactionSplitFilter spltFilt;
	
	// ---------------------------------------------------------------
	
	public TransactionFilter() {
		init();
		reset();
	}

	// ---------------------------------------------------------------
	
	private void init() {
		// type = null;

		datePostedFrom  = DATE_UNSET;
		datePostedTo    = DATE_UNSET;
		
		dateEnteredFrom = DATE_UNSET;
		dateEnteredTo   = DATE_UNSET;
		
		nofSpltFrom = NOF_SPLT_UNSET;
		nofSpltTo   = NOF_SPLT_UNSET;

		descrPart = "";
		
		// ---
		
		spltFilt = new TransactionSplitFilter();
	}
	
	public void reset() {
		// type = null;
		
		datePostedFrom  = DATE_UNSET;
		datePostedTo    = DATE_UNSET;
		
		dateEnteredFrom = DATE_UNSET;
		dateEnteredTo   = DATE_UNSET;
		
		nofSpltFrom = NOF_SPLT_UNSET;
		nofSpltTo   = NOF_SPLT_UNSET;

		descrPart = "";
		
		// ---
		
		spltFilt.reset();
	}
	
	// ---------------------------------------------------------------
	
	public boolean matchesCriteria(final GnuCashTransaction trx,
								   final boolean withSplits,
								   final SplitLogic splitLogic) {
		return matchesCriteria(trx, true, withSplits, splitLogic);
	}

	public boolean matchesCriteria(final GnuCashTransaction trx,
								   final boolean datePostedAlreadyFiltered,
			                       final boolean withSplits,
			                       final SplitLogic splitLogic) {
		
		if ( trx == null ) {
			throw new IllegalArgumentException("argument <trx> is null");
		}
		
		// 1) Transaction Level
//		if ( type != null ) {
//			if ( trx.gettype() != type) {
//				return false;
//			}
//		}

		// ---
		
		if ( ! datePostedAlreadyFiltered ) {
			if ( isDatePostedFromSet() ) {
				if ( trx.getDatePosted().toLocalDate().isBefore(datePostedFrom) ) {
					return false;
				}
			}
		
			if ( isDatePostedToSet() ) {
				if ( trx.getDatePosted().toLocalDate().isAfter(datePostedTo) ) {
					return false;
				}
			}
		}
		
		// ---
			
		if ( isDateEnteredFromSet() ) {
			if ( trx.getDateEntered().toLocalDate().isBefore(dateEnteredFrom) ) {
				return false;
			}
		}
		
		if ( isDateEnteredToSet() ) {
			if ( trx.getDateEntered().toLocalDate().isAfter(dateEnteredTo) ) {
				return false;
			}
		}
			
		// ---
		
		if ( nofSpltFrom != NOF_SPLT_UNSET ) {
			if ( trx.getSplitsCount() < nofSpltFrom ) {
				return false;
			}
		}
		
		if ( nofSpltTo != NOF_SPLT_UNSET ) {
			if ( trx.getSplitsCount() > nofSpltTo ) {
				return false;
			}
		}
		
		// ---
		
		if ( ! descrPart.trim().equals("") ) {
			if ( trx.getDescription() != null ) {
				if ( ! trx.getDescription().toLowerCase().contains(descrPart.trim().toLowerCase()) ) {
					return false;
				}
			} else {
				return false;
			}
		}
		
		// ---------
		
		// 2) Split Level
		if ( withSplits ) {
			if ( ! splitsMatchCriteria(trx, splitLogic) ) {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean splitsMatchCriteria(final GnuCashTransaction trx,
										final SplitLogic splitLogic) {
		if ( spltFilt == null ) {
			throw new IllegalStateException("split-filter is null");
		}
		
		if ( splitLogic == SplitLogic.AND ) {
			for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
				if ( ! spltFilt.matchesCriteria(splt) ) {
					return false;
				}
			}
			return true;
		} else if ( splitLogic == SplitLogic.OR ) {
			boolean oneMatch = false;
			for ( GnuCashTransactionSplit splt : trx.getSplits() ) {
				if ( spltFilt.matchesCriteria(splt) ) {
					oneMatch = true;
				}
			}
			if ( ! oneMatch )
				return false;
			else
				return true;
		} // splitLogic
		
		return true; // Compiler happy
	}
	
	// ---------------------------------------------------------------
	// helpers

	public boolean isDatePostedFromSet() {
		if ( datePostedFrom.equals(DATE_UNSET) )
			return false;
		else			
			return true;
	}

	public boolean isDatePostedToSet() {
		if ( datePostedTo.equals(DATE_UNSET) )
			return false;
		else			
			return true;
	}
	
	// ----------------------------

	public boolean isDateEnteredFromSet() {
		if ( dateEnteredFrom.equals(DATE_UNSET) )
			return false;
		else			
			return true;
	}

	public boolean isDateEnteredToSet() {
		if ( dateEnteredTo.equals(DATE_UNSET) )
			return false;
		else			
			return true;
	}
	
	// ---------------------------------------------------------------

	@Override
	public String toString() {
		return "TransactionFilter [" + 
	              "datePostedFrom=" + datePostedFrom  + ( isDatePostedFromSet()  ? "" : " (unset)" ) + ", " +
				    "datePostedTo=" + datePostedTo    + ( isDatePostedToSet()    ? "" : " (unset)" ) + ", " +
	              
                 "dateEnteredFrom=" + dateEnteredFrom + ( isDateEnteredFromSet() ? "" : " (unset)" ) + ", " +
                   "dateEnteredTo=" + dateEnteredTo   + ( isDateEnteredToSet()   ? "" : " (unset)" ) + ", " +

	                 "nofSpltFrom=" + nofSpltFrom + ( nofSpltFrom == NOF_SPLT_UNSET ? " (unset)" : "" ) + ", " + 
				       "nofSpltTo=" + nofSpltTo   + ( nofSpltTo   == NOF_SPLT_UNSET ? " (unset)" : "" ) + ", " +
	                 
	                  "descrPart='" + descrPart + "', " +

				        "spltFilt=" + spltFilt + "]";
	}

}
