package org.gnucash.apiext.secacct;

import java.util.ArrayList;
import java.util.List;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.read.GnuCashFile;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.schnorxoborx.base.numbers.FixedPointNumber;

public class SecuritiesAccountManager {

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger(SecuritiesAccountManager.class);
    
    // ---------------------------------------------------------------
    
    private GnuCashAccount invstAcct = null;
    
    // ---------------------------------------------------------------
    
    public SecuritiesAccountManager() {
    }
    
    public SecuritiesAccountManager(GnuCashFile gcshFile, GCshAcctID acctID) {
    	if ( acctID == null ) {
    		throw new IllegalArgumentException("null account ID given");
    	}
    	
    	if ( ! acctID.isSet() ) {
    		throw new IllegalArgumentException("unset account ID given");
    	}
    	
    	invstAcct = gcshFile.getAccountByID(acctID);
    	if ( invstAcct.getType() != GnuCashAccount.Type.ASSET ) {
    		invstAcct = null;
    		throw new IllegalArgumentException("account is not of type '" + GnuCashAccount.Type.ASSET + "'");
    	}
    }
    
    public SecuritiesAccountManager(GnuCashAccount acct) {
    	setInvstAcct(acct);
    }
    
    // ---------------------------------------------------------------
    
    public GnuCashAccount getInvstAcct() {
		return invstAcct;
	}

	public void setInvstAcct(GnuCashAccount acct) {
    	if ( acct == null ) {
    		throw new IllegalArgumentException("null account given");
    	}
    	
    	if ( acct.getType() != GnuCashAccount.Type.ASSET ) {
    		throw new IllegalArgumentException("account is not of type '" + GnuCashAccount.Type.ASSET + "'");
    	}

		this.invstAcct = acct;
	}

    // ---------------------------------------------------------------
    
    public List<GnuCashAccount> getShareAccts() {
    	return invstAcct.getChildren();
    }
    
	public ArrayList<GnuCashAccount> getActiveShareAccts() {
    	ArrayList<GnuCashAccount> result = new ArrayList<GnuCashAccount>();
    	
    	for ( GnuCashAccount acct : getShareAccts() ) {
    		if ( acct.getBalance().isGreaterThan(new FixedPointNumber()) ) {
    			result.add(acct);
    		}
    	}
    	
    	return result;
    }
    
}
