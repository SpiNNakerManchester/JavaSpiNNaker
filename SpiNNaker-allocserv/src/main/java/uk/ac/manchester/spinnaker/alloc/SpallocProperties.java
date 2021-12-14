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

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

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

	private HistoricalDataProperties historicalData;

	private KeepaliveProperties keepalive;

	private AllocatorProperties allocator;

	private AuthProperties auth;

	private QuotaProperties quota;

	private TxrxProperties transceiver;

	private DBProperties sqlite;

	private CompatibilityProperties compat;

	@SuppressWarnings("checkstyle:ParameterNumber")
	public SpallocProperties(//
			@DefaultValue("spalloc.sqlite3") File databasePath,
			@DefaultValue("30s") Duration wait,
			@DefaultValue("false") boolean pause,
			@DefaultValue AllocatorProperties allocator,
			@DefaultValue AuthProperties auth,
			@DefaultValue CompatibilityProperties compat,
			@DefaultValue HistoricalDataProperties historicalData,
			@DefaultValue KeepaliveProperties keepalive,
			@DefaultValue QuotaProperties quota,
			@DefaultValue DBProperties sqlite,
			@DefaultValue TxrxProperties transceiver) {
		this.databasePath = databasePath;
		this.wait = wait;
		this.pause = pause;
		this.allocator = allocator;
		this.auth = auth;
		this.compat = compat;
		this.historicalData = historicalData;
		this.keepalive = keepalive;
		this.quota = quota;
		this.sqlite = sqlite;
		this.transceiver = transceiver;
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

	public void setDatabasePath(File databasePath) {
		this.databasePath = databasePath;
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

	public void setWait(Duration wait) {
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

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	/**
	 * @return Properties relating to historical data management.
	 */
	@NotNull
	@Valid
	public HistoricalDataProperties getHistoricalData() {
		return historicalData;
	}

	public void setHistoricalData(HistoricalDataProperties historicalData) {
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

	public void setKeepalive(KeepaliveProperties keepalive) {
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

	public void setAllocator(AllocatorProperties allocator) {
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

	public void setAuth(AuthProperties auth) {
		this.auth = auth;
	}

	/**
	 * @return Properties relating to quota management.
	 */
	@NotNull
	@Valid
	public QuotaProperties getQuota() {
		return quota;
	}

	public void setQuota(QuotaProperties quota) {
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

	public void setTransceiver(TxrxProperties transceiver) {
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

	public void setSqlite(DBProperties sqlite) {
		this.sqlite = sqlite;
	}

	/**
	 * @return Properties relating to the Spalloc v1 compatibility layer.
	 */
	@NotNull
	@Valid
	public CompatibilityProperties getCompat() {
		return compat;
	}

	public void setCompat(CompatibilityProperties compat) {
		this.compat = compat;
	}

	/** How to handle job data that is now only of historic interest. */
	public static class HistoricalDataProperties {
		/** Path to the historical job database file. */
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

		public HistoricalDataProperties(
				@DefaultValue("spalloc-history.sqlite3") File path,
				@DefaultValue("14d") Duration gracePeriod,
				@DefaultValue("0 0 2 * * *") String schedule) {
			this.path = path;
			this.gracePeriod = gracePeriod;
			this.schedule = schedule;
		}

		/** @return Path to the historical job database file. */
		@NotNull
		public File getPath() {
			return path;
		}

		public void setPath(File path) {
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

		public void setGracePeriod(Duration gracePeriod) {
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

		public void setSchedule(String schedule) {
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

		public void setExpiryPeriod(Duration expiryPeriod) {
			this.expiryPeriod = expiryPeriod;
		}

		/** @return Minimum keepalive period. */
		@NotNull
		public Duration getMin() {
			return min;
		}

		public void setMin(Duration min) {
			this.min = min;
		}

		/** @return Maximum keepalive period. */
		@NotNull
		public Duration getMax() {
			return max;
		}

		public void setMax(Duration max) {
			this.max = max;
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

		/** Properties relating to board issue reporting. */
		private ReportProperties reportEmail;

		public AllocatorProperties(@DefaultValue("5s") Duration period,
				@DefaultValue("10000") int importanceSpan,
				@DefaultValue PriorityScale priorityScale,
				@DefaultValue("2") int reportActionThreshold,
				@DefaultValue ReportProperties reportEmail) {
			this.period = period;
			this.importanceSpan = importanceSpan;
			this.priorityScale = priorityScale;
			this.reportActionThreshold = reportActionThreshold;
			this.reportEmail = reportEmail;
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

		public void setPeriod(Duration period) {
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

		public void setImportanceSpan(int importanceSpan) {
			this.importanceSpan = importanceSpan;
		}

		/**
		 * Properties relating to job priority scaling.
		 *
		 * @return Properties relating to job priority scaling.
		 */
		@Valid
		public PriorityScale getPriorityScale() {
			return priorityScale;
		}

		public void setPriorityScale(PriorityScale priorityScale) {
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

		public void setReportActionThreshold(int threshold) {
			reportActionThreshold = threshold;
		}

		/** @return Properties relating to board issue reporting. */
		@Valid
		public ReportProperties getReportEmail() {
			return reportEmail;
		}

		public void setReportEmail(ReportProperties reportEmail) {
			this.reportEmail = reportEmail;
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

		public void setSize(double factor) {
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

		public void setDimensions(double factor) {
			dimensions = factor;
		}

		/**
		 * @return Priority scaling factor for jobs requiring a specific board.
		 */
		@Positive
		public double getSpecificBoard() {
			return specificBoard;
		}

		public void setSpecificBoard(double factor) {
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

		public void setSend(boolean send) {
			this.send = send;
		}

		/** @return The {@code From:} email address. */
		@Email
		public String getFrom() {
			return from;
		}

		public void setFrom(String address) {
			from = address;
		}

		/** @return The {@code To:} email address. */
		@Email
		public String getTo() {
			return to;
		}

		public void setTo(String address) {
			to = address;
		}

		/** @return The {@code Subject:} header. */
		@NotBlank
		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

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

		@SuppressWarnings("checkstyle:ParameterNumber")
		public AuthProperties(//
				@DefaultValue("true") boolean basic,
				@DefaultValue("true") boolean localForm,
				@DefaultValue("false") boolean addDummyUser,
				@DefaultValue("true") boolean dummyRandomPass,
				@DefaultValue("false") boolean debugFailures,
				@DefaultValue("3") int maxLoginFailures,
				@DefaultValue("24h") Duration accountLockDuration,
				@DefaultValue("60s") Duration unlockPeriod,
				@DefaultValue OpenIDProperties openid) {
			this.basic = basic;
			this.localForm = localForm;
			this.addDummyUser = addDummyUser;
			this.dummyRandomPass = dummyRandomPass;
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

		public void setBasic(boolean basic) {
			this.basic = basic;
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

		public void setLocalForm(boolean localForm) {
			this.localForm = localForm;
		}

		/**
		 * @return Force a known local admin user to exist with a known
		 *         password.
		 */
		public boolean isAddDummyUser() {
			return addDummyUser;
		}

		public void setAddDummyUser(boolean addDummyUser) {
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

		public void setDummyRandomPass(boolean dummyRandomPass) {
			this.dummyRandomPass = dummyRandomPass;
		}

		/** @return Provide extra information to callers on auth failures. */
		public boolean isDebugFailures() {
			return debugFailures;
		}

		public void setDebugFailures(boolean debugFailures) {
			this.debugFailures = debugFailures;
		}

		/** @return Number of login failures before automatic lock-out. */
		@Positive
		public int getMaxLoginFailures() {
			return maxLoginFailures;
		}

		public void setMaxLoginFailures(int maxLoginFailures) {
			this.maxLoginFailures = maxLoginFailures;
		}

		/** @return Length of time that automatic lock-out lasts. */
		@NotNull
		public Duration getAccountLockDuration() {
			return accountLockDuration;
		}

		public void setAccountLockDuration(Duration accountLockDuration) {
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

		public void setUnlockPeriod(Duration unlockPeriod) {
			this.unlockPeriod = unlockPeriod;
		}

		/**
		 * OpenID-related security properties. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 *
		 * @return OpenID-related security properties.
		 */
		public OpenIDProperties getOpenid() {
			return openid;
		}

		public void setOpenid(OpenIDProperties openid) {
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

		@SuppressWarnings("checkstyle:ParameterNumber")
		public OpenIDProperties(@DefaultValue("false") boolean enable,
				@DefaultValue("") String domain, //
				@DefaultValue("") String id, //
				@DefaultValue("") String secret,
				@DefaultValue("openid.") String usernamePrefix,
				@DefaultValue("PKCS12") String truststoreType,
				@DefaultValue("classpath:/truststore.p12") //
				Resource truststorePath,
				@DefaultValue("") String truststorePassword) {
			this.enable = enable;
			this.domain = domain;
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

		public void setEnable(boolean enable) {
			this.enable = enable;
		}

		/**
		 * The application installation identity. Required for allowing people
		 * to use HBP/EBRAINS identities.
		 *
		 * @return The application installation identity.
		 */
		public String getId() {
			return id == null ? null : id.trim();
		}

		public void setId(String id) {
			this.id = id;
		}

		/**
		 * The application installation secret. Required for allowing people to
		 * use HBP/EBRAINS identities.
		 *
		 * @return The application installation secret.
		 */
		public String getSecret() {
			return secret == null ? null : secret.trim();
		}

		public void setSecret(String secret) {
			this.secret = secret;
		}

		/**
		 * Prefix for user names originating from OpenID auto-registration. Not
		 * a good idea to modify this frequently!
		 *
		 * @return Prefix for user names originating from OpenID
		 *         auto-registration.
		 */
		public String getUsernamePrefix() {
			return usernamePrefix;
		}

		public void setUsernamePrefix(String usernamePrefix) {
			this.usernamePrefix = usernamePrefix;
		}

		/**
		 * The root path of the OpenID 2 Discovery domain. Referred to elsewhere
		 * in the configuration file.
		 *
		 * @return The root path of the OpenID 2 Discovery domain.
		 */
		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		/**
		 * What kind of truststore is it.
		 *
		 * @return truststore type (default: {@code PKCS12})
		 */
		public String getTruststoreType() {
			return truststoreType;
		}

		public void setTruststoreType(String truststoreType) {
			this.truststoreType = truststoreType;
		}

		/**
		 * Where the truststore is.
		 *
		 * @return truststore location
		 */
		public Resource getTruststorePath() {
			return truststorePath;
		}

		public void setTruststorePath(Resource truststorePath) {
			this.truststorePath = truststorePath;
		}

		/**
		 * How to unlock the truststore. This is not considered to be actually
		 * secret, but rather just a technical requirement of the truststore
		 * format.
		 *
		 * @return password for truststore
		 */
		public String getTruststorePassword() {
			return truststorePassword;
		}

		public void setTruststorePassword(String truststorePassword) {
			this.truststorePassword = truststorePassword;
		}

		@AssertTrue(
				message = "id and secret must be given if OpenID is enabled")
		private boolean isValid() {
			return !enable || (nonNull(id) && !id.trim().isEmpty()
					&& nonNull(secret) && !secret.trim().isEmpty());
		}
	}

	/** Quota management. */
	public static class QuotaProperties {
		/** Default user quota in board-seconds. */
		private int defaultQuota;

		/**
		 * Cron expression that says when we consolidate job quotas into the
		 * main quota table.
		 */
		private String consolidationSchedule;

		public QuotaProperties(@DefaultValue("100") int defaultQuota,
				@DefaultValue("0 0 * * * *") String consolidationSchedule) {
			this.defaultQuota = defaultQuota;
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

		public void setDefaultQuota(int defaultQuota) {
			this.defaultQuota = defaultQuota;
		}

		/**
		 * @return Cron expression that says when we consolidate job quotas into
		 *         the main quota table.
		 */
		@NotBlank
		public String getConsolidationSchedule() {
			return consolidationSchedule;
		}

		public void setConsolidationSchedule(String consolidationSchedule) {
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

		/** Whether to use a dummy transceiver. Useful for testing only. */
		private boolean dummy;

		public TxrxProperties(@DefaultValue("10s") Duration period,
				@DefaultValue("15s") Duration probeInterval,
				@DefaultValue("2") int powerAttempts,
				@DefaultValue("3") int fpgaAttempts,
				@DefaultValue("false") boolean dummy) {
			this.period = period;
			this.probeInterval = probeInterval;
			this.powerAttempts = powerAttempts;
			this.fpgaAttempts = fpgaAttempts;
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

		public void setPeriod(Duration period) {
			this.period = period;
		}

		/** @return The basic wait time used by the BMP control tasks. */
		@NotNull
		public Duration getProbeInterval() {
			return probeInterval;
		}

		public void setProbeInterval(Duration probeInterval) {
			this.probeInterval = probeInterval;
		}

		/**
		 * @return Number of attempts that will be made to switch on a board.
		 */
		@Positive
		public int getPowerAttempts() {
			return powerAttempts;
		}

		public void setPowerAttempts(int powerAttempts) {
			this.powerAttempts = powerAttempts;
		}

		/** @return Number of attempts that will be made to bring up an FPGA. */
		@Positive
		public int getFpgaAttempts() {
			return fpgaAttempts;
		}

		public void setFpgaAttempts(int fpgaAttempts) {
			this.fpgaAttempts = fpgaAttempts;
		}

		/**
		 * Useful for testing only.
		 *
		 * @return Whether to use a dummy transceiver.
		 */
		public boolean isDummy() {
			return dummy;
		}

		public void setDummy(boolean dummy) {
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

		public DBProperties(@DefaultValue("1s") Duration timeout,
				@DefaultValue("false") boolean debugFailures,
				@DefaultValue("400") int analysisLimit,
				@DefaultValue("false") boolean performanceLog,
				@DefaultValue("1e6") double performanceThreshold) {
			this.timeout = timeout;
			this.debugFailures = debugFailures;
			this.analysisLimit = analysisLimit;
			this.performanceLog = performanceLog;
			this.performanceThreshold = performanceThreshold;
		}

		/** @return How long to wait to get a database lock. */
		@NotNull
		public Duration getTimeout() {
			return timeout;
		}

		public void setTimeout(Duration timeout) {
			this.timeout = timeout;
		}

		/**
		 * @return Whether to send details of SQL-related exceptions to users.
		 */
		public boolean isDebugFailures() {
			return debugFailures;
		}

		public void setDebugFailures(boolean debugFailures) {
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

		public void setAnalysisLimit(int analysisLimit) {
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

		public void setPerformanceLog(boolean log) {
			this.performanceLog = log;
		}

		/**
		 * @return Number of nanoseconds where performance stats are not
		 *         reported for queries with a max less than this.
		 */
		public final double getPerformanceThreshold() {
			return performanceThreshold;
		}

		public void setPerformanceThreshold(double threshold) {
			this.performanceThreshold = threshold;
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

		private String host;

		/** What port to run the spalloc v1 compatibility service on. */
		private int port;

		/**
		 * What user to run jobs submitted through the spalloc v1 compatibility
		 * service with. We recommend that this user exists but is disabled
		 * (i.e., login using this service identity need not be supported).
		 */
		private String serviceUser;

		public CompatibilityProperties(@DefaultValue("false") boolean enable,
				@DefaultValue("0") int threadPoolSize,
				@DefaultValue("0.0.0.0") String host,
				@DefaultValue("22244") int port,
				@DefaultValue("") String serviceUser) {
			this.enable = enable;
			this.threadPoolSize = threadPoolSize;
			this.host = host;
			this.port = port;
			this.serviceUser = serviceUser;
		}

		/**
		 * @return Whether to turn the spalloc version 1 compatibility service
		 *         interface on.
		 */
		public boolean isEnable() {
			return enable;
		}

		public void setEnable(boolean enable) {
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

		public void setThreadPoolSize(int threadPoolSize) {
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

		public void setHost(String host) {
			this.host = host;
		}

		/** @return What port to run the spalloc v1 compatibility service on. */
		@Min(MIN_PORT)
		@Max(MAX_PORT)
		public int getPort() {
			return port;
		}

		public void setPort(int port) {
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

		public void setServiceUser(String serviceUser) {
			this.serviceUser = serviceUser;
		}

		@AssertTrue(message = "a service username must be given "
				+ "if the v1 service is enabled")
		private boolean isValidUserIfEnabled() {
			return !enable
					|| (nonNull(serviceUser) && !serviceUser.trim().isEmpty());
		}
	}
}
