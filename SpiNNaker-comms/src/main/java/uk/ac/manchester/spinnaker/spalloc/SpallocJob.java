package uk.ac.manchester.spinnaker.spalloc;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.util.Collections.emptyMap;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.KEEPALIVE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MACHINE_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_BOARDS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MAX_DEAD_LINKS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.MIN_RATIO_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_DEFAULT;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.RECONNECT_DELAY_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.REQUIRE_TORUS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TAGS_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.TIMEOUT_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.JobConstants.USER_PROPERTY;
import static uk.ac.manchester.spinnaker.spalloc.Utils.makeTimeout;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timeLeft;
import static uk.ac.manchester.spinnaker.spalloc.Utils.timedOut;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.DESTROYED;
import static uk.ac.manchester.spinnaker.spalloc.messages.State.UNKNOWN;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MS_PER_S;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.messages.model.Version;
import uk.ac.manchester.spinnaker.spalloc.exceptions.JobDestroyedException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocProtocolTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocServerException;
import uk.ac.manchester.spinnaker.spalloc.exceptions.SpallocStateChangeTimeoutException;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.Connection;
import uk.ac.manchester.spinnaker.spalloc.messages.JobMachineInfo;
import uk.ac.manchester.spinnaker.spalloc.messages.JobState;
import uk.ac.manchester.spinnaker.spalloc.messages.State;
import uk.ac.manchester.spinnaker.spalloc.messages.WhereIs;

/**
 * A high-level interface for requesting and managing allocations of SpiNNaker
 * boards.
 * <p>
 * Constructing a {@link SpallocJob} object connects to a
 * <a href="https://github.com/project-rig/spalloc_server">spalloc-server</a>
 * and requests a number of SpiNNaker boards. The job object may then be used to
 * monitor the state of the request, control the boards allocated and determine
 * their IP addresses.
 * <p>
 * In its simplest form, a {@link SpallocJob} can be used as a context manager
 * like so::
 *
 * <pre>
 * try (SpallocJob j = new SpallocJob(Arrays.asList(6), null)) {
 * 	myApplication.boot(j.getHostname(), j.getDimensions());
 * 	myApplication.run(j.getHostname());
 * }
 * </pre>
 *
 * In this example a six-board machine is requested and the
 * <tt>try</tt>-with-resources context is entered once the allocation has been
 * made and the allocated boards are fully powered on. When control leaves the
 * block, the job is destroyed and the boards shut down by the server ready for
 * another job.
 * <p>
 * For more fine-grained control, the same functionality is available via
 * various methods:
 *
 * <pre>
 * SpallocJob j = new SpallocJob(Arrays.asList(6), null));
 * j.waitUntilReady();
 * myApplication.boot(j.getHostname(), j.getDimensions());
 * myApplication.run(j.getHostname());
 * j.destroy();
 * </pre>
 *
 * <b>Note:</b> <blockquote class="note"> More complex applications may wish to
 * log the following properties of their job to support later debugging efforts:
 * <ul>
 * <li><tt>ID</tt> &mdash; May be used to query the state of the job and find
 * out its fate if cancelled or destroyed. The <i>spalloc-job</i> command can be
 * used to discover the state/fate of the job and <i>spalloc-where-is</i> may be
 * used to find out what boards problem chips reside on.
 * <li><tt>machineName</tt> and <tt>boards</tt> together give a complete record
 * of the hardware used by the job. The <i>spalloc-where-is</i> command may be
 * used to find out the physical locations of the boards used.
 * </ul>
 * </blockquote>
 */
public class SpallocJob implements AutoCloseable, SpallocJobAPI {
	private static final Logger log = getLogger(SpallocJob.class);
	private static final int DEFAULT_KEEPALIVE = 30;
	private static final int MAX_SHAPE_ARGS = 3;
	/** Minimum supported server version. */
	private static final Version MIN_VER = new Version(0, 4, 0);
	/** Maximum supported server version. */
	private static final Version MAX_VER = new Version(2, 0, 0);
	private static final int STATUS_CACHE_PERIOD = 500;

