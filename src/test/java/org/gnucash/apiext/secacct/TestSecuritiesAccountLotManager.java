package org.gnucash.apiext.secacct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.InputStream;

import org.gnucash.api.read.GnuCashAccount;
import org.gnucash.api.write.impl.GnuCashWritableFileImpl;
import org.gnucash.apiext.ConstTest;
import org.gnucash.base.basetypes.simple.GCshAcctID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import junit.framework.JUnit4TestAdapter;

public class TestSecuritiesAccountLotManager {

	private static GCshAcctID STOCK_ACCT_ID  = new GCshAcctID("b3741e92e3b9475b9d5a2dc8254a8111"); // SAP

	// -----------------------------------------------------------------

	private GnuCashWritableFileImpl gcshInFile = null;

	// https://stackoverflow.com/questions/11884141/deleting-file-and-directory-in-junit
	@SuppressWarnings("exports")
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	// -----------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

	@SuppressWarnings("exports")
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(TestSecuritiesAccountLotManager.class);
	}

	@Before
	public void initialize() throws Exception {
		ClassLoader classLoader = getClass().getClassLoader();
		// URL gcshFileURL = classLoader.getResource(Const.GCSH_FILENAME);
		// System.err.println("GnuCash test file resource: '" + gcshFileURL + "'");
		InputStream gcshInFileStream = null;
		try {
			gcshInFileStream = classLoader.getResourceAsStream(ConstTest.GCSH_FILENAME_IN);
		} catch (Exception exc) {
			System.err.println("Cannot generate input stream from resource");
			return;
		}

		try {
			gcshInFile = new GnuCashWritableFileImpl(gcshInFileStream);
		} catch (Exception exc) {
			System.err.println("Cannot parse GnuCash in-file");
			exc.printStackTrace();
		}
	}

	@Test
	public void test01() throws Exception {
		GnuCashAccount acct = gcshInFile.getAccountByID(STOCK_ACCT_ID);
		assertEquals(GnuCashAccount.Type.STOCK, acct.getType());
		assertNotEquals(null, acct.getLots());
		assertEquals(1, acct.getLots().size());
		
		assertEquals(true, SecuritiesAccountLotManager.areLotsOK(acct));
	}
}
