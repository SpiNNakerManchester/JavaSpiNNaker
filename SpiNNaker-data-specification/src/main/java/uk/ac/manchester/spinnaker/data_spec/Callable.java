package uk.ac.manchester.spinnaker.data_spec;

import uk.ac.manchester.spinnaker.data_spec.exceptions.DataSpecificationException;

/**
 * How to actually call a DSE operation.
 *
 * @author Donal Fellows
 */
@FunctionalInterface
interface Callable {
	/**
	 * The outer interface of a DSE operation. Note that this is subject to
	 * coercion to make the actual operations have a wider range of supported
	 * types.
	 *
	 * @param cmd
	 *            The encoded command word.
	 * @return Usually <tt>0</tt>. Sometimes a marker to indicate special
	 *         states (currently just for end-of-specification).
	 * @throws DataSpecificationException
	 *             If anything goes wrong in the data specification.
	 */
	int execute(int cmd) throws DataSpecificationException;
}
