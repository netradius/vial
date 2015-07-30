package com.netradius.vmware.vial;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * This class is used to disable SSL certificate validation. Use with caution.
 */
public class TrustAllTrustManager implements TrustManager, X509TrustManager {

	public X509Certificate[] getAcceptedIssuers() {
		return new X509Certificate[0];
	}

	@SuppressWarnings("unused")
	public boolean isServerTrusted(X509Certificate[] certs) {
		return true;
	}

	@SuppressWarnings("unused")
	public boolean isClientTrusted(X509Certificate[] certs) {
		return true;
	}

	public void checkServerTrusted(X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
		// do nothing
	}

	public void checkClientTrusted(X509Certificate[] certs, String authType)
			throws java.security.cert.CertificateException {
		// do nothing
	}
}