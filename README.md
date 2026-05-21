# Notes on the Module "API Extensions"

## What Does It Do?

This module provides simplified, high-level access functions to a 
GnuCash 
file via the modules "API (Core)" and "API Specialized Entities".
They constitute sort of "macros" for specialized, complex tasks.

## Packages
Currently, the module consists of two packages:

* "SecAcct"
* "TrxMgr"

### SecAcct
This package contains classes that provide a simplified, high-level interface for...

* generating and maintaining stock accounts,
* generating buy- and dividend/distribution transactions in a securities account (brokerage account),
* handling and tracking account lots for stock accounts (needed, e.g., to prepare German tax filings).

### TrxMgr
This package contains classes that help to...

* find transaction and splits by setting filter criteria,
* merge stock account transcations,
* generally manipulate transactions in a more convenient way than by using the pure API.

## What is This Repo's Relationship with the Other Repos?

* This is a module-level repository which is part of a multi-module project, i.e. it has a parent and several siblings. 

  [Parent](https://github.com/jross765/JGnuCashLibNTools.git)

* Under normal circumstances, you cannot compile it on its own (at least not without further preparation), but instead, you should clone it together with the other repos and use the parent repo's build-script.

* This repository contains no history before V. 1.7 (cf. notes in parent repo).

## Major Changes

Cf. document "[Major Changes](https://github.com/jross765/JGnuCashLibNTools/gnucash-api-ext/major_changes.md)".

## Planned
* Package SecAcct: 
	* More variants of buy/sell/dividend/etc. transactions, including wrappers which you provide account names to instead of account IDs.
	* Possibly new class for high-level consistency checks of existing transactions, e.g.: All dividends of domestic shares are actually posted to the domestic dividend account.

* New package for accounting-macros, such as closing the books.

* New package for management of securities and currencies (esp. bulk quote import).

* New package for management of customer jobs and invoices and possibly employee vouchers.

## Known Issues

### Package SecAcct
* The specialized entities are built-up "manually", as the according entities' design and 
  implementation (module "API Specialized Entities") assumes that the transaction passed 
  to the contructor is already built -- at least to the point that the validation test passes.

### Package TrxMgr
(Nothing)

