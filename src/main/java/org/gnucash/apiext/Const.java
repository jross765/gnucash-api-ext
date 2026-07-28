package org.gnucash.apiext;

import java.time.LocalDate;

import org.apache.commons.numbers.fraction.BigFraction;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

// On purpose redundant to according class in org.gnucash.api
public class Const {
  
  public static final int    DIFF_TOLERANCE_DAYS = 3;

  public static final double DIFF_TOLERANCE_VALUE = 0.005;
  
  // ---
  
  public static final double           UNSET_VALUE       = -999999.99;
  private static final int             UNSET_VALUE_NUM   = -99999999; 
  private static final int             UNSET_VALUE_DENOM = 100; 
  
  @Deprecated
  public static final FixedPointNumber UNSET_VALUE_FP    = new FixedPointNumber(UNSET_VALUE); 
  public static final BigFraction      UNSET_VALUE_BF    = BigFraction.of(UNSET_VALUE_NUM, UNSET_VALUE_DENOM); 

  // ---
  
  // For (pseudo-)filtering:
  public static final LocalDate TRX_SUPER_EARLY_DATE = LocalDate.of(1980, 1, 1);
  public static final LocalDate TRX_SUPER_LATE_DATE = LocalDate.of(2100, 12, 31);
  
}
