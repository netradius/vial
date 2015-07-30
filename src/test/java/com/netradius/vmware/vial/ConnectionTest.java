package com.netradius.vmware.vial;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author Erik R. Jensen
 */
@Slf4j
public class ConnectionTest extends BaseTest {

	@Config
	private String host;

	@Config
	private String username;

	@Config
	private String password;

	@Test
	public void test() throws VialException {
		try (VialConnection con = new VialConnection(host, username, password)) {
			con.setAlwaysTrustSSL(true);
			con.open();
			VirtualMachine vm = con.getVirtualMachine("lei-db-tst-107 - orabi");
			if (vm != null) {
				log.debug(vm.toString());
				vm.createSnapshot("test", "This is a test", false, false);
			}
		}
	}
}