	private SpallocClient client;
	private int id;
	private Integer timeout;
	private Integer keepaliveTime;
	/** The keepalive thread. */
	private Thread keepalive;
	/** Used to signal that the keepalive thread should stop. */
	private volatile boolean stopping;
	/**
	 * Cache of information about a job's state. This information can change,
	 * but not usually extremely rapidly; it has a caching period, implemented
	 * using the {@link #statusTimestamp} field.
	 */
	private JobState statusCache;
	/**
	 * The time when the information in {@link #statusCache} was last collected.
	 */
	private long statusTimestamp;
	/**
	 * Cache of information about a machine. This is information which doesn't
	 * change once it is assigned, so there is no expiry mechanism.
	 */
	private JobMachineInfo machineInfoCache;
	private int reconnectDelay = f2ms(RECONNECT_DELAY_DEFAULT);
	private static final ThreadGroup SPALLOC_WORKERS =
			new ThreadGroup("spalloc worker threads");

	private static Configuration config;

	/**
	 * Set up where configuration settings come from. By default, this is from a
	 * file called {@value #DEFAULT_CONFIGURATION_SOURCE}; this method allows
	 * you to override that (e.g., for testing).
	 *
	 * @param filename
	 *            the base filename (without a path) to load the configuration
	 *            from. This is expected to be a <tt>.ini</tt> file.
	 * @see Configuration
	 */
	public static void setConfigurationSource(String filename) {
		config = new Configuration(filename);
	}

	/**
	 * The name of the default file to load the configuration from.
	 */
	public static final String DEFAULT_CONFIGURATION_SOURCE = "spalloc.ini";

