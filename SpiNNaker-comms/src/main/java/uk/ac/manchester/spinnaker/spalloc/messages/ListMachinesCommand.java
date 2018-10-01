package uk.ac.manchester.spinnaker.spalloc.messages;

/**
 * Request to get the known machines from the spalloc service.
 */
public class ListMachinesCommand extends Command<String> {
    /** Create a request to list the known SpiNNaker machines. */
    public ListMachinesCommand() {
        super("list_machines");
    }
}
