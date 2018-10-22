package uk.ac.manchester.spinnaker.connections.model;

import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;

/**
 * A receiver of EIEIO data or commands.
 */
public interface EIEIOReceiver extends Connection,
		MessageReceiver<EIEIOMessage<? extends EIEIOHeader>> {
}
