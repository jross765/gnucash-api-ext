module gnucash.apiext {
	requires static org.slf4j;
	requires java.desktop;
	
	requires transitive schnorxoborx.schnorxolib;

	requires transitive gnucash.base;
	requires transitive gnucash.api;

	exports org.gnucash.apiext.secacct;
	exports org.gnucash.apiext.trxmgr;
}