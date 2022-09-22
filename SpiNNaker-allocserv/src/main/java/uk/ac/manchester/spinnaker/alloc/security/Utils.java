/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.alloc.security;

import static java.util.Objects.nonNull;
import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.OpenIDProperties;

/** Support utility methods for working with SSL stuff. */
public abstract class Utils {
	private static Logger log;

	private Utils() {
	}

	// Late init
	private static synchronized Logger log() {
		if (log == null) {
			log = getLogger(Utils.class);
		}
		return log;
	}

	/**
	 * Load a trust store into a trust manager.
	 *
	 * @param truststore
	 *            The trust store to load, or {@code null} to use the default.
	 * @return The configured trust manager.
	 * @throws GeneralSecurityException
	 *             If things go wrong.
	 * @see <a href="https://stackoverflow.com/a/24561444/301832">Stack
	 *      Overflow</a>
	 */
	public static X509TrustManager trustManager(KeyStore truststore)
			throws GeneralSecurityException {
		var tmf = TrustManagerFactory.getInstance(getDefaultAlgorithm());
		tmf.init(truststore);
		for (var tm : tmf.getTrustManagers()) {
			if (tm instanceof X509TrustManager) {
				return (X509TrustManager) tm;
			}
		}
		return null;
	}

	/**
	 * Set up the default SSL context so that we can inject our trust store for
	 * verifying servers.
	 *
	 * @param injector
	 *            How to get the value to inject.
	 * @throws GeneralSecurityException
	 *             If the SSL context can't be built.
	 * @see <a href="https://stackoverflow.com/a/24561444/301832">Stack
	 *      Overflow</a>
	 */
	public static void installInjectableTrustStoreAsDefault(
			Supplier<X509TrustManager> injector)
			throws GeneralSecurityException {
		var defaultTm = trustManager(null);
		var sslContext = SSLContext.getInstance("TLS");
		sslContext.init(null, new TrustManager[] {
			new X509TrustManager() {
				@Override
				public X509Certificate[] getAcceptedIssuers() {
					return defaultTm.getAcceptedIssuers();
				}

				@Override
				public void checkServerTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					var customTm = injector.get();
					if (nonNull(customTm)) {
						try {
							customTm.checkServerTrusted(chain, authType);
							// If we got here, we passed!
							return;
						} catch (CertificateException e) {
							log().trace("ignoring certificate exception", e);
						}
					}
					defaultTm.checkServerTrusted(chain, authType);
				}

				@Override
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) throws CertificateException {
					defaultTm.checkClientTrusted(chain, authType);
				}
			}
		}, null);
		SSLContext.setDefault(sslContext);
	}

	/**
	 * Loads a trust store.
	 *
	 * @param props
	 *            Where is the trust store and how should we load it?
	 * @return The trust store. Note that this is not installed into anything.
	 * @throws GeneralSecurityException If the trust store contains bad info.
	 * @throws IOException If the trust store can't be read at all.
	 */
	public static KeyStore loadTrustStore(OpenIDProperties props)
			throws GeneralSecurityException, IOException {
		var myTrustStore = KeyStore.getInstance(props.getTruststoreType());
		try (var myCerts = props.getTruststorePath().getInputStream()) {
			myTrustStore.load(myCerts,
					props.getTruststorePassword().toCharArray());
		}
		return myTrustStore;
	}
}