	static {
		setConfigurationSource(DEFAULT_CONFIGURATION_SOURCE);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 * <p>
	 * The requested machine shape can be one of the following:
	 * <ul>
	 * <li><b>Empty</b> list, to get a single board.
	 * <li><b>Singleton</b> list, to get a machine with that number of boards.
	 * <li><b>Pair</b>, to get a rectangle of boards,
	 * <i>width</i>&times;<i>height</i>.
	 * <li><b>Triple</b>, to get a specific board (<i>x, y, z</i>).
	 * </ul>
	 * The supported extra properties consist of:
	 * <dl>
	 * <dt>{@value JobConstants#USER_PROPERTY} ({@link String})</dt>
	 * <dd>The name of the owner of the job. By convention this should be your
	 * email address.</dd>
	 * <dt>{@value JobConstants#KEEPALIVE_PROPERTY} ({@link Number})</dt>
	 * <dd>The number of seconds after which the server may consider the job
	 * dead if this client cannot communicate with it. If <tt>null</tt>, no
	 * timeout will be used and the job will run until explicitly destroyed. Use
	 * with extreme caution.</dd>
	 * <dt>{@value JobConstants#MACHINE_PROPERTY} ({@link String})</dt>
	 * <dd>Specify the name of a machine which this job must be executed on. If
	 * <tt>null</tt>, the first suitable machine available will be used,
	 * according to the tags selected below. Must be <tt>null</tt> when
	 * {@value JobConstants#TAGS_PROPERTY} are given.</dd>
	 * <dt>{@value JobConstants#TAGS_PROPERTY} ({@link String}[])</dt>
	 * <dd>The set of tags which any machine running this job must have. If
	 * <tt>null</tt> is supplied, only machines with the <tt>"default"</tt> tag
	 * will be used. If {@value JobConstants#MACHINE_PROPERTY} is given, this
	 * argument must be <tt>null</tt>.</dd>
	 * <dt>{@value JobConstants#MIN_RATIO_PROPERTY} ({@link Double})</dt>
	 * <dd>The aspect ratio (h/w) which the allocated region must be 'at least
	 * as square as'. Set to <tt>0.0</tt> for any allowable shape, <tt>1.0</tt>
	 * to be exactly square etc. Ignored when allocating single boards or
	 * specific rectangles of triads.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_BOARDS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken or unreachable boards to allow in the
	 * allocated region. If <tt>null</tt>, any number of dead boards is
	 * permitted, as long as the board on the bottom-left corner is alive.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_LINKS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken links allow in the allocated region.
	 * When {@value JobConstants#REQUIRE_TORUS_PROPERTY} is true this includes
	 * wrap-around links, otherwise peripheral links are not counted. If
	 * <tt>null</tt>, any number of broken links is allowed.</dd>
	 * <dt>{@value JobConstants#REQUIRE_TORUS_PROPERTY} ({@link Boolean})</dt>
	 * <dd>If <tt>true</tt>, only allocate blocks with torus connectivity. In
	 * general this will only succeed for requests to allocate an entire
	 * machine. Must be <tt>false</tt> (or not supplied) when allocating
	 * boards.</dd>
	 * </dl>
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param timeout
	 *            The communications timeout
	 * @param args
	 *            The machine shape description.
	 * @param kwargs
	 *            The extra properties.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	public SpallocJob(String hostname, Integer timeout, List<Integer> args,
			Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this(hostname, config.getPort(), timeout, args, kwargs);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 * <p>
	 * The requested machine shape can be one of the following:
	 * <ul>
	 * <li><b>Empty</b> list, to get a single board.
	 * <li><b>Singleton</b> list, to get a machine with that number of boards.
	 * <li><b>Pair</b>, to get a rectangle of boards,
	 * <i>width</i>&times;<i>height</i>.
	 * <li><b>Triple</b>, to get a specific board (<i>x, y, z</i>).
	 * </ul>
	 * The supported extra properties consist of:
	 * <dl>
	 * <dt>{@value JobConstants#USER_PROPERTY} ({@link String})</dt>
	 * <dd>The name of the owner of the job. By convention this should be your
	 * email address.</dd>
	 * <dt>{@value JobConstants#KEEPALIVE_PROPERTY} ({@link Number})</dt>
	 * <dd>The number of seconds after which the server may consider the job
	 * dead if this client cannot communicate with it. If <tt>null</tt>, no
	 * timeout will be used and the job will run until explicitly destroyed. Use
	 * with extreme caution.</dd>
	 * <dt>{@value JobConstants#MACHINE_PROPERTY} ({@link String})</dt>
	 * <dd>Specify the name of a machine which this job must be executed on. If
	 * <tt>null</tt>, the first suitable machine available will be used,
	 * according to the tags selected below. Must be <tt>null</tt> when
	 * {@value JobConstants#TAGS_PROPERTY} are given.</dd>
	 * <dt>{@value JobConstants#TAGS_PROPERTY} ({@link String}[])</dt>
	 * <dd>The set of tags which any machine running this job must have. If
	 * <tt>null</tt> is supplied, only machines with the <tt>"default"</tt> tag
	 * will be used. If {@value JobConstants#MACHINE_PROPERTY} is given, this
	 * argument must be <tt>null</tt>.</dd>
	 * <dt>{@value JobConstants#MIN_RATIO_PROPERTY} ({@link Double})</dt>
	 * <dd>The aspect ratio (h/w) which the allocated region must be 'at least
	 * as square as'. Set to <tt>0.0</tt> for any allowable shape, <tt>1.0</tt>
	 * to be exactly square etc. Ignored when allocating single boards or
	 * specific rectangles of triads.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_BOARDS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken or unreachable boards to allow in the
	 * allocated region. If <tt>null</tt>, any number of dead boards is
	 * permitted, as long as the board on the bottom-left corner is alive.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_LINKS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken links allow in the allocated region.
	 * When {@value JobConstants#REQUIRE_TORUS_PROPERTY} is true this includes
	 * wrap-around links, otherwise peripheral links are not counted. If
	 * <tt>null</tt>, any number of broken links is allowed.</dd>
	 * <dt>{@value JobConstants#REQUIRE_TORUS_PROPERTY} ({@link Boolean})</dt>
	 * <dd>If <tt>true</tt>, only allocate blocks with torus connectivity. In
	 * general this will only succeed for requests to allocate an entire
	 * machine. Must be <tt>false</tt> (or not supplied) when allocating
	 * boards.</dd>
	 * </dl>
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param args
	 *            The machine shape description.
	 * @param kwargs
	 *            The extra properties.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	public SpallocJob(String hostname, List<Integer> args,
			Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this(hostname, config.getPort(), f2ms(config.getTimeout()), args,
				kwargs);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 * <p>
	 * The requested machine shape can be one of the following:
	 * <ul>
	 * <li><b>Empty</b> list, to get a single board.
	 * <li><b>Singleton</b> list, to get a machine with that number of boards.
	 * <li><b>Pair</b>, to get a rectangle of boards,
	 * <i>width</i>&times;<i>height</i>.
	 * <li><b>Triple</b>, to get a specific board (<i>x, y, z</i>).
	 * </ul>
	 * The supported extra properties consist of:
	 * <dl>
	 * <dt>{@value JobConstants#USER_PROPERTY} ({@link String})</dt>
	 * <dd>The name of the owner of the job. By convention this should be your
	 * email address.</dd>
	 * <dt>{@value JobConstants#KEEPALIVE_PROPERTY} ({@link Number})</dt>
	 * <dd>The number of seconds after which the server may consider the job
	 * dead if this client cannot communicate with it. If <tt>null</tt>, no
	 * timeout will be used and the job will run until explicitly destroyed. Use
	 * with extreme caution.</dd>
	 * <dt>{@value JobConstants#MACHINE_PROPERTY} ({@link String})</dt>
	 * <dd>Specify the name of a machine which this job must be executed on. If
	 * <tt>null</tt>, the first suitable machine available will be used,
	 * according to the tags selected below. Must be <tt>null</tt> when
	 * {@value JobConstants#TAGS_PROPERTY} are given.</dd>
	 * <dt>{@value JobConstants#TAGS_PROPERTY} ({@link String}[])</dt>
	 * <dd>The set of tags which any machine running this job must have. If
	 * <tt>null</tt> is supplied, only machines with the <tt>"default"</tt> tag
	 * will be used. If {@value JobConstants#MACHINE_PROPERTY} is given, this
	 * argument must be <tt>null</tt>.</dd>
	 * <dt>{@value JobConstants#MIN_RATIO_PROPERTY} ({@link Double})</dt>
	 * <dd>The aspect ratio (h/w) which the allocated region must be 'at least
	 * as square as'. Set to <tt>0.0</tt> for any allowable shape, <tt>1.0</tt>
	 * to be exactly square etc. Ignored when allocating single boards or
	 * specific rectangles of triads.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_BOARDS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken or unreachable boards to allow in the
	 * allocated region. If <tt>null</tt>, any number of dead boards is
	 * permitted, as long as the board on the bottom-left corner is alive.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_LINKS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken links allow in the allocated region.
	 * When {@value JobConstants#REQUIRE_TORUS_PROPERTY} is true this includes
	 * wrap-around links, otherwise peripheral links are not counted. If
	 * <tt>null</tt>, any number of broken links is allowed.</dd>
	 * <dt>{@value JobConstants#REQUIRE_TORUS_PROPERTY} ({@link Boolean})</dt>
	 * <dd>If <tt>true</tt>, only allocate blocks with torus connectivity. In
	 * general this will only succeed for requests to allocate an entire
	 * machine. Must be <tt>false</tt> (or not supplied) when allocating
	 * boards.</dd>
	 * </dl>
	 *
	 * @param args
	 *            The machine shape description.
	 * @param kwargs
	 *            The extra properties.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	public SpallocJob(List<Integer> args, Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this(config.getHost(), config.getPort(), f2ms(config.getTimeout()),
				args, kwargs);
	}

	/**
	 * Create a spalloc job that requests a SpiNNaker machine.
	 * <p>
	 * The requested machine shape can be one of the following:
	 * <ul>
	 * <li><b>Empty</b> list, to get a single board.
	 * <li><b>Singleton</b> list, to get a machine with that number of boards.
	 * <li><b>Pair</b>, to get a rectangle of boards,
	 * <i>width</i>&times;<i>height</i>.
	 * <li><b>Triple</b>, to get a specific board (<i>x, y, z</i>).
	 * </ul>
	 * The supported extra properties consist of:
	 * <dl>
	 * <dt>{@value JobConstants#USER_PROPERTY} ({@link String})</dt>
	 * <dd>The name of the owner of the job. By convention this should be your
	 * email address.</dd>
	 * <dt>{@value JobConstants#KEEPALIVE_PROPERTY} ({@link Number})</dt>
	 * <dd>The number of seconds after which the server may consider the job
	 * dead if this client cannot communicate with it. If <tt>null</tt>, no
	 * timeout will be used and the job will run until explicitly destroyed. Use
	 * with extreme caution.</dd>
	 * <dt>{@value JobConstants#MACHINE_PROPERTY} ({@link String})</dt>
	 * <dd>Specify the name of a machine which this job must be executed on. If
	 * <tt>null</tt>, the first suitable machine available will be used,
	 * according to the tags selected below. Must be <tt>null</tt> when
	 * {@value JobConstants#TAGS_PROPERTY} are given.</dd>
	 * <dt>{@value JobConstants#TAGS_PROPERTY} ({@link String}[])</dt>
	 * <dd>The set of tags which any machine running this job must have. If
	 * <tt>null</tt> is supplied, only machines with the <tt>"default"</tt> tag
	 * will be used. If {@value JobConstants#MACHINE_PROPERTY} is given, this
	 * argument must be <tt>null</tt>.</dd>
	 * <dt>{@value JobConstants#MIN_RATIO_PROPERTY} ({@link Double})</dt>
	 * <dd>The aspect ratio (h/w) which the allocated region must be 'at least
	 * as square as'. Set to <tt>0.0</tt> for any allowable shape, <tt>1.0</tt>
	 * to be exactly square etc. Ignored when allocating single boards or
	 * specific rectangles of triads.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_BOARDS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken or unreachable boards to allow in the
	 * allocated region. If <tt>null</tt>, any number of dead boards is
	 * permitted, as long as the board on the bottom-left corner is alive.</dd>
	 * <dt>{@value JobConstants#MAX_DEAD_LINKS_PROPERTY} ({@link Integer})</dt>
	 * <dd>The maximum number of broken links allow in the allocated region.
	 * When {@value JobConstants#REQUIRE_TORUS_PROPERTY} is true this includes
	 * wrap-around links, otherwise peripheral links are not counted. If
	 * <tt>null</tt>, any number of broken links is allowed.</dd>
	 * <dt>{@value JobConstants#REQUIRE_TORUS_PROPERTY} ({@link Boolean})</dt>
	 * <dd>If <tt>true</tt>, only allocate blocks with torus connectivity. In
	 * general this will only succeed for requests to allocate an entire
	 * machine. Must be <tt>false</tt> (or not supplied) when allocating
	 * boards.</dd>
	 * </dl>
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param port
	 *            The spalloc server port
	 * @param timeout
	 *            The communications timeout
	 * @param args
	 *            The machine shape description.
	 * @param kwargs
	 *            The extra properties.
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	public SpallocJob(String hostname, Integer port, Integer timeout,
			List<Integer> args, Map<String, Object> kwargs)
			throws IOException, SpallocServerException {
		this.client = new SpallocClient(hostname, port, timeout);
		this.timeout = timeout;
		client.connect();
		if (args == null || args.size() > MAX_SHAPE_ARGS) {
			throw new IllegalArgumentException(
					"the machine shape description must have between 0 and "
							+ MAX_SHAPE_ARGS);
		}
		if (kwargs != null && kwargs.containsKey(RECONNECT_DELAY_PROPERTY)) {
			reconnectDelay = f2ms(kwargs.get(RECONNECT_DELAY_PROPERTY));
		} else {
			reconnectDelay = f2ms(config.getReconnectDelay());
		}
		kwargs = makeJobKeywordArguments(kwargs);
		id = client.createJob(args, kwargs, timeout);
		/*
		 * We also need the keepalive configuration so we know when to send
		 * keepalive messages.
		 */
		keepaliveTime = f2ms(kwargs.get(KEEPALIVE_PROPERTY));
		log.info("created spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	private static void setIfValid(Map<String, Object> map, String key,
			Object value, Class<?> clazz) {
		if (value == null) {
			return;
		}
		try {
			map.put(key, clazz.cast(value));
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("the \"" + key
					+ "\" property must map to a " + clazz.getSimpleName());
		}
	}

	/**
	 * Conditions the arguments for spalloc.
	 *
	 * @param kwargs
	 *            The arguments supplied by the caller. May be <tt>null</tt>.
	 * @param defaults
	 *            The set of defaults read from the configuration file.
	 * @return The actual arguments to give to spalloc.
	 * @throws IllegalArgumentException
	 *             if a bad argument is given.
	 */
	private Map<String, Object> makeJobKeywordArguments(
			Map<String, Object> kwargs) {
		if (kwargs == null) {
			kwargs = emptyMap();
		}
		Map<String, Object> defaults = config.getDefaults();
		Map<String, Object> map = new HashMap<>();
		map.put(USER_PROPERTY, kwargs.getOrDefault(USER_PROPERTY, defaults
				.getOrDefault(USER_PROPERTY, System.getProperty("user.name"))));
		if (!(map.get(USER_PROPERTY) instanceof String)) {
			throw new IllegalArgumentException(
					"the \"" + USER_PROPERTY + "\" key must map to a string");
		}
		setIfValid(map, KEEPALIVE_PROPERTY,
				kwargs.getOrDefault(KEEPALIVE_PROPERTY, defaults
						.getOrDefault(KEEPALIVE_PROPERTY, DEFAULT_KEEPALIVE)),
				Number.class);
		setIfValid(map, MACHINE_PROPERTY, kwargs.getOrDefault(MACHINE_PROPERTY,
				defaults.get(MACHINE_PROPERTY)), String.class);
		setIfValid(map, TAGS_PROPERTY,
				kwargs.getOrDefault(TAGS_PROPERTY, defaults.get(TAGS_PROPERTY)),
				String[].class);
		setIfValid(map, MIN_RATIO_PROPERTY,
				kwargs.getOrDefault(MIN_RATIO_PROPERTY,
						defaults.get(MIN_RATIO_PROPERTY)),
				Double.class);
		setIfValid(map, MAX_DEAD_BOARDS_PROPERTY,
				kwargs.getOrDefault(MAX_DEAD_BOARDS_PROPERTY,
						defaults.get(MAX_DEAD_BOARDS_PROPERTY)),
				Integer.class);
		setIfValid(map, MAX_DEAD_LINKS_PROPERTY,
				kwargs.getOrDefault(MAX_DEAD_LINKS_PROPERTY,
						defaults.get(MAX_DEAD_LINKS_PROPERTY)),
				Integer.class);
		setIfValid(map, REQUIRE_TORUS_PROPERTY,
				kwargs.getOrDefault(REQUIRE_TORUS_PROPERTY,
						defaults.get(REQUIRE_TORUS_PROPERTY)),
				Boolean.class);
		map.put(TIMEOUT_PROPERTY, timeout);
		return map;
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 */
	public SpallocJob(int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this(config.getHost(), config.getPort(), f2ms(config.getTimeout()), id);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 */
	public SpallocJob(String hostname, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this(hostname, config.getPort(), f2ms(config.getTimeout()), id);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param timeout
	 *            The communications timeout
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 */
	public SpallocJob(String hostname, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this(hostname, config.getPort(), timeout, id);
	}

	/**
	 * Converts a "float" number of seconds to milliseconds.
	 *
	 * @param obj
	 *            The number of seconds as a {@link Number} but up-casted to an
	 *            object for convenience.
	 * @return The number of milliseconds, suitable for use with Java timing
	 *         operations.
	 */
	private static Integer f2ms(Object obj) {
		if (obj == null) {
			return null;
		}
		return (int) (((Number) obj).doubleValue() * MS_PER_S);
	}

	/**
	 * Create a job client that resumes an existing job given its ID.
	 *
	 * @param hostname
	 *            The spalloc server host
	 * @param port
	 *            The TCP port
	 * @param timeout
	 *            The communications timeout
	 * @param id
	 *            The job ID
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 * @throws JobDestroyedException
	 *             If the job doesn't exist (any more).
	 */
	public SpallocJob(String hostname, int port, Integer timeout, int id)
			throws IOException, SpallocServerException, JobDestroyedException {
		this.client = new SpallocClient(hostname, port, timeout);
		this.timeout = timeout;
		this.id = id;
		client.connect();
		reconnectDelay = f2ms(config.getReconnectDelay());
		/*
		 * If the job no longer exists, we can't get the keepalive interval (and
		 * there's nothing to keepalive) so just bail out.
		 */
		JobState jobState = getStatus();
		if (jobState.getState() == UNKNOWN
				|| jobState.getState() == DESTROYED) {
			if (jobState.getReason() != null) {
				throw new JobDestroyedException(format(
						"SpallocJob %d does not exist: %s: %s", id,
						jobState.getState().name(), jobState.getReason()));
			} else {
				throw new JobDestroyedException(
						format("SpallocJob %d does not exist: %s", id,
								jobState.getState().name()));
			}
		}
		// Snag the keepalive interval from the job
		keepaliveTime = f2ms(jobState.getKeepalive());
		log.info("resumed spalloc job with ID: {}", id);
		launchKeepaliveDaemon();
	}

	private void launchKeepaliveDaemon() {
		log.info("launching keepalive thread for " + id + " with interval "
				+ (keepaliveTime / 2) + "ms");
		if (keepalive != null) {
			log.warn("launching second keepalive thread for " + id);
		}
		keepalive = new Thread(SPALLOC_WORKERS, this::keepalive,
				"keepalive for spalloc job " + id);
		keepalive.setDaemon(true);
		stopping = false;
		keepalive.start();
	}

	private void keepalive() {
		while (!stopping) {
			try {
				client.jobKeepAlive(id, timeout);
				if (!interrupted()) {
					sleep(keepaliveTime / 2);
				}
			} catch (IOException | SpallocServerException e) {
				stopping = true;
			} catch (InterruptedException e) {
				continue;
			}
		}
	}

	@Override
	public void close() throws IOException {
		stopping = true;
		keepalive.interrupt();
		client.close();
	}

	/**
	 * Reconnect to the server and check version.
	 *
	 * If reconnection fails, the error is reported as a warning but no
	 * exception is raised.
	 */
	private void reconnect() {
		try {
			client.connect(timeout);
			assertCompatibleVersion();
			log.info("Reconnected to spalloc server successfully");
		} catch (SpallocServerException | IOException e) {
			/*
			 * Connect/version command failed... Leave the socket clearly broken
			 * so that we retry again
			 */
			log.warn("Spalloc server is unreachable ({}), will keep trying...",
					e.getMessage());
			try {
				client.close();
			} catch (IOException inner) {
				// close failed?! Nothing we can do but log and try later
				log.error("problem closing connection", inner);
			}
		}
	}

	/**
	 * Assert that the server version is compatible. This client supports from
	 * 0.4.0 to 2.0.0 (but not including the latter).
	 *
	 * @throws IOException
	 *             If communications fail.
	 * @throws SpallocServerException
	 *             If the spalloc server rejects the operation request.
	 */
	protected void assertCompatibleVersion()
			throws IOException, SpallocServerException {
		Version v = client.version(timeout);
		if (MIN_VER.compareTo(v) <= 0 && MAX_VER.compareTo(v) > 0) {
			return;
		}
		client.close();
		throw new IllegalStateException(
				"Server version " + v + " is not compatible with this client");
	}

	@Override
	public void destroy(String reason)
			throws IOException, SpallocServerException {
		try {
			client.destroyJob(id, reason, timeout);
		} finally {
			close();
		}
		purgeStatus();
	}

	@Override
	public void setPower(Boolean powerOn)
			throws IOException, SpallocServerException {
		if (powerOn == null) {
			return;
		}
		if (powerOn) {
			client.powerOnJobBoards(id, timeout);
		} else {
			client.powerOffJobBoards(id, timeout);
		}
		purgeStatus();
	}

	@Override
	public int getID() {
		return id;
	}

	private JobState getStatus() throws IOException, SpallocServerException {
		if (statusCache == null || statusTimestamp < currentTimeMillis()
				- STATUS_CACHE_PERIOD) {
			statusCache = client.getJobState(id, timeout);
			statusTimestamp = currentTimeMillis();
		}
		return statusCache;
	}

	private void purgeStatus() {
		statusCache = null;
	}

	@Override
	public State getState() throws IOException, SpallocServerException {
		return getStatus().getState();
	}

	@Override
	public Boolean getPower() throws IOException, SpallocServerException {
		return getStatus().getPower();
	}

	@Override
	public String getDestroyReason()
			throws IOException, SpallocServerException {
		return getStatus().getReason();
	}

	private void retrieveMachineInfo()
			throws IOException, SpallocServerException {
		machineInfoCache = client.getJobMachineInfo(id, timeout);
	}

	@Override
	public List<Connection> getConnections()
			throws IOException, SpallocServerException {
		if (machineInfoCache == null
				|| machineInfoCache.getConnections() == null) {
			retrieveMachineInfo();
		}
		return machineInfoCache.getConnections();
	}

	@Override
	public String getHostname() throws IOException, SpallocServerException {
		for (Connection c : getConnections()) {
			if (c.getChip().onSameChipAs(ZERO_ZERO)) {
				return c.getHostname();
			}
		}
		return null;
	}

	@Override
	public MachineDimensions getDimensions()
			throws IOException, SpallocServerException {
		if (machineInfoCache == null || machineInfoCache.getWidth() == 0) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null || machineInfoCache.getWidth() == 0) {
			return null;
		}
		return new MachineDimensions(machineInfoCache.getWidth(),
				machineInfoCache.getHeight());
	}

	@Override
	public String getMachineName() throws IOException, SpallocServerException {
		if (machineInfoCache == null
				|| machineInfoCache.getMachineName() == null) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null) {
			return null;
		}
		return machineInfoCache.getMachineName();
	}

	@Override
	public List<BoardCoordinates> getBoards()
			throws IOException, SpallocServerException {
		if (machineInfoCache == null || machineInfoCache.getBoards() == null) {
			retrieveMachineInfo();
		}
		if (machineInfoCache == null || machineInfoCache.getBoards() == null) {
			return null;
		}
		return machineInfoCache.getBoards();
	}

	@Override
	public State waitForStateChange(State oldState, Integer timeout)
			throws SpallocServerException {
		Long finishTime = makeTimeout(timeout);

		// We may get disconnected while waiting so keep listening...
		while (!timedOut(finishTime)) {
			try {
				// Watch for changes in this SpallocJob's state
				client.notifyJob(id, true);

				// Wait for job state to change
				while (!timedOut(finishTime)) {
					// Has the job changed state?
					purgeStatus();
					State newState = getStatus().getState();
					if (newState != oldState) {
						return newState;
					}

					// Wait for a state change and keep the job alive
					if (!doWaitForAChange(finishTime)) {
						/*
						 * The user's timeout expired while waiting for a state
						 * change, return the old state and give up.
						 */
						return oldState;
					}
				}
			} catch (IOException e) {
				/*
				 * Something went wrong while communicating with the server,
				 * reconnect after the reconnection delay (or timeout, whichever
				 * came first).
				 */
				try {
					doReconnect(finishTime);
				} catch (IOException | InterruptedException e1) {
					log.error("problem when reconnecting after disconnect", e1);
				}
			}
		}

		/*
		 * If we get here, the timeout expired without a state change, just
		 * return the old state
		 */
		return oldState;
	}

	/**
	 * Wait for a state change and keep the job alive.
	 *
	 * @param finishTime
	 *            when our timeout expires, or <tt>null</tt> for never
	 * @return True if the state has changed, or false on timeout
	 * @throws SpallocServerException
	 * @throws IOException
	 */
	private boolean doWaitForAChange(Long finishTime)
			throws IOException, SpallocServerException {
		/*
		 * Since we're about to block holding the client lock, we must be
		 * responsible for keeping everything alive.
		 */
		while (!timedOut(finishTime)) {
			client.jobKeepAlive(id, timeout);

			// Wait for the job to change
			try {
				/*
				 * Block waiting for the job to change no-longer than the
				 * user-specified timeout or half the keepalive interval.
				 */
				Integer waitTimeout;
				if (finishTime != null && keepaliveTime != null) {
					waitTimeout = min(keepaliveTime / 2, timeLeft(finishTime));
				} else if (finishTime == null) {
					waitTimeout =
							(keepalive == null) ? null : keepaliveTime / 2;
				} else {
					waitTimeout = timeLeft(finishTime);
				}
				if (waitTimeout == null || waitTimeout >= 0) {
					client.waitForNotification(waitTimeout);
					return true;
				}
			} catch (SpallocProtocolTimeoutException e) {
				/*
				 * Its been a while, send a keep-alive since we're still holding
				 * the lock
				 */
			}
		}
		// The user's timeout expired while waiting for a state change
		return false;
	}

	/**
	 * Reconnect after the reconnection delay (or timeout, whichever came
	 * first).
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void doReconnect(Long finishTime)
			throws IOException, InterruptedException {
		client.close();
		int delay;
		if (finishTime != null) {
			delay = min(timeLeft(finishTime), reconnectDelay);
		} else {
			delay = reconnectDelay;
		}
		sleep(max(0, delay));
		reconnect();
	}

	@Override
	public void waitUntilReady(Integer timeout)
			throws JobDestroyedException, IOException, SpallocServerException,
			SpallocStateChangeTimeoutException {
		State curState = null;
		Long finishTime = makeTimeout(timeout);
		while (!timedOut(finishTime)) {
			if (curState == null) {
				/*
				 * Get initial state (NB: done here such that the command is
				 * never sent if the timeout has already occurred)
				 */
				curState = getStatus().getState();
			}

			// Are we ready yet?
			switch (curState) {
			case READY:
				log.info("job:{} is now ready", id);
				// Now in the ready state!
				return;
			case QUEUED:
				log.info("job:{} has been queued by the spalloc server", id);
				break;
			case POWER:
				log.info("waiting for board power commands to "
						+ "complete for job:{}", id);
				break;
			case DESTROYED:
				// In a state which can never become ready
				throw new JobDestroyedException(getDestroyReason());
			default: // UNKNOWN
				// Server has forgotten what this job even was...
				throw new JobDestroyedException(
						"Spalloc server no longer recognises job:" + id);
			}
			// Wait for a state change...
			curState = waitForStateChange(curState, timeLeft(finishTime));
		}
		// Timed out!
		throw new SpallocStateChangeTimeoutException();
	}

	@Override
	public BoardPhysicalCoordinates whereIs(HasChipLocation chip)
			throws IOException, SpallocServerException {
		WhereIs result = client.whereIs(id, chip, timeout);
		if (result == null) {
			throw new IllegalStateException(
					"received null instead of machine location");
		}
		return result.getPhysical();
	}

	/**
	 * @return The underlying client, allowing access to non-job-related
	 *         operations.
	 */
	public SpallocClient getClient() {
		return client;
	}
}
