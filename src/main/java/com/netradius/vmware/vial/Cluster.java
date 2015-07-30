package com.netradius.vmware.vial;

import com.vmware.vim25.ManagedObjectReference;

/**
 * @author Erik R. Jensen
 */
public class Cluster extends VialObject {

	protected Cluster(VialConnection connection, ManagedObjectReference ref) {
		super(connection, ref);
	}
}
