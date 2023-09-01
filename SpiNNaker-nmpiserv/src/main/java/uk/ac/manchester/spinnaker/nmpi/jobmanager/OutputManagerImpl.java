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
package uk.ac.manchester.spinnaker.nmpi.jobmanager;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.Files.move;
import static java.nio.file.Files.probeContentType;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.utils.ThreadUtils.waitfor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import uk.ac.manchester.spinnaker.nmpi.model.job.nmpi.DataItem;
import uk.ac.manchester.spinnaker.nmpi.rest.OutputManager;
import uk.ac.manchester.spinnaker.nmpi.rest.UnicoreFileClient;

/**
 * Service for managing Job output files.
 */
//TODO needs security; Role = OutputHandler
public class OutputManagerImpl implements OutputManager {
	/** Indicates that a file has been removed. */
	private static final String PURGED_FILE = ".purged_";

	/** The directory to store files in. */
	@Value("${results.directory}")
	private File resultsDirectory;

	/** The URL of the server. */
	private final URL baseServerUrl;

	/** The amount of time results should be kept, in milliseconds. */
	private long timeToKeepResults;

	/** Map of locks for files. */
	private final Map<File, LockToken> synchronizers = new HashMap<>();

	/** The logger. */
	private static final Logger logger = getLogger(OutputManagerImpl.class);

	/**
	 * A lock token. Initially locked.
	 */
	private static final class LockToken {
		/** True if the token is locked. */
		private boolean locked = true;

		/** True if the token is waiting for a lock. */
		private boolean waiting = false;

		/**
		 * Wait until the token is unlocked.
		 */
		private synchronized void waitForUnlock() {
			waiting = true;

			// Wait until unlocked
			while (locked) {
				waitfor(this);
			}

			// Now lock again
			locked = true;
			waiting = false;
		}

		/**
		 * Unlock the token.
		 *
		 * @return True if the token is waiting again.
		 */
		private synchronized boolean unlock() {
			locked = false;
			notifyAll();
			return waiting;
		}
	}

	/**
	 * A class to lock a job.
	 */
	private class JobLock implements AutoCloseable {
		/** The directory being locked by this token. */
		private File dir;

		/**
		 * Create a new lock for a directory.
		 *
		 * @param dir
		 *            The directory to lock
		 */
		JobLock(final File dir) {
			this.dir = dir;

			LockToken lock;
			synchronized (synchronizers) {
				if (!synchronizers.containsKey(dir)) {
					// Constructed pre-locked
					synchronizers.put(dir, new LockToken());
					return;
				}
				lock = synchronizers.get(dir);
			}

			lock.waitForUnlock();
		}

		@Override
		public void close() {
			synchronized (synchronizers) {
				final var lock = synchronizers.get(dir);
				if (!lock.unlock()) {
					synchronizers.remove(dir);
				}
			}
		}
	}

	/**
	 * Instantiate the output manager.
	 *
	 * @param baseServerUrl
	 *            The base URL of the overall service, used when generating
	 *            internal URLs.
	 */
	public OutputManagerImpl(final URL baseServerUrl) {
		this.baseServerUrl = baseServerUrl;
	}

	/**
	 * Set the number of days after a job has finished to keep results.
	 *
	 * @param nDaysToKeepResults
	 *            The number of days to keep the results
	 */
	@Value("${results.purge.days}")
	void setPurgeTimeout(final long nDaysToKeepResults) {
		timeToKeepResults = MILLISECONDS.convert(nDaysToKeepResults, DAYS);
	}

	/** Periodic execution engine. */
	private final ScheduledExecutorService scheduler = newScheduledThreadPool(
			1);

	/**
	 * Arrange for old output to be purged once per day.
	 */
	@PostConstruct
	private void initPurgeScheduler() {
		scheduler.scheduleAtFixedRate(this::removeOldFiles, 0, 1, DAYS);
	}

	/**
	 * Stop the scheduler running jobs.
	 */
	@PreDestroy
	private void stopPurgeScheduler() {
		scheduler.shutdown();
	}

	/**
	 * Get the project directory for a given project.
	 *
	 * @param projectId
	 *            The id of the project
	 * @return The directory of the project
	 */
	private File getProjectDirectory(final String projectId) {
		if (isNull(projectId) || projectId.isEmpty()
				|| projectId.endsWith("/")) {
			throw new IllegalArgumentException("bad projectId");
		}
		final var name = new File(projectId).getName();
		if (name.equals(".") || name.equals("..") || name.isEmpty()) {
			throw new IllegalArgumentException("bad projectId");
		}
		return new File(resultsDirectory, name);
	}

