package com.netradius.vmware.vial;

/**
 * @author Erik R. Jensen
 */
public class VialException extends Exception {

	public VialException() {}

	public VialException(String msg) {
		super(msg);
	}

	public VialException(Throwable t) {
		super(t);
	}

	public VialException(String msg, Throwable t) {
		super(msg, t);
	}
}
