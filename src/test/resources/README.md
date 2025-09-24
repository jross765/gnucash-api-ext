# Technical Aspects
The test file has been generated with 
Gnucash V. 5.12.

Please note that the file contains *german* data, and not only superficially (i.e., german account names etc.), but also under the hood: GnuCash stores some (semi-)internal information in locale-specific form. This file has been generated on a german system (locale de_DE), and it probably should not be used / changed on a system with another locale.

[ In future releases, we will therefore probably provide one test data file per supported locale. ]

When you change the test.xml file, please save it in *uncompressed* XML format 
(by convention, compressed 
GnuCash
files have the extension 
"gnucash").

# Testing Aspects
Please be careful when making changes on the file: All JUnit test cases of this module heavily rely on it, and you might break things.

# Comparison to Other Modules' Test Files
This test file currently is *identical* to the one of module "API".

This is no coincidence, of course, because until 
V. 1.7, 
we had both modules' JUnit test cases run on one single test data file -- *the* test data file.

However, for organizational reasons, we now 
(i.e, V. 1.7-RESTRUCT and onwards) 
have a separate, redundant copy for this module. Therefore, the two files will very likely not stay identical. Please expect them to divert from one another in the course of the releases to come.

# Accounting Aspects
Cf. the notes in module "API"'s README file for this aspect.