	@Override
	public List<DataItem> addOutputs(final String projectId, final int id,
			final File baseDirectory, final Collection<File> outputs)
			throws IOException {
		if (isNull(outputs)) {
			return null;
		}

		final var pId = new File(projectId).getName();
		final int pathStart = baseDirectory.getAbsolutePath().length();
		final var projectDirectory = getProjectDirectory(projectId);
		final var idDirectory = new File(projectDirectory, String.valueOf(id));

		try (var op = new JobLock(idDirectory)) {
			final var outputData = new ArrayList<DataItem>();
			for (final var output : outputs) {
				if (!output.getAbsolutePath()
						.startsWith(baseDirectory.getAbsolutePath())) {
					throw new IOException("Output file " + output
							+ " is outside base directory " + baseDirectory);
				}

				var outputPath = output.getAbsolutePath()
						.substring(pathStart).replace('\\', '/');
				if (outputPath.startsWith("/")) {
					outputPath = outputPath.substring(1);
				}

				final var newOutput = new File(idDirectory, outputPath);
				newOutput.getParentFile().mkdirs();
				if (newOutput.exists()) {
					if (!newOutput.delete()) {
						logger.warn("Could not delete existing file {};"
								+ " new file will not be used!", newOutput);
					} else {
						logger.warn("Overwriting existing file {}", newOutput);
					}
				}
				if (!newOutput.exists()) {
					move(output.toPath(), newOutput.toPath());
				}
				final var outputUrl = new URL(baseServerUrl,
						"output/" + pId + "/" + id + "/" + outputPath);
				outputData.add(new DataItem(outputUrl.toExternalForm()));
				logger.debug("New output {} mapped to {}",
						newOutput, outputUrl);
			}

			return outputData;
		}
	}

	/**
	 * Get a file as a response to a query.
	 *
	 * @param idDirectory
	 *            The directory of the project
	 * @param filename
	 *            The name of the file to be stored
	 * @param download
	 *            True if the content type should be set to guarantee that the
	 *            file is downloaded, False to attempt to guess the content type
	 * @return The response
	 */
	private Response getResultFile(final File idDirectory,
			final String filename, final boolean download) {
		final var resultFile = new File(idDirectory, filename);
		final var purgeFile = getPurgeFile(idDirectory);

		try (var op = new JobLock(idDirectory)) {
			if (purgeFile.exists()) {
				logger.debug("{} was purged", idDirectory);
				return status(NOT_FOUND).entity("Results from job "
						+ idDirectory.getName() + " have been removed")
						.build();
			}

			if (!resultFile.canRead()) {
				logger.debug("{} was not found", resultFile);
				return status(NOT_FOUND).build();
			}

			try {
				if (!download) {
					final var contentType =
							probeContentType(resultFile.toPath());
					if (nonNull(contentType)) {
						logger.debug("File has content type {}", contentType);
						return ok(resultFile, contentType).build();
					}
				}
			} catch (final IOException e) {
				logger.debug("Content type of {} could not be determined",
						resultFile, e);
			}

			return ok(resultFile).header("Content-Disposition",
					"attachment; filename=" + filename).build();
		}
	}

	/**
	 * Get the file that marks a directory as purged.
	 *
	 * @param directory
	 *            The directory to find the file in
	 * @return The purge marker file
	 */
	private File getPurgeFile(final File directory) {
		return new File(resultsDirectory, PURGED_FILE + directory.getName());
	}

	@Override
	public Response getResultFile(final String projectId, final int id,
			final String filename, final boolean download) {
		logger.debug("Retrieving {} from {}/{}", filename, projectId, id);
		final var projectDirectory = getProjectDirectory(projectId);
		final var idDirectory = new File(projectDirectory, String.valueOf(id));
		return getResultFile(idDirectory, filename, download);
	}

	@Override
	public Response getResultFile(final int id, final String filename,
			final boolean download) {
		logger.debug("Retrieving {} from {}", filename, id);
		final var idDirectory = getProjectDirectory(String.valueOf(id));
		return getResultFile(idDirectory, filename, download);
	}

