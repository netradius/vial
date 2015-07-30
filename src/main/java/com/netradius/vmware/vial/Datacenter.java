package com.netradius.vmware.vial;

import com.vmware.vim25.ManagedObjectReference;

/**
 * @author Erik R. Jensen
 */
public class Datacenter extends VialObject {

	protected Datacenter(VialConnection connection, ManagedObjectReference ref) {
		super(connection, ref);
	}
}
