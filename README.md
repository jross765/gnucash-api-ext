# Notes on the Module "API Extensions"

## What Does It Do?

This module provides simplified, high-level access functions to a 
GnuCash 
file via the "API" module (sort of "macros") for specialized, complex tasks.

## Sub-Modules
Currently, the module consists of two sub-modules:

* "SecAcct"
* "TrxMgr"

### SecAcct
This sub-module contains classes that provide a simplified, high-level interface for...

* generating and maintaining stock accounts,
* generating buy- and dividend/distribution transactions in a securities account (brokerage account),
* handling and tracking account lots for stock accounts (needed, e.g., to prepare German tax filings).

### TrxMgr
This sub-module contains classes that help to...

* find transaction and splits by setting filter criteria,
* merge stock account transcations,
* generally manipulate transactions in a more convenient way than by using the pure API.

## What is This Repo's Relationship with the Other Repos?

* This is a module-level repository which is part of a multi-module project, i.e. it has a parent and several siblings. 

  [Parent](https://github.com/jross765/JGnuCashLibNTools.git)

* Under normal circumstances, you cannot compile it on its own (at least not without further preparation), but instead, you should clone it together with the other repos and use the parent repo's build-script.

* This repository contains no history before V. 1.7 (cf. notes in parent repo).

## Major Changes
### V. 1.6 &rarr; 1.7
* Introduced new (dummy) ID types (cf. module "Base") for type safety and better symmetry with sister project.

### V. 1.5 &rarr; 1.6
* Added sub-module TrxMgr.
  * New: `Transaction(Split)Filter`
  * New: `TransactionFinder`
  * New: `TransactionManager`, `TransactionMergerXYZ` (the latter in two variants)

* Extended sub-module SecAcct:
  * New: `SecuritiesAccountLotManager`
  * `SecuritiesAccountTransactionManager`: new type "distribution" (as opposed to "dividend"). 
    This leads to a subtle, but importance difference in the generated transaction: 
    One of the splits generated will have another split action.

### V. 1.4 &rarr; 1.5
* Sub-module SecAcct:
  * Added support for stock splits / reverse splits.
  * Added helper class that filters out inactive stock accounts.
  * Added `WritableSecuritiesAccountManager` (analogous to separation in module "API").

### V. 1.3 &rarr; 1.4
Created module.

## Planned
* Sub-module SecAcct: 
	* More variants of buy/sell/dividend/etc. transactions, including wrappers which you provide account names to instead of account IDs.
	* Possibly new class for high-level consistency checks of existing transactions, e.g.: All dividends of domestic shares are actually posted to the domestic dividend account.

* New sub-module for accounting-macros, such as closing the books.

* New sub-module for management of commodities and currencies (esp. bulk quote import).

* New sub-module for management of customer jobs and invoices and possibly employee vouchers.

## Known Issues
(None)