	/**
	 * Upload files in recursive subdirectories to UniCore.
	 *
	 * @param authHeader
	 *            The authentication to use
	 * @param directory
	 *            The directory to start from
	 * @param fileManager
	 *            The UniCore client
	 * @param storageId
	 *            The id of the UniCore storage
	 * @param filePath
	 *            The path in the UniCore storage to upload to
	 * @throws IOException
	 *             If something goes wrong
	 */
	private void recursivelyUploadFiles(final String authHeader,
			final File directory,
			final UnicoreFileClient fileManager, final String storageId,
			final String filePath) throws IOException {
		final var files = directory.listFiles();
		if (isNull(files)) {
			return;
		}
		for (final var file : files) {
			if (file.getName().equals(".") || file.getName().equals("..")
					|| file.getName().isEmpty()) {
				continue;
			}
			final var uploadFileName = filePath + "/" + file.getName();
			if (file.isDirectory()) {
				recursivelyUploadFiles(authHeader, file, fileManager, storageId,
						uploadFileName);
				continue;
			}
			if (!file.isFile()) {
				continue;
			}
			try (var input = new FileInputStream(file)) {
				fileManager.upload(authHeader, storageId, uploadFileName,
						input);
			} catch (final WebApplicationException e) {
				throw new IOException("Error uploading file to " + storageId
						+ "/" + uploadFileName, e);
			} catch (final FileNotFoundException e) {
				// Ignore files which vanish.
			}
		}
	}

	@Override
	public Response uploadResultsToHPCServer(final String projectId,
			final int id, final String serverUrl, final String storageId,
			final String filePath, final String userId, final String token) {
		final var idDirectory =
				new File(getProjectDirectory(projectId), String.valueOf(id));
		if (!idDirectory.canRead()) {
			logger.debug("{} was not found", idDirectory);
			return status(NOT_FOUND).build();
		}

		try {
			final var authHeader = "Bearer: " + token;
			final var fileClient = UnicoreFileClient.createClient(serverUrl);
			try (var op = new JobLock(idDirectory)) {
				recursivelyUploadFiles(authHeader, idDirectory, fileClient,
						storageId, filePath.replaceAll("/+$", ""));
			}
		} catch (final MalformedURLException e) {
			logger.error("bad user-supplied URL", e);
			return status(BAD_REQUEST)
					.entity("The URL specified was malformed").build();
		} catch (final Throwable e) {
			logger.error("failure in upload", e);
			return serverError()
					.entity("General error reading or uploading a file")
					.build();
		}

		return ok("ok").build();
	}

	/**
	 * Recursively remove a directory.
	 *
	 * @param directory
	 *            The directory to remove
	 */
	private void removeDirectory(final File directory) {
		for (final var file : directory.listFiles()) {
			if (file.isDirectory()) {
				removeDirectory(file);
			} else {
				file.delete();
			}
		}
		directory.delete();
	}

	/**
	 * Remove files that are deemed to have expired.
	 */
	private void removeOldFiles() {
		final long startTime = currentTimeMillis();
		for (final var projectDirectory : resultsDirectory.listFiles()) {
			if (projectDirectory.isDirectory()
					&& removeOldProjectDirectoryContents(startTime,
							projectDirectory)) {
				logger.info("No more outputs for project {}",
						projectDirectory.getName());
				projectDirectory.delete();
			}
		}
	}

	/**
	 * Remove project contents that are deemed to have expired.
	 *
	 * @param startTime
	 *            The current time being considered
	 * @param projectDirectory
	 *            The directory containing the project files
	 * @return True if every job in the project has been removed
	 */
	private boolean removeOldProjectDirectoryContents(final long startTime,
			final File projectDirectory) {
		boolean allJobsRemoved = true;
		for (final var jobDirectory : projectDirectory.listFiles()) {
			logger.debug("Determining whether to remove {} "
					+ "which is {}ms old of {}", jobDirectory,
					startTime - jobDirectory.lastModified(),
					timeToKeepResults);
			if (jobDirectory.isDirectory() && ((startTime
					- jobDirectory.lastModified()) > timeToKeepResults)) {
				logger.info("Removing results for job {}",
						jobDirectory.getName());
				try (var op = new JobLock(jobDirectory)) {
					removeDirectory(jobDirectory);
				}

				try (var purgedFileWriter =
						new PrintWriter(getPurgeFile(jobDirectory))) {
					purgedFileWriter.println(currentTimeMillis());
				} catch (final IOException e) {
					logger.error("Error writing purge file", e);
				}
			} else {
				allJobsRemoved = false;
			}
		}
		return allJobsRemoved;
	}
}
