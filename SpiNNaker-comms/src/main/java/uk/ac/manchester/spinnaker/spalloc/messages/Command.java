package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialisable request to spalloc.
 *
 * @param <A>
 *            The type of arguments.
 */
public abstract class Command<A> {
	private final String command;
	private final List<A> args = new ArrayList<>();
	private final Map<String, Object> kwargs = new HashMap<>();

	/**
	 * Add to the keyword arguments part.
	 *
	 * @param key
	 *            The keyword
	 * @param value
	 *            The argument value; will be converted to a string
	 */
	protected final void addKwArg(String key, Object value) {
		kwargs.put(key, value);
	}

	/**
	 * Add to the positional arguments part.
	 *
	 * @param values
	 *            The arguments to add.
	 */
	@SafeVarargs
	protected final void addArg(A... values) {
		for (final A value : values) {
			args.add(value);
		}
	}

	/**
	 * Create a command.
	 *
	 * @param command
	 *            The command token.
	 */
	public Command(String command) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public List<A> getArgs() {
		return args;
	}

	public Map<String, Object> getKwargs() {
		return kwargs;
	}
}
