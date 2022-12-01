package uk.ac.manchester.spinnaker.storage;

import javax.validation.Valid;

/**
 * Information about the proxy to allow connection.
 */
public class ProxyInformation {

	/**
	 * The URL of the spalloc server to connect to.
	 */
	@Valid
	public final String spallocUrl;

	/**
	 * The URL of the job to connect to.
	 */
	@Valid
	public final String jobUrl;

	/**
	 * The token to use to authenticate access.
	 */
	@Valid
	public final String bearerToken;

	/**
	 * Create a new ProxyInformation object.
	 *
	 * @param url The URL of the proxy to connect to.
	 * @param bearerToken The bearer token to use as authentication.
	 */
	public ProxyInformation(final String spallocUrl, final String jobUrl,
			final String bearerToken) {
		this.spallocUrl = spallocUrl;
		this.jobUrl = jobUrl;
		this.bearerToken = bearerToken;
	}
}
