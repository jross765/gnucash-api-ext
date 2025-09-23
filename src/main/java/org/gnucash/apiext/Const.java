package org.gnucash.apiext;

import java.time.LocalDate;

// On purpose redundant to according class in org.gnucash.api
public class Const {
  
  public static final int    DIFF_TOLERANCE_DAYS = 2;

  public static final double DIFF_TOLERANCE_VALUE = 0.005;
  
  // ---
  
  public static final double UNSET_VALUE = -999999.99; 

  // ---
  
  // For (pseudo-)filtering:
  public static final LocalDate TRX_SUPER_EARLY_DATE = LocalDate.of(1980, 1, 1);
  public static final LocalDate TRX_SUPER_LATE_DATE = LocalDate.of(2100, 12, 31);
  
}
