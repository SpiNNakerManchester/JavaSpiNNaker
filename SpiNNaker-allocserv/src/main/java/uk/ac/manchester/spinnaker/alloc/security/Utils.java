/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import com.google.errorprone.annotations.Var;
import com.google.errorprone.annotations.concurrent.LazyInit;

import uk.ac.manchester.spinnaker.alloc.SpallocProperties.OpenIDProperties;

/** Support utility methods for working with SSL stuff. */
public abstract class Utils {
	@LazyInit
	private static Logger log;

	private Utils() {
	}

	// Late init
	private static synchronized Logger log() {
		@Var
		var l = log;
		if (l == null) {
			l = getLogger(Utils.class);
			log = l;
		}
		return l;
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
			if (tm instanceof X509TrustManager x509tm) {
				return x509tm;
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
