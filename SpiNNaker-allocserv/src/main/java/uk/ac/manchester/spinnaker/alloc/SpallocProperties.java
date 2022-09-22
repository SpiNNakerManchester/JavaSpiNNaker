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
package uk.ac.manchester.spinnaker.alloc;

import static java.util.Objects.nonNull;

import java.io.File;
import java.time.Duration;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

import com.google.errorprone.annotations.Keep;

import uk.ac.manchester.spinnaker.alloc.model.IPAddress;

/**
 * Spalloc service management properties. These are all intended to be set via
 * the configuration file.
 *
 * @author Donal Fellows
 */
@ConfigurationProperties("spalloc")
@ConstructorBinding
@Validated
public class SpallocProperties {
	/** Path to the main database file. */
	private File databasePath;

	/** How long should long calls take before returning anyway? */
	private Duration wait;

	/**
	 * Whether to pause <em>all</em> periodic callbacks. Probably only useful
	 * for debugging and testing.
	 */
	private boolean pause;

	/** The main working directory. Referred to by other properties. */
	private File workingDirectory;

	private HistoricalDataProperties historicalData;

	private KeepaliveProperties keepalive;

	private AllocatorProperties allocator;

	private AuthProperties auth;

	private QuotaProperties quota;

	private TxrxProperties transceiver;

	private DBProperties sqlite;

	private CompatibilityProperties compat;

	private ProxyProperties proxy;

	/** Properties relating to board issue reporting. */
	private ReportProperties reportEmail;

	/** Properties relating to control of the overall machine state. */
	private StateControlProperties stateCtrl;

	/**
	 * @param databasePath
	 *            Path to the main database file.
	 * @param wait
	 *            How long should long calls take before returning anyway?
	 * @param pause
	 *            Whether to pause <em>all</em> periodic callbacks. Probably
	 *            only useful for debugging and testing.
	 * @param workingDirectory
	 *            The main working directory. Referred to by other properties.
	 * @param allocator
	 *            Properties relating to the allocation engine.
	 * @param auth
	 *            Properties relating to authentication and authorization.
	 * @param compat
	 *            Properties relating to the Spalloc v1 compatibility layer.
	 * @param historicalData
	 *            Properties relating to historical data management.
	 * @param keepalive
	 *            Properties relating to job keep-alive messages.
	 * @param proxy
	 *            Properties relating to the SDP proxying.
	 * @param quota
	 *            Properties relating to quota management.
	 * @param sqlite
	 *            Properties relating to the details of working with SQLite.
	 * @param reportEmail
	 *            Properties relating to board issue reporting.
	 * @param transceiver
	 *            Properties relating to low-level transceiver control and the
	 *            BMPs.
	 * @param stateControl
	 *            Properties relating to control of the overall machine state.
	 */
	public SpallocProperties(//
			@DefaultValue("spalloc.sqlite3") File databasePath,
			@DefaultValue("30s") Duration wait,
			@DefaultValue("false") boolean pause,
			@DefaultValue(".") File workingDirectory,
			@DefaultValue AllocatorProperties allocator,
			@DefaultValue AuthProperties auth,
			@DefaultValue CompatibilityProperties compat,
			@DefaultValue HistoricalDataProperties historicalData,
			@DefaultValue KeepaliveProperties keepalive,
			@DefaultValue ProxyProperties proxy,
			@DefaultValue QuotaProperties quota,
			@DefaultValue DBProperties sqlite,
			@DefaultValue ReportProperties reportEmail,
			@DefaultValue TxrxProperties transceiver,
			@DefaultValue StateControlProperties stateControl) {
		this.databasePath = databasePath;
		this.wait = wait;
		this.pause = pause;
		this.workingDirectory = workingDirectory;
		this.allocator = allocator;
		this.auth = auth;
		this.compat = compat;
		this.historicalData = historicalData;
		this.keepalive = keepalive;
		this.proxy = proxy;
		this.quota = quota;
		this.sqlite = sqlite;
		this.reportEmail = reportEmail;
		this.transceiver = transceiver;
		this.stateCtrl = stateControl;
	}

	/**
	 * Path to the main database file.
	 *
	 * @return Path.
	 */
	@NotNull
	public File getDatabasePath() {
		return databasePath;
	}

	void setDatabasePath(File databasePath) {
		this.databasePath = databasePath;
	}

	@Keep
	@AssertTrue(message = "directory of database path must exist")
	private boolean isDatabaseInSaneLocation() {
		return databasePath.getAbsoluteFile().getParentFile().exists();
	}

	/**
	 * How long should long calls take before returning anyway?
	 *
	 * @return Wait time.
	 */
	@NotNull
	public Duration getWait() {
		return wait;
	}

	void setWait(Duration wait) {
		this.wait = wait;
	}

	/**
	 * Whether to pause <em>all</em> periodic callbacks. Probably only useful
	 * for debugging.
	 *
	 * @return Whether to pause periodic callbacks. Note that this property is
	 *         runtime-settable via the global administration interface, which
	 *         should be the only place that reads from here directly.
	 */
	public boolean isPause() {
		return pause;
	}

	void setPause(boolean pause) {
		this.pause = pause;
	}

