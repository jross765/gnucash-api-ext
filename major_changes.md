# Major Changes

## V. 1.8 &rarr; 1.9
Followed the deprecation of `FixedPointNumber` in the modules
"(Core) API", V. 1.9 and 
"Specialized Entitites", V. 0.4: 

* Deprecated everything that is `FixedPointNumber`-related (cf. previous release).

* Partially changed implementations so that `BigFraction` is used internally instead of `FixedPointNumber`.

* Usual maintenance: Fixed small bugs, small improvements, low-level code-cleaning.

In more detail:

* Package SecAcct: Small improvements.

* Package TrxMgr: 
  * `SecuritiesAccountTransactionManager_[BF|FP]`:
     * Added method `genSellStockTrx()` (both variants).
     * Used newly-introduced types `KMyMoney(Writable)Stock[Buy|Sell]Transaction`.
     * A number of small improvements.

## V. 1.7 &rarr; 1.8
Adapted to module "Base", V. 1.8 and "API Specialized Entities", V. 0.3.

In more detail:

* Package SecAcct:
  * `SecuritiesAccountManager`: 
     * Added more variants of method `getShareAccounts()`
     * Changed logic of method `getActiveShareAccounts()`
       (now additianally checks for account being hidden).
  * `SecuritiesAccountTransactionManager`: Changed interface:
    Transaction-generating methods now return the according specialized
    entities from module "API Specialized Entities", 
    as you would expect.

* Package TrxMgr:
  
  Nothing

## V. 1.6 &rarr; 1.7
* Introduced new (dummy) ID types (cf. module "Base") for type safety and better symmetry with sister project.

## V. 1.5 &rarr; 1.6
* Added package TrxMgr.
  * New: `Transaction(Split)Filter`
  * New: `TransactionFinder`
  * New: `TransactionManager`, `TransactionMergerXYZ` (the latter in two variants)

* Extended package SecAcct:
  * New: `SecuritiesAccountLotManager`
  * `SecuritiesAccountTransactionManager`: new type "distribution" (as opposed to "dividend"). 
    This leads to a subtle, but importance difference in the generated transaction: 
    One of the splits generated will have another split action.

## V. 1.4 &rarr; 1.5
* Package SecAcct:
  * Added support for stock splits / reverse splits.
  * Added helper class that filters out inactive stock accounts.
  * Added `WritableSecuritiesAccountManager` (analogous to separation in module "API").

## V. 1.3 &rarr; 1.4
Created module.
