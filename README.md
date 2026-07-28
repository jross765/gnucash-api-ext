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
It should go without saying, but the following items are of course subject to change and 
by no means a promise that they will actually be implemented soon:

* Package `SecAcct`: 
	* More variants of buy/sell/dividend/etc. transactions, including wrappers which you provide account names to instead of account IDs.
	* *Possibly*: New class for high-level consistency checks of existing transactions, e.g.: All dividends of domestic shares are actually posted to the domestic dividend account.

* New package for accounting-macros for more complex stuff.

  Currently, the author sees two candidates for this:

  * Closing the books: This actually already has been implemented in
    the tool `CloseBooks` (cf. module "Tools"), and the code there
    is so simple that it's not really worth while opening a new 
    package for it.
  * Correct and efficient handling of crypto-currency transactions:
    Actually, that one is already in the pipeline and about to undergo
    some testing, but it's not published yet. Please be patient.

* New package for management of securities and currencies (esp. bulk quote import).

  The author actually already has written such a module as well as several tools based on it, 
  but he cannot publish them in the current state, as they...
  * have additional dependencies that are not generally available, and 
  * they are too tightly embedded in and tailored to his specific 
    working environment and needs, i.e. not general enough.

  One day, when he finds some time, he might get this to-do done. Please be patient.

* *Possibly*: New package for management of customer jobs and invoices and possibly employee vouchers.

## Known Issues

### Package SecAcct
* The specialized entities are built-up "manually", as the according entities' design and 
  implementation (module "API Specialized Entities") assumes that the transaction passed 
  to the contructor is already built -- at least to the point that the validation test passes.

### Package TrxMgr
(Nothing)

