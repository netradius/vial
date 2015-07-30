package com.netradius.vmware.vial;

import com.vmware.vim25.ManagedObjectReference;

/**
 * @author Erik R. Jensen
 */
public abstract class VialObject {

	protected VialConnection connection;
	protected ManagedObjectReference ref;

	protected VialObject(VialConnection connection, ManagedObjectReference ref) {
		this.connection = connection;
		this.ref = ref;
	}
}