	/**
	 * The main working directory. Referred to by other properties.
	 *
	 * @return Main working directory.
	 */
	@NotNull
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	void setWorkingDirectory(File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

	@Keep
	@AssertTrue(message = "working directory must exist")
	private boolean isValidWorkingDirectory() {
		return workingDirectory.exists() && workingDirectory.isDirectory();
	}

	/**
	 * @return Properties relating to historical data management.
	 */
	@NotNull
	@Valid
	public HistoricalDataProperties getHistoricalData() {
		return historicalData;
	}

	void setHistoricalData(HistoricalDataProperties historicalData) {
		this.historicalData = historicalData;
	}

	/**
	 * @return Properties relating to job keep-alive messages.
	 */
	@NotNull
	@Valid
	public KeepaliveProperties getKeepalive() {
		return keepalive;
	}

	void setKeepalive(KeepaliveProperties keepalive) {
		this.keepalive = keepalive;
	}

	/**
	 * @return Properties relating to the allocation engine.
	 */
	@NotNull
	@Valid
	public AllocatorProperties getAllocator() {
		return allocator;
	}

	void setAllocator(AllocatorProperties allocator) {
		this.allocator = allocator;
	}

	/**
	 * @return Properties relating to authentication and authorization.
	 */
	@NotNull
	@Valid
	public AuthProperties getAuth() {
		return auth;
	}

	void setAuth(AuthProperties auth) {
		this.auth = auth;
	}

	/**
	 * @return Properties relating to the SDP proxying.
	 */
	@NotNull
	@Valid
	public ProxyProperties getProxy() {
		return proxy;
	}

	void setProxy(ProxyProperties proxy) {
		this.proxy = proxy;
	}

	/**
	 * @return Properties relating to quota management.
	 */
	@NotNull
	@Valid
	public QuotaProperties getQuota() {
		return quota;
	}

	void setQuota(QuotaProperties quota) {
		this.quota = quota;
	}

	/**
	 * @return Properties relating to low-level transceiver control and the
	 *         BMPs.
	 */
	@NotNull
	@Valid
	public TxrxProperties getTransceiver() {
		return transceiver;
	}

	void setTransceiver(TxrxProperties transceiver) {
		this.transceiver = transceiver;
	}

	/**
	 * @return Properties relating to the details of working with SQLite.
	 */
	@NotNull
	@Valid
	public DBProperties getSqlite() {
		return sqlite;
	}

	void setSqlite(DBProperties sqlite) {
		this.sqlite = sqlite;
	}

	/** @return Properties relating to board issue reporting. */
	@NotNull
	@Valid
	public ReportProperties getReportEmail() {
		return reportEmail;
	}

	void setReportEmail(ReportProperties reportEmail) {
		this.reportEmail = reportEmail;
	}

	/**
	 * @return Properties relating to the Spalloc v1 compatibility layer.
	 */
	@NotNull
	@Valid
	public CompatibilityProperties getCompat() {
		return compat;
	}

	void setCompat(CompatibilityProperties compat) {
		this.compat = compat;
	}

	/** @return Properties relating to control of the overall machine state. */
	@NotNull
	@Valid
	public StateControlProperties getStateControl() {
		return stateCtrl;
	}

	void setStateControl(StateControlProperties stateCtrl) {
		this.stateCtrl = stateCtrl;
	}

	/** How to handle job data that is now only of historic interest. */
	public static class HistoricalDataProperties {
		/**
		 * Path to the historical job database file. <em>Must be distinct from
		 * the main database!</em>
		 */
		private File path;

		/**
		 * How long after job completion should we wait before moving the data?
		 */
		private Duration gracePeriod;

		/**
		 * Cron expression saying when to move completed jobs to the historical
		 * database.
		 */
		private String schedule;

		/**
		 * @param path
		 *            Path to the historical job database file. <em>Must be
		 *            distinct from the main database!</em>
		 * @param gracePeriod
		 *            How long after job completion should we wait before moving
		 *            the data?
		 * @param schedule
		 *            Cron expression saying when to move completed jobs to the
		 *            historical database.
		 */
		public HistoricalDataProperties(
				@DefaultValue("spalloc-history.sqlite3") File path,
				@DefaultValue("14d") Duration gracePeriod,
				@DefaultValue("0 0 2 * * *") String schedule) {
			this.path = path;
			this.gracePeriod = gracePeriod;
			this.schedule = schedule;
		}

		/**
		 * @return Path to the historical job database file. <em>Must be
		 *         distinct from the main database!</em>
		 */
		@NotNull
		public File getPath() {
			return path;
		}

		void setPath(File path) {
			this.path = path;
		}

		/**
		 * @return How long after job completion should we wait before moving
		 *         the data?
		 */
		@NotNull
		public Duration getGracePeriod() {
			return gracePeriod;
		}

		void setGracePeriod(Duration gracePeriod) {
			this.gracePeriod = gracePeriod;
		}

		/**
		 * @return Cron expression saying when to move completed jobs to the
		 *         historical database.
		 */
		@NotBlank
		public String getSchedule() {
			return schedule;
		}

		void setSchedule(String schedule) {
			this.schedule = schedule;
		}
	}

	/** How to handle keepalive messages. */
	public static class KeepaliveProperties {
		/**
		 * Time between runs of the keepalive-expiry algorithm.
		 */
		private Duration expiryPeriod;

		/** Minimum keepalive period. */
		private Duration min;

		/** Maximum keepalive period. */
		private Duration max;

		/**
		 * @param expiryPeriod
		 *            Time between runs of the keepalive-expiry algorithm.
		 * @param min
		 *            Minimum keepalive period.
		 * @param max
		 *            Maximum keepalive period.
		 */
		public KeepaliveProperties(//
				@DefaultValue("30s") Duration expiryPeriod,
				@DefaultValue("30s") Duration min,
				@DefaultValue("300s") Duration max) {
			this.expiryPeriod = expiryPeriod;
			this.min = min;
			this.max = max;
		}

		/**
		 * Time between runs of the keepalive-expiry algorithm.
		 *
		 * @return Time between runs of the keepalive-expiry algorithm.
		 */
		@NotNull
		public Duration getExpiryPeriod() {
			return expiryPeriod;
		}

		void setExpiryPeriod(Duration expiryPeriod) {
			this.expiryPeriod = expiryPeriod;
		}

		/** @return Minimum keepalive period. */
		@NotNull
		public Duration getMin() {
			return min;
		}

		void setMin(Duration min) {
			this.min = min;
		}

		/** @return Maximum keepalive period. */
		@NotNull
		public Duration getMax() {
			return max;
		}

		void setMax(Duration max) {
			this.max = max;
		}

		@Keep
		@AssertTrue(message = "max must be more than min")
		private boolean isMaxMoreThanMin() {
			return max.compareTo(min) > 0;
		}
	}

	/** Configuration of the main resource allocation engine. */
	public static class AllocatorProperties {
		/**
		 * Time between runs of the main allocation algorithm.
		 */
		private Duration period;

		/**
		 * Maximum span of job importance that will be allocated at once.
		 * Priority is the rate at which importance is accrued.
		 */
		private int importanceSpan;

		/** Properties relating to job priority scaling. */
		private PriorityScale priorityScale;

		/**
		 * Number of reports of board problems at which the board is taken out
		 * of service.
		 */
		private int reportActionThreshold;

		/** Name of user that system-generated reports are done by. */
		private String systemReportUser;

		/**
		 * @param period
		 *            Time between runs of the main allocation algorithm.
		 * @param importanceSpan
		 *            Maximum span of job importance that will be allocated at
		 *            once. Priority is the rate at which importance is accrued.
		 * @param priorityScale
		 *            Properties relating to job priority scaling.
		 * @param reportActionThreshold
		 *            Number of reports of board problems at which the board is
		 *            taken out of service.
		 * @param systemReportUser
		 *            Name of user that system-generated reports are done by.
		 */
		public AllocatorProperties(@DefaultValue("5s") Duration period,
				@DefaultValue("10000") int importanceSpan,
				@DefaultValue PriorityScale priorityScale,
				@DefaultValue("2") int reportActionThreshold,
				@DefaultValue("") String systemReportUser) {
			this.period = period;
			this.importanceSpan = importanceSpan;
			this.priorityScale = priorityScale;
			this.reportActionThreshold = reportActionThreshold;
			this.systemReportUser = systemReportUser;
		}

		/**
		 * Time between runs of the main allocation algorithm.
		 *
		 * @return Time between runs of the main allocation algorithm.
		 */
		@NotNull
		public Duration getPeriod() {
			return period;
		}

		void setPeriod(Duration period) {
			this.period = period;
		}

		/**
		 * Maximum span of job importance that will be allocated at once.
		 * Priority is the rate at which importance is accrued.
		 *
		 * @return Maximum importance span for considered jobs for a particular
		 *         run of the allocator algorithm.
		 */
		@Positive
		public int getImportanceSpan() {
			return importanceSpan;
		}

		void setImportanceSpan(int importanceSpan) {
			this.importanceSpan = importanceSpan;
		}

		/**
		 * Properties relating to job priority scaling.
		 *
		 * @return Properties relating to job priority scaling.
		 */
		@NotNull
		@Valid
		public PriorityScale getPriorityScale() {
			return priorityScale;
		}

		void setPriorityScale(PriorityScale priorityScale) {
			this.priorityScale = priorityScale;
		}

		/**
		 * @return Number of reports of board problems at which the board is
		 *         taken out of service.
		 */
		@Positive
		public int getReportActionThreshold() {
			return reportActionThreshold;
		}

		void setReportActionThreshold(int threshold) {
			reportActionThreshold = threshold;
		}

		/** @return Name of user that system-generated reports are done by. */
		@NotNull
		public String getSystemReportUser() {
			return systemReportUser;
		}

		void setSystemReportUser(String systemReportUser) {
			this.systemReportUser = systemReportUser;
		}
	}

	/**
	 * Priority is the rate at which importance is accrued. Importance is
	 * determines the order in which jobs are allocated.
	 */
	public static class PriorityScale {
		/** Priority scaling factor for jobs given by number of boards. */
		private double size;

		/** Priority scaling factor for jobs given by rectangular dimensions. */
		private double dimensions;

		/** Priority scaling factor for jobs requiring a specific board. */
		private double specificBoard;

		/**
		 * @param size
		 *            Priority scaling factor for jobs given by number of
		 *            boards.
		 * @param dimensions
		 *            Priority scaling factor for jobs given by rectangular
		 *            dimensions.
		 * @param specificBoard
		 *            Priority scaling factor for jobs requiring a specific
		 *            board.
		 */
		public PriorityScale(@DefaultValue("1.0") double size,
				@DefaultValue("1.5") double dimensions,
				@DefaultValue("65.0") double specificBoard) {
			this.size = size;
			this.dimensions = dimensions;
			this.specificBoard = specificBoard;
		}

		/**
		 * @return Priority scaling factor for jobs given by number of boards.
		 */
		@Positive
		public double getSize() {
			return size;
		}

		void setSize(double factor) {
			size = factor;
		}

		/**
		 * @return Priority scaling factor for jobs given by rectangular
		 *         dimensions.
		 */
		@Positive
		public double getDimensions() {
			return dimensions;
		}

		void setDimensions(double factor) {
			dimensions = factor;
		}

		/**
		 * @return Priority scaling factor for jobs requiring a specific board.
		 */
		@Positive
		public double getSpecificBoard() {
			return specificBoard;
		}

		void setSpecificBoard(double factor) {
			specificBoard = factor;
		}
	}

	/** Notify an administrator about problems reported with boards. */
	public static class ReportProperties {
		private static final String DEFAULT_SUBJECT =
				"NOTICE: Board taken out of service";

		/** Whether to send an email about reported boards. */
		private boolean send;

		/** The {@code From:} email address. */
		private String from;

		/** The {@code To:} email address. */
		private String to;

		/** The {@code Subject:} header. */
		private String subject;

		/**
		 * @param send
		 *            Whether to send an email about reported boards.
		 * @param from
		 *            The {@code From:} email address.
		 * @param to
		 *            The {@code To:} email address.
		 * @param subject
		 *            The {@code Subject:} header.
		 */
		public ReportProperties(@DefaultValue("false") boolean send,
				@DefaultValue("spalloc@localhost") String from,
				@DefaultValue("root@localhost") String to,
				@DefaultValue(DEFAULT_SUBJECT) String subject) {
			this.send = send;
			this.from = from;
			this.to = to;
			this.subject = subject;
		}

		/** @return Whether to send an email about reported boards. */
		public boolean isSend() {
			return send;
		}

		void setSend(boolean send) {
			this.send = send;
		}

		/** @return The {@code From:} email address. */
		@Email
		public String getFrom() {
			return from;
		}

		void setFrom(String address) {
			from = address;
		}

		/** @return The {@code To:} email address. */
		@Email
		public String getTo() {
			return to;
		}

		void setTo(String address) {
			to = address;
		}

		/** @return The {@code Subject:} header. */
		@NotBlank
		public String getSubject() {
			return subject;
		}

		void setSubject(String subject) {
			this.subject = subject;
		}

		@Keep
		@AssertTrue(
				message = "must supply from, to, and subject if send enabled")
		private boolean fieldsIfEnabled() {
			return !send || (nonNull(from) && nonNull(to) && nonNull(subject));
		}
	}

	/** Authentication and authorization configuration. */
	public static class AuthProperties {
		/**
		 * Whether to enable HTTP BASIC authentication. Useful for simple
		 * clients.
		 */
		private boolean basic;

		/**
		 * The authentication realm. Must not contain quote characters!
		 */
		private String realm;

		/**
		 * Whether to enable HTTP form+session authentication. Much faster than
		 * BASIC, but requires a more complex client. You must enable this if
		 * you are supporting the Web UI.
		 */
		private boolean localForm;

		/**
		 * Force a known local admin user to exist with a known (by default)
		 * password.
		 */
		private boolean addDummyUser;

		/**
		 * Whether to generate a random password for the above user. If so, the
		 * password will be written to the log.
		 */
		private boolean dummyRandomPass;

		/**
		 * The name of the system default group. Only made if the
		 * {@linkplain #addDummyUser dummy user} is made.
		 */
		private String systemGroup;

		/** Provide extra information to callers on auth failures. */
		private boolean debugFailures;

		/** Number of login failures before automatic lock-out. */
		private int maxLoginFailures;

		/** Length of time that automatic lock-out lasts. */
		private Duration accountLockDuration;

		/**
		 * How often do we look for users to end their lock-out?
		 */
		private Duration unlockPeriod;

		/**
		 * OpenID-related security properties. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 */
		private OpenIDProperties openid;

		/**
		 * @param basic
		 *            Whether to enable HTTP BASIC authentication. Useful for
		 *            simple clients.
		 * @param realm
		 *            The authentication realm. Must not contain quote
		 *            characters!
		 * @param localForm
		 *            Whether to enable HTTP form+session authentication. Much
		 *            faster than BASIC, but requires a more complex client. You
		 *            must enable this if you are supporting the Web UI.
		 * @param addDummyUser
		 *            Force a known local admin user to exist with a known (by
		 *            default) password.
		 * @param dummyRandomPass
		 *            Whether to generate a random password for the above user.
		 *            If so, the password will be written to the log.
		 * @param systemGroup
		 *            The name of the system default group. Only made if the
		 *            dummy user is made.
		 * @param debugFailures
		 *            Provide extra information to callers on auth failures.
		 * @param maxLoginFailures
		 *            Number of login failures before automatic lock-out.
		 * @param accountLockDuration
		 *            Length of time that automatic lock-out lasts.
		 * @param unlockPeriod
		 *            How often do we look for users to end their lock-out?
		 * @param openid
		 *            OpenID-related security properties. Required for allowing
		 *            people to use HBP/EBRAINS identities.
		 */
		public AuthProperties(//
				@DefaultValue("true") boolean basic,
				@DefaultValue("SpallocService") String realm,
				@DefaultValue("true") boolean localForm,
				@DefaultValue("false") boolean addDummyUser,
				@DefaultValue("true") boolean dummyRandomPass,
				@DefaultValue("wheel") String systemGroup,
				@DefaultValue("false") boolean debugFailures,
				@DefaultValue("3") int maxLoginFailures,
				@DefaultValue("24h") Duration accountLockDuration,
				@DefaultValue("60s") Duration unlockPeriod,
				@DefaultValue OpenIDProperties openid) {
			this.basic = basic;
			this.realm = realm;
			this.localForm = localForm;
			this.addDummyUser = addDummyUser;
			this.dummyRandomPass = dummyRandomPass;
			this.systemGroup = systemGroup;
			this.debugFailures = debugFailures;
			this.maxLoginFailures = maxLoginFailures;
			this.accountLockDuration = accountLockDuration;
			this.unlockPeriod = unlockPeriod;
			this.setOpenid(openid);
		}

		/**
		 * Whether to enable HTTP BASIC authentication. Useful for simple
		 * clients; not great with browsers.
		 *
		 * @return Whether to enable HTTP BASIC authentication.
		 */
		public boolean isBasic() {
			return basic;
		}

		void setBasic(boolean basic) {
			this.basic = basic;
		}

		/**
		 * The authentication realm. Must not contain quote characters!
		 *
		 * @return the realm.
		 */
		@NotNull
		public String getRealm() {
			return realm;
		}

		void setRealm(String realm) {
			this.realm = realm;
		}

		/**
		 * Whether to enable HTTP form+session authentication. Much faster than
		 * BASIC, but requires a more complex client. You must enable this if
		 * you are supporting the Web UI.
		 *
		 * @return Whether to enable HTTP form+session authentication.
		 */
		public boolean isLocalForm() {
			return localForm;
		}

		void setLocalForm(boolean localForm) {
			this.localForm = localForm;
		}

		/**
		 * @return Force a known local admin user to exist with a known
		 *         password.
		 */
		public boolean isAddDummyUser() {
			return addDummyUser;
		}

		void setAddDummyUser(boolean addDummyUser) {
			this.addDummyUser = addDummyUser;
		}

		/**
		 * Whether to generate a random password for the default admin user. If
		 * so, the password will be written to the log.
		 *
		 * @return Whether to generate a random password for the default admin
		 *         user.
		 */
		public boolean isDummyRandomPass() {
			return dummyRandomPass;
		}

		void setDummyRandomPass(boolean dummyRandomPass) {
			this.dummyRandomPass = dummyRandomPass;
		}

		/**
		 * The name of the system default group, that is internal and has no
		 * quota (initially). Only made if the {@linkplain #addDummyUser dummy
		 * user} is made.
		 *
		 * @return the name of the system group
		 */
		public String getSystemGroup() {
			return systemGroup;
		}

		void setSystemGroup(String systemGroup) {
			this.systemGroup = systemGroup;
		}

		/** @return Provide extra information to callers on auth failures. */
		public boolean isDebugFailures() {
			return debugFailures;
		}

		void setDebugFailures(boolean debugFailures) {
			this.debugFailures = debugFailures;
		}

		/** @return Number of login failures before automatic lock-out. */
		@Positive
		public int getMaxLoginFailures() {
			return maxLoginFailures;
		}

		void setMaxLoginFailures(int maxLoginFailures) {
			this.maxLoginFailures = maxLoginFailures;
		}

		/** @return Length of time that automatic lock-out lasts. */
		@NotNull
		public Duration getAccountLockDuration() {
			return accountLockDuration;
		}

		void setAccountLockDuration(Duration accountLockDuration) {
			this.accountLockDuration = accountLockDuration;
		}

		/**
		 * How often do we look for users to end their lock-out?
		 *
		 * @return How often do we look for users to end their lock-out?
		 */
		@NotNull
		public Duration getUnlockPeriod() {
			return unlockPeriod;
		}

		void setUnlockPeriod(Duration unlockPeriod) {
			this.unlockPeriod = unlockPeriod;
		}

		/**
		 * OpenID-related security properties. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 *
		 * @return OpenID-related security properties.
		 */
		@NotNull
		@Valid
		public OpenIDProperties getOpenid() {
			return openid;
		}

		void setOpenid(OpenIDProperties openid) {
			this.openid = openid;
		}
	}

	/**
	 * OpenID-related security properties. Required for allowing people to use
	 * HBP/EBRAINS identities.
	 */
	public static class OpenIDProperties {
		/**
		 * Whether to enable OIDC authentication. Required for allowing people
		 * to use HBP/EBRAINS identities.
		 */
		private boolean enable;

		/**
		 * The root path of the OpenID 2 Discovery domain. Referred to elsewhere
		 * in the configuration file.
		 */
		private String domain;

		/**
		 * The scopes desired. Referred to elsewhere in the configuration file.
		 */
		private Set<String> scopes;

		/**
		 * The application installation identity. Required for allowing people
		 * to use HBP/EBRAINS identities.
		 */
		private String id;

		/**
		 * The application installation secret. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 */
		private String secret;

		/** Prefix for user names originating from OpenID auto-registration. */
		private String usernamePrefix;

		/** What kind of truststore is it. */
		private String truststoreType;

		/** Where the truststore is. */
		private Resource truststorePath;

		/** How to unlock the truststore. */
		private String truststorePassword;

		/**
		 * @param enable
		 *            Whether to enable OIDC authentication. Required for
		 *            allowing people to use HBP/EBRAINS identities.
		 * @param domain
		 *            The root path of the OpenID 2 Discovery domain. Referred
		 *            to elsewhere in the configuration file.
		 * @param scopes
		 *            The scopes desired. Referred to elsewhere in the
		 *            configuration file.
		 * @param id
		 *            The application installation identity. Required for
		 *            allowing people to use HBP/EBRAINS identities.
		 * @param secret
		 *            The application installation secret. Required for allowing
		 *            people to use HBP/EBRAINS identities.
		 * @param usernamePrefix
		 *            Prefix for user names originating from OpenID
		 *            auto-registration.
		 * @param truststoreType
		 *            What kind of truststore is it.
		 * @param truststorePath
		 *            Where the truststore is.
		 * @param truststorePassword
		 *            How to unlock the truststore.
		 */
		public OpenIDProperties(@DefaultValue("false") boolean enable,
				@DefaultValue("") String domain, //
				Set<String> scopes, //
				@DefaultValue("") String id, //
				@DefaultValue("") String secret,
				@DefaultValue("openid.") String usernamePrefix,
				@DefaultValue("PKCS12") String truststoreType,
				@DefaultValue("classpath:/truststore.p12") //
				Resource truststorePath,
				@DefaultValue("") String truststorePassword) {
			this.enable = enable;
			this.domain = domain;
			this.setScopes(scopes != null ? scopes : Set.of());
			this.id = id;
			this.secret = secret;
			this.usernamePrefix = usernamePrefix;
			this.truststoreType = truststoreType;
			this.truststorePath = truststorePath;
			this.truststorePassword = truststorePassword;
		}

		/**
		 * Whether to enable OIDC authentication. Required for allowing people
		 * to use HBP/EBRAINS identities.
		 *
		 * @return Whether to enable OIDC authentication.
		 */
		public boolean isEnable() {
			return enable;
		}

		void setEnable(boolean enable) {
			this.enable = enable;
		}

		/**
		 * The application installation identity. Required for allowing people
		 * to use HBP/EBRAINS identities.
		 *
		 * @return The application installation identity.
		 */
		public String getId() {
			return id == null ? null : id.strip();
		}

		void setId(String id) {
			this.id = id;
		}

		/**
		 * The application installation secret. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 *
		 * @return The application installation secret.
		 */
		public String getSecret() {
			return secret == null ? null : secret.strip();
		}

		void setSecret(String secret) {
			this.secret = secret;
		}

		/**
		 * Prefix for user names originating from OpenID auto-registration. Not
		 * a good idea to modify this frequently!
		 *
		 * @return Prefix for user names originating from OpenID
		 *         auto-registration.
		 */
		@NotNull
		public String getUsernamePrefix() {
			return usernamePrefix;
		}

		void setUsernamePrefix(String usernamePrefix) {
			this.usernamePrefix = usernamePrefix;
		}

		/**
		 * The root path of the OpenID 2 Discovery domain. Referred to elsewhere
		 * in the configuration file.
		 *
		 * @return The root path of the OpenID 2 Discovery domain.
		 */
		@NotNull
		public String getDomain() {
			return domain;
		}

		void setDomain(String domain) {
			this.domain = domain;
		}

		/**
		 * The scopes desired. Referred to elsewhere in the configuration file.
		 *
		 * @return The OpenID scopes.
		 */
		@NotEmpty
		public Set<String> getScopes() {
			return scopes;
		}

		void setScopes(Set<String> scopes) {
			this.scopes = scopes;
		}

		/**
		 * What kind of truststore is it.
		 *
		 * @return truststore type (default: {@code PKCS12})
		 */
		@NotNull
		public String getTruststoreType() {
			return truststoreType;
		}

		void setTruststoreType(String truststoreType) {
			this.truststoreType = truststoreType;
		}

		/**
		 * Where the truststore is.
		 *
		 * @return truststore location
		 */
		@NotNull
		public Resource getTruststorePath() {
			return truststorePath;
		}

		void setTruststorePath(Resource truststorePath) {
			this.truststorePath = truststorePath;
		}

		/**
		 * How to unlock the truststore. This is not considered to be actually
		 * secret, but rather just a technical requirement of the truststore
		 * format.
		 *
		 * @return password for truststore
		 */
		@NotNull
		public String getTruststorePassword() {
			return truststorePassword;
		}

		void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

		@Keep
		@AssertTrue(
				message = "id and secret must be given if OpenID is enabled")
		private boolean isValid() {
			return !enable || (nonNull(id) && !id.isBlank()
					&& nonNull(secret) && !secret.isBlank());
		}
	}

	/** Quota management. */
	public static class QuotaProperties {
		/** Default user quota in board-seconds. */
		private int defaultQuota;

		/**
		 * Default quota for organisations inflated from OpenID, in
		 * board-seconds.
		 */
		private long orgQuota;

		/**
		 * Default quota for collabratories inflated from OpenID, in
		 * board-seconds.
		 */
		private long collabQuota;

		/**
		 * Cron expression that says when we consolidate job quotas into the
		 * main quota table.
		 */
		private String consolidationSchedule;

		/**
		 * @param defaultQuota
		 *            Default user quota in board-seconds.
		 * @param defaultOrgQuota
		 *            Default quota for organisations inflated from OpenID, in
		 *            board-seconds.
		 * @param defaultCollabQuota
		 *            Default quota for collabratories inflated from OpenID, in
		 *            board-seconds.
		 * @param consolidationSchedule
		 *            Cron expression that says when we consolidate job quotas
		 *            into the main quota table.
		 */
		public QuotaProperties(@DefaultValue("100") int defaultQuota,
				@DefaultValue("0") long defaultOrgQuota,
				@DefaultValue("3600000") long defaultCollabQuota,
				@DefaultValue("0 0 * * * *") String consolidationSchedule) {
			this.defaultQuota = defaultQuota;
			this.orgQuota = defaultOrgQuota;
			this.collabQuota = defaultCollabQuota;
			this.consolidationSchedule = consolidationSchedule;
		}

		/**
		 * Default user quota in board-seconds.
		 *
		 * @return Default user quota in board-seconds.
		 */
		@PositiveOrZero
		public int getDefaultQuota() {
			return defaultQuota;
		}

		void setDefaultQuota(int defaultQuota) {
			this.defaultQuota = defaultQuota;
		}

		/**
		 * Default quota for organisations.
		 *
		 * @return Default quota for organisations inflated from OpenID, in
		 *         board-seconds.
		 */
		public long getDefaultOrgQuota() {
			return orgQuota;
		}

		void setDefaultOrgQuota(long orgQuota) {
			this.orgQuota = orgQuota;
		}

		/**
		 * Default quota for collabratories.
		 *
		 * @return Default quota for collabratories inflated from OpenID, in
		 *         board-seconds.
		 */
		public long getDefaultCollabQuota() {
			return collabQuota;
		}

		void setDefaultCollabQuota(long collabQuota) {
			this.collabQuota = collabQuota;
		}

		/**
		 * @return Cron expression that says when we consolidate job quotas into
		 *         the main quota table.
		 */
		@NotBlank
		public String getConsolidationSchedule() {
			return consolidationSchedule;
		}

		void setConsolidationSchedule(String consolidationSchedule) {
			this.consolidationSchedule = consolidationSchedule;
		}
	}

	/** Controls how Spalloc talks to BMPs on machines. */
	public static class TxrxProperties {
		/**
		 * How long between when we send requests to the BMP control tasks.
		 */
		private Duration period;

		/** The basic wait time used by the BMP control tasks. */
		private Duration probeInterval;

		/** Number of attempts that will be made to switch on a board. */
		private int powerAttempts;

		/** Number of attempts that will be made to bring up an FPGA. */
		private int fpgaAttempts;

		/**
		 * Whether to reload the FPGA bitfiles when they don't come up
		 * correctly.
		 */
		private boolean fpgaReload;

		/**
		 * Number of attempts that will be made to bring up a transceiver,
		 * provided the failures are due to timeouts and not outright network
		 * errors. Note that a failure to bring up a transceiver is lethal to
		 * the service.
		 */
		private int buildAttempts;

		/** Whether to use a dummy transceiver. Useful for testing only. */
		private boolean dummy;

		/**
		 * @param period
		 *            How long between when we send requests to the BMP control
		 *            tasks.
		 * @param probeInterval
		 *            The basic wait time used by the BMP control tasks.
		 * @param powerAttempts
		 *            Number of attempts that will be made to switch on a board.
		 * @param fpgaAttempts
		 *            Number of attempts that will be made to bring up an FPGA.
		 * @param fpgaReload
		 *            Whether to reload the FPGA bitfiles when they don't come
		 *            up correctly.
		 * @param buildAttempts
		 *            Number of attempts that will be made to bring up a
		 *            transceiver, provided the failures are due to timeouts and
		 *            not outright network errors. Note that a failure to bring
		 *            up a transceiver is lethal to the service.
		 * @param dummy
		 *            Whether to use a dummy transceiver. Useful for testing
		 *            only.
		 */
		public TxrxProperties(@DefaultValue("10s") Duration period,
				@DefaultValue("15s") Duration probeInterval,
				@DefaultValue("2") int powerAttempts,
				@DefaultValue("3") int fpgaAttempts,
				@DefaultValue("false") boolean fpgaReload,
				@DefaultValue("5") int buildAttempts,
				@DefaultValue("false") boolean dummy) {
			this.period = period;
			this.probeInterval = probeInterval;
			this.powerAttempts = powerAttempts;
			this.fpgaAttempts = fpgaAttempts;
			this.fpgaReload = fpgaReload;
			this.buildAttempts = buildAttempts;
			this.dummy = dummy;
		}

		/**
		 * How long between when we send requests to the BMP control tasks.
		 *
		 * @return How long between when we send requests to the BMP control
		 *         tasks.
		 */
		@NotNull
		public Duration getPeriod() {
			return period;
		}

		void setPeriod(Duration period) {
			this.period = period;
		}

		/** @return The basic wait time used by the BMP control tasks. */
		@NotNull
		public Duration getProbeInterval() {
			return probeInterval;
		}

		void setProbeInterval(Duration probeInterval) {
			this.probeInterval = probeInterval;
		}

		/**
		 * @return Number of attempts that will be made to switch on a board.
		 */
		@Positive
		public int getPowerAttempts() {
			return powerAttempts;
		}

		void setPowerAttempts(int powerAttempts) {
			this.powerAttempts = powerAttempts;
		}

		/** @return Number of attempts that will be made to bring up an FPGA. */
		@Positive
		public int getFpgaAttempts() {
			return fpgaAttempts;
		}

		void setFpgaAttempts(int fpgaAttempts) {
			this.fpgaAttempts = fpgaAttempts;
		}

		/**
		 * @return Whether to reload the FPGA bitfiles when they don't come up
		 *         correctly.
		 */
		public boolean isFpgaReload() {
			return fpgaReload;
		}

		void setFpgaReload(boolean fpgaReload) {
			this.fpgaReload = fpgaReload;
		}

		/**
		 * Number of attempts that will be made to bring up a transceiver,
		 * provided the failures are due to timeouts and not outright network
		 * errors. Note that a failure to bring up a transceiver is lethal to
		 * the service.
		 *
		 * @return Number of attempts that will be made to bring up a
		 *         transceiver.
		 */
		@Positive
		public int getBuildAttempts() {
			return buildAttempts;
		}

		void setBuildAttempts(int value) {
			this.buildAttempts = value;
		}

		/**
		 * Useful for testing only.
		 *
		 * @return Whether to use a dummy transceiver.
		 */
		public boolean isDummy() {
			return dummy;
		}

		void setDummy(boolean dummy) {
			this.dummy = dummy;
		}
	}

	/** Additional database configuration. */
	public static class DBProperties {
		private static final int MAX_ANALYSIS = 1000;

		private static final int MIN_ANALYSIS = 100;

		/** How long to wait to get a database lock. */
		private Duration timeout;

		/** Whether to send details of SQL-related exceptions to users. */
		private boolean debugFailures;

		/**
		 * Amount of effort to spend on DB optimisation on application close.
		 * See the SQLite documentation for meaning. Note that this is spent by
		 * every worker thread that touches the database.
		 */
		private int analysisLimit;

		/**
		 * Whether to collect and write query performance metrics to the log on
		 * termination.
		 */
		private boolean performanceLog;

		/**
		 * Performance stats not reported for queries with a max less than this
		 * (in nanoseconds).
		 */
		private double performanceThreshold;

		/** Number of times to try to take the lock in a transaction. */
		private int lockTries;

		/** Delay after transaction failure before retrying. */
		private Duration lockFailedDelay;

		/** Time delay before we issue a warning on transaction end. */
		private Duration lockNoteThreshold;

		/**
		 * Time delay before we issue a warning during the execution of a
		 * transaction.
		 */
		private Duration lockWarnThreshold;

		/** Whether to determine the caller when doing transaction logging. */
		private boolean enableExpensiveTransactionDebugging;

		/**
		 * @param timeout
		 *            How long to wait to get a database lock.
		 * @param debugFailures
		 *            Whether to send details of SQL-related exceptions to
		 *            users.
		 * @param analysisLimit
		 *            Amount of effort to spend on DB optimisation on
		 *            application close. See the SQLite documentation for
		 *            meaning. Note that this is spent by every worker thread
		 *            that touches the database.
		 * @param performanceLog
		 *            Whether to collect and write query performance metrics to
		 *            the log on termination.
		 * @param performanceThreshold
		 *            Performance stats not reported for queries with a max less
		 *            than this (in nanoseconds).
		 * @param lockTries
		 *            Number of times to try to take the lock in a transaction.
		 * @param lockFailedDelay
		 *            Delay after transaction failure before retrying.
		 * @param lockNoteThreshold
		 *            Time delay before we issue a warning on transaction end.
		 * @param lockWarnThreshold
		 *            Time delay before we issue a warning during the execution
		 *            of a transaction.
		 * @param enableExpensiveTransactionDebugging
		 *            Whether to determine the caller when doing transaction
		 *            logging.
		 */
		public DBProperties(@DefaultValue("1s") Duration timeout,
				@DefaultValue("false") boolean debugFailures,
				@DefaultValue("400") int analysisLimit,
				@DefaultValue("false") boolean performanceLog,
				@DefaultValue("1e6") double performanceThreshold,
				@DefaultValue("3") int lockTries,
				@DefaultValue("100ms") Duration lockFailedDelay,
				@DefaultValue("50ms") Duration lockNoteThreshold,
				@DefaultValue("100ms") Duration lockWarnThreshold,
				@DefaultValue("false") //
				boolean enableExpensiveTransactionDebugging) {
			this.timeout = timeout;
			this.debugFailures = debugFailures;
			this.analysisLimit = analysisLimit;
			this.performanceLog = performanceLog;
			this.performanceThreshold = performanceThreshold;
			this.lockTries = lockTries;
			this.lockFailedDelay = lockFailedDelay;
			this.lockNoteThreshold = lockNoteThreshold;
			this.lockWarnThreshold = lockWarnThreshold;
			this.enableExpensiveTransactionDebugging =
					enableExpensiveTransactionDebugging;
		}

		/** @return How long to wait to get a database lock. */
		@NotNull
		public Duration getTimeout() {
			return timeout;
		}

		void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		/**
		 * @return Whether to send details of SQL-related exceptions to users.
		 */
		public boolean isDebugFailures() {
			return debugFailures;
		}

		void setDebugFailures(boolean debugFailures) {
			this.debugFailures = debugFailures;
		}

		/**
		 * Amount of effort to spend on DB optimisation on application close.
		 * See the SQLite documentation for meaning. Note that this is spent by
		 * every worker thread that touches the database.
		 *
		 * @return Amount of effort to spend on DB optimisation on application
		 *         close.
		 * @see <a href="https://sqlite.org/lang_analyze.html">SQLite docs</a>
		 */
		@Min(MIN_ANALYSIS)
		@Max(MAX_ANALYSIS)
		public int getAnalysisLimit() {
			return analysisLimit;
		}

		void setAnalysisLimit(int analysisLimit) {
			this.analysisLimit = analysisLimit;
		}

		/**
		 * @return Whether to collect and write query performance metrics to the
		 *         log on termination. Note that this is checked both when
		 *         recording performance (on each database query) and when the
		 *         log writes happen (on termination).
		 */
		public final boolean isPerformanceLog() {
			return performanceLog;
		}

		void setPerformanceLog(boolean log) {
			this.performanceLog = log;
		}

		/**
		 * @return Number of nanoseconds where performance stats are not
		 *         reported for queries with a max less than this.
		 */
		@Positive
		public final double getPerformanceThreshold() {
			return performanceThreshold;
		}

		void setPerformanceThreshold(double threshold) {
			this.performanceThreshold = threshold;
		}

		/** @return Number of times to try to take the lock in a transaction. */
		@Positive
		public int getLockTries() {
			return lockTries;
		}

		void setLockTries(int lockTries) {
			this.lockTries = lockTries;
		}

		/** @return Delay after transaction failure before retrying. */
		@NotNull
		public Duration getLockFailedDelay() {
			return lockFailedDelay;
		}

		void setLockFailedDelay(Duration value) {
			this.lockFailedDelay = value;
		}

		/** @return Time delay before we issue a warning on transaction end. */
		@NotNull
		public Duration getLockNoteThreshold() {
			return lockNoteThreshold;
		}

		void setLockNoteThreshold(Duration value) {
			this.lockNoteThreshold = value;
		}

		/**
		 * @return Time delay before we issue a warning during the execution of
		 *         a transaction.
		 */
		@NotNull
		public Duration getLockWarnThreshold() {
			return lockWarnThreshold;
		}

		void setLockWarnThreshold(Duration value) {
			this.lockWarnThreshold = value;
		}

		/**
		 * @return Whether to determine the caller when doing transaction
		 *         logging.
		 */
		public boolean isEnableExpensiveTransactionDebugging() {
			return enableExpensiveTransactionDebugging;
		}

		void setEnableExpensiveTransactionDebugging(boolean value) {
			this.enableExpensiveTransactionDebugging = value;
		}
	}

	/** Settings relating to the v1 spalloc configuration interface. */
	public static class CompatibilityProperties {
		private static final int MIN_PORT = 1024;

		private static final int MAX_PORT = 65535;

		/**
		 * Whether to turn the spalloc version 1 compatibility service interface
		 * on.
		 */
		private boolean enable;

		/**
		 * The number of threads to use to service the v1 clients. 0 means no
		 * limit.
		 */
		private int threadPoolSize;

		/** What hostname to run the spalloc v1 compatibility service on. */
		private String host;

		/** What UDP port to run the spalloc v1 compatibility service on. */
		private int port;

		/** How long to wait for the executor to shut down, maximum. */
		private Duration shutdownTimeout;

		/**
		 * How long to wait for a message to be received. Making this too short
		 * makes the service more expensive. Making this too long makes the
		 * service difficult to shut down correctly.
		 */
		private Duration receiveTimeout;

		/**
		 * What user to run jobs submitted through the spalloc v1 compatibility
		 * service with. We recommend that this user exists but is disabled
		 * (i.e., login using this service identity need not be supported).
		 */
		private String serviceUser;

		/**
		 * What group to run jobs submitted through the spalloc v1 compatibility
		 * service against. This group needs to exist, and the service user
		 * needs to be a member of it, but does not need to have a quota set.
		 */
		private String serviceGroup;

		/**
		 * How long to pass to the spalloc core to wait for timeouts relating to
		 * message notifications (i.e., due to machine or job status changes).
		 */
		private Duration notifyWaitTime;

		/** The default value for the keepalive property of jobs. */
		private Duration defaultKeepalive;

		/**
		 * @param enable
		 *            Whether to turn the spalloc version 1 compatibility
		 *            service interface on.
		 * @param threadPoolSize
		 *            The number of threads to use to service the v1 clients. 0
		 *            means no limit.
		 * @param host
		 *            What hostname to run the spalloc v1 compatibility service
		 *            on.
		 * @param port
		 *            What UDP port to run the spalloc v1 compatibility service
		 *            on.
		 * @param serviceUser
		 *            What user to run jobs submitted through the spalloc v1
		 *            compatibility service with. We recommend that this user
		 *            exists but is disabled (i.e., login using this service
		 *            identity need not be supported).
		 * @param serviceGroup
		 *            What group to run jobs submitted through the spalloc v1
		 *            compatibility service against. This group needs to exist,
		 *            and the service user needs to be a member of it, but does
		 *            not need to have a quota set.
		 * @param receiveTimeout
		 *            How long to wait for a message to be received. Making this
		 *            too short makes the service more expensive. Making this
		 *            too long makes the service difficult to shut down
		 *            correctly.
		 * @param shutdownTimeout
		 *            How long to wait for the executor to shut down, maximum.
		 * @param notifyWaitTime
		 *            How long to pass to the spalloc core to wait for timeouts
		 *            relating to message notifications (i.e., due to machine or
		 *            job status changes).
		 * @param defaultKeepalive
		 *            The default value for the keepalive property of jobs.
		 */
		public CompatibilityProperties(@DefaultValue("false") boolean enable,
				@DefaultValue("0") int threadPoolSize,
				@DefaultValue("0.0.0.0") String host,
				@DefaultValue("22244") int port,
				@DefaultValue("") String serviceUser,
				@DefaultValue("") String serviceGroup,
				@DefaultValue("2000ms") Duration receiveTimeout,
				@DefaultValue("3s") Duration shutdownTimeout,
				@DefaultValue("1m") Duration notifyWaitTime,
				@DefaultValue("1m") Duration defaultKeepalive) {
			this.enable = enable;
			this.threadPoolSize = threadPoolSize;
			this.host = host;
			this.port = port;
			this.serviceUser = serviceUser;
			this.serviceGroup = serviceGroup;
			this.receiveTimeout = receiveTimeout;
			this.shutdownTimeout = shutdownTimeout;
			this.notifyWaitTime = notifyWaitTime;
			this.defaultKeepalive = defaultKeepalive;
		}

		/**
		 * @return Whether to turn the spalloc version 1 compatibility service
		 *         interface on.
		 */
		public boolean isEnable() {
			return enable;
		}

		void setEnable(boolean enable) {
			this.enable = enable;
		}

		/**
		 * @return The number of threads to use to service the v1 clients. 0
		 *         means no limit (slightly dangerous).
		 */
		@PositiveOrZero
		public int getThreadPoolSize() {
			return threadPoolSize;
		}

		void setThreadPoolSize(int threadPoolSize) {
			this.threadPoolSize = threadPoolSize;
		}

		/**
		 * @return What host address to run the spalloc v1 compatibility service
		 *         on.
		 */
		@NotBlank
		public String getHost() {
			return host;
		}

		void setHost(String host) {
			this.host = host;
		}

		/** @return What port to run the spalloc v1 compatibility service on. */
		@Min(MIN_PORT)
		@Max(MAX_PORT)
		public int getPort() {
			return port;
		}

		void setPort(int port) {
			this.port = port;
		}

		/**
		 * What user to run jobs submitted through the spalloc v1 compatibility
		 * service with. We recommend that this user exists but is disabled
		 * (i.e., login using this service identity need not be supported).
		 *
		 * @return What user to run jobs submitted through the spalloc v1
		 *         compatibility service with.
		 */
		public String getServiceUser() {
			return serviceUser;
		}

		void setServiceUser(String serviceUser) {
			this.serviceUser = serviceUser;
		}

		@Keep
		@AssertTrue(message = "a service username must be given "
				+ "if the v1 service is enabled")
		private boolean isValidUserIfEnabled() {
			return !enable
					|| (nonNull(serviceUser) && !serviceUser.isBlank());
		}

		/**
		 * What group to run jobs submitted through the spalloc v1 compatibility
		 * service against. This group needs to exist, and the service user
		 * needs to be a member of it, but does not need to have a quota set.
		 *
		 * @return What group to run jobs submitted through the spalloc v1
		 *         compatibility service against.
		 */
		public String getServiceGroup() {
			return serviceGroup;
		}

		void setServiceGroup(String serviceUser) {
			this.serviceGroup = serviceUser;
		}

		@Keep
		@AssertTrue(message = "a service group must be given "
				+ "if the v1 service is enabled")
		private boolean isValidGroupIfEnabled() {
			return !enable || (nonNull(serviceGroup)
					&& !serviceGroup.isBlank());
		}

		/**
		 * @return How long to wait for the executor to shut down, maximum.
		 */
		@NotNull
		public Duration getShutdownTimeout() {
			return shutdownTimeout;
		}

		void setShutdownTimeout(Duration value) {
			this.shutdownTimeout = value;
		}

		/**
		 * Making this too short makes the service more expensive. Making this
		 * too long makes the service difficult to shut down correctly. (Failure
		 * to receive in this time triggers an exception, but it needs to be
		 * fairly frequent or the thread can't be interrupted.)
		 *
		 * @return How long to wait for a message to be received.
		 */
		@NotNull
		public Duration getReceiveTimeout() {
			return receiveTimeout;
		}

		void setReceiveTimeout(Duration value) {
			this.receiveTimeout = value;
		}

		/**
		 * @return How long to pass to the spalloc core to wait for timeouts
		 *         relating to message notifications (i.e., due to machine or
		 *         job status changes).
		 */
		@NotNull
		public Duration getNotifyWaitTime() {
			return notifyWaitTime;
		}

		void setNotifyWaitTime(Duration value) {
			this.notifyWaitTime = value;
		}

		/**
		 * @return The default value for the keepalive property of jobs.
		 */
		@NotNull
		public Duration getDefaultKeepalive() {
			return defaultKeepalive;
		}

		void setDefaultKeepalive(Duration value) {
			this.defaultKeepalive = value;
		}
	}

	/** Settings for the proxies. */
	public static class ProxyProperties {
		/** Whether to enable the UDP proxy subsystem. */
		private boolean enable;

		/**
		 * Whether to log the number of packets read and written on each
		 * channel.
		 */
		private boolean logWriteCounts;

		/**
		 * The address for local proxied sockets to listen on if they're not
		 * being bound to a specific board. If empty, that type of socket will
		 * be disabled.
		 */
		private String localHost;

		/**
		 * @param enable
		 *            Whether to enable the UDP proxy subsystem.
		 * @param logWriteCounts
		 *            Whether to log the number of packets read and written on
		 *            each channel.
		 * @param localHost
		 *            The address for local proxied sockets to listen on if
		 *            they're not being bound to a specific board. If empty,
		 *            that type of socket will be disabled.
		 */
		public ProxyProperties(@DefaultValue("true") boolean enable,
				@DefaultValue("false") boolean logWriteCounts,
				@DefaultValue("") String localHost) {
			this.enable = enable;
			this.logWriteCounts = logWriteCounts;
			this.localHost = localHost;
		}

		/** @return Whether to enable the UDP proxy subsystem. */
		public boolean isEnable() {
			return enable;
		}

		void setEnable(boolean enable) {
			this.enable = enable;
		}

		/**
		 * @return Whether to log the number of packets read and written on each
		 *         channel.
		 */
		public boolean isLogWriteCounts() {
			return logWriteCounts;
		}

		void setLogWriteCounts(boolean logWriteCounts) {
			this.logWriteCounts = logWriteCounts;
		}

		/**
		 * @return The address for local proxied sockets to listen on if
		 *         they're not being bound to a specific board. If empty, that
		 *         type of socket will be disabled.
		 */
		@NotNull
		@IPAddress(emptyOK = true)
		public String getLocalHost() {
			return localHost;
		}

		void setLocalHost(String localHost) {
			this.localHost = localHost;
		}
	}

	/** Settings for control of the administrative state of a machine. */
	public static class StateControlProperties {
		/** How long to wait between polls of the BMP controller. */
		private Duration blacklistPoll;

		/** How long to wait for a blacklist operation to complete. */
		private Duration blacklistTimeout;

		/**
		 * How many board serial numbers to read from a full machine at once
		 * when synchronizing the overall state?
		 */
		private int serialReadBatchSize;

		/**
		 * How many blacklists to read from a full machine at once when
		 * synchronizing the overall state?
		 */
		private int blacklistReadBatchSize;

		/**
		 * @param blacklistPoll
		 *            How long to wait between polls of the BMP controller.
		 * @param blacklistTimeout
		 *            How long to wait for a blacklist operation to complete.
		 * @param serialReadBatchSize
		 *            How many board serial numbers to read from a full machine
		 *            at once when synchronizing the overall state?
		 * @param blacklistReadBatchSize
		 *            How many blacklists to read from a full machine at once
		 *            when synchronizing the overall state?
		 */
		public StateControlProperties(
				@DefaultValue("15s") Duration blacklistPoll,
				@DefaultValue("60s") Duration blacklistTimeout,
				@DefaultValue("24") int serialReadBatchSize,
				@DefaultValue("6") int blacklistReadBatchSize) {
			this.blacklistPoll = blacklistPoll;
			this.blacklistTimeout = blacklistTimeout;
			this.serialReadBatchSize = serialReadBatchSize;
			this.blacklistReadBatchSize = blacklistReadBatchSize;
		}

		/** @return How long to wait between polls of the BMP controller. */
		@NotNull
		public Duration getBlacklistPoll() {
			return blacklistPoll;
		}

		void setBlacklistPoll(Duration blacklistPoll) {
			this.blacklistPoll = blacklistPoll;
		}

		/** @return How long to wait for a blacklist operation to complete. */
		@NotNull
		public Duration getBlacklistTimeout() {
			return blacklistTimeout;
		}

		void setBlacklistTimeout(Duration blacklistTimeout) {
			this.blacklistTimeout = blacklistTimeout;
		}

		/**
		 * @return How many board serial numbers to read from a full machine at
		 *         once when synchronizing the overall state?
		 */
		@Positive
		public int getSerialReadBatchSize() {
			return serialReadBatchSize;
		}

		void setSerialReadBatchSize(int serialReadBatchSize) {
			this.serialReadBatchSize = serialReadBatchSize;
		}

		/**
		 * @return How many blacklists to read from a full machine at once when
		 *         synchronizing the overall state?
		 */
		@Positive
		public int getBlacklistReadBatchSize() {
			return blacklistReadBatchSize;
		}

		void setBlacklistReadBatchSize(int blacklistReadBatchSize) {
			this.blacklistReadBatchSize = blacklistReadBatchSize;
		}
	}
}
