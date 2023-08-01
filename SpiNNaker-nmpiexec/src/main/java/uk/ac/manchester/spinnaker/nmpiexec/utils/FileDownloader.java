/*
 * Copyright (c) 2014 The University of Manchester
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
package uk.ac.manchester.spinnaker.nmpiexec.utils;

import static java.io.File.createTempFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.copy;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;

/**
 * Utilities for downloading a file.
 */
public abstract class FileDownloader {
	/**
	 * Stops instantiation.
	 */
	private FileDownloader() {
		// Does Nothing
	}

	/**
	 * Get the filename from the content disposition header.
	 *
	 * @param contentDisposition The header
	 * @return The filename
	 */
	private static String getFileName(final String contentDisposition) {
		if (nonNull(contentDisposition)) {
			final var cdl = contentDisposition.toLowerCase();
			if (cdl.startsWith("form-data") || cdl.startsWith("attachment")) {
				return new ContentDisposition(cdl).getFilename();
			}
		}
		return null;
	}

	/**
	 * Create an authenticated connection.
	 *
	 * @param url The URL to connect to
	 * @param userInfo The authentication to use, as username:password
	 * @throws IOException if an I/O error occurs
	 * @return The created connection
	 */
	private static URLConnection createConnectionWithAuth(final URL url,
			final String userInfo) throws IOException {
		var urlConnection = requireNonNull(url).openConnection();
		urlConnection.setDoInput(true);

		urlConnection.setRequestProperty("Accept", "*/*");
		if (nonNull(userInfo) && urlConnection instanceof HttpURLConnection) {
			var httpConnection = (HttpURLConnection) urlConnection;
			var basicAuth = "Basic " + Base64.encodeBase64URLSafeString(
				userInfo.getBytes(UTF_8));
			httpConnection.setRequestProperty("Authorization", basicAuth);
			httpConnection.setInstanceFollowRedirects(false);
		}
		return urlConnection;
	}

	/**
	 * Downloads a file from a URL.
	 *
	 * @param url
	 *            The URL to download the file from
	 * @param workingDirectory
	 *            The directory to output the file to
	 * @param defaultFilename
	 *            The name of the file to use if none can be worked out from the
	 *            URL or headers, or {@code null} to use a generated name
	 * @return The file downloaded
	 * @throws IOException
	 *          If anything goes wrong.
	 */
	public static File downloadFile(final URL url, final File workingDirectory,
			final String defaultFilename) throws IOException {
		requireNonNull(workingDirectory);

		// Open a connection
		var userInfo = url.getUserInfo();
		if (nonNull(userInfo)) {
			userInfo = URLDecoder.decode(url.getUserInfo(), UTF_8);
		}
		var urlConnection = createConnectionWithAuth(url, userInfo);

		if (urlConnection instanceof HttpURLConnection) {
			boolean redirect = false;
			do {
				redirect = false;
				var httpConnection = (HttpURLConnection) urlConnection;
				httpConnection.connect();
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
						|| responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
					var location = httpConnection.getHeaderField("Location");
					if (isNull(location)) {
						location = url.toString();
					}
					urlConnection = createConnectionWithAuth(
						new URL(location), userInfo);
					redirect = true;

				}
			} while (redirect);
		}

		// Work out the output filename
		final var output = getTargetFile(url, workingDirectory,
			defaultFilename, urlConnection);

		// Write the file
		copy(urlConnection.getInputStream(), output.toPath());

		return output;
	}

	/**
	 * Get the file to write to.
	 *
	 * @param url The URL of the file
	 * @param workingDirectory The directory to put the file in
	 * @param defaultFilename The default file name if nothing else can be used
	 * @param urlConnection The connection where the file has been downloaded
	 *     from
	 * @return The file to write to.
	 * @throws IOException If the file cannot be created
	 */
	private static File getTargetFile(final URL url,
			final File workingDirectory, final String defaultFilename,
			final URLConnection urlConnection) throws IOException {
		final var filename = getFileName(
				urlConnection.getHeaderField("Content-Disposition"));
		if (nonNull(filename)) {
			return new File(workingDirectory, filename);
		}
		if (nonNull(defaultFilename)) {
			return new File(workingDirectory, defaultFilename);
		}
		final var path = url.getPath();
		if (path.isEmpty()) {
			return createTempFile("download", "file", workingDirectory);
		}
		return new File(workingDirectory, new File(path).getName());
	}

	/**
	 * Downloads a file from a URL.
	 *
	 * @param url
	 *            The URL to download the file from
	 * @param workingDirectory
	 *            The directory to output the file to
	 * @param defaultFilename
	 *            The name of the file to use if none can be worked out from the
	 *            URL or headers, or {@code null} to use a generated name
	 * @return The file downloaded
	 * @throws IOException
	 *          If anything goes wrong.
	 */
	public static File downloadFile(final String url,
			final File workingDirectory, final String defaultFilename)
			throws IOException {
		return downloadFile(new URL(url), workingDirectory, defaultFilename);
	}
}
