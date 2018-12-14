package uk.ac.manchester.spinnaker.messages.model;

/**
 * Application identifiers are used by SCAMP to tie various resources to their
 * owners.
 *
 * @author Donal Fellows
 */
public final class AppID {
	/**
	 * Maximum app ID.
	 */
	private static final int MAX_APP_ID = 255;

	/**
	 * Th default application ID, used when code doesn't have a better idea.
	 */
	public static final AppID DEFAULT = new AppID(0);

	/**
	 * The application ID.
	 */
	public final int appID;

	public AppID(int appID) {
		if (appID < 0 || appID > MAX_APP_ID) {
			throw new IllegalArgumentException(
					"appID must be between 0 and " + MAX_APP_ID);
		}
		this.appID = appID;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AppID) {
			AppID other = (AppID) o;
			return appID == other.appID;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (appID << 5) ^ 1236984681;
	}
}
