/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A serialisable request to spalloc. This is modelled on the calling convention
 * used by Python.
 *
 * @param <A>
 *            The type of arguments.
 */
public abstract sealed class Command<A> //
		permits CreateJobCommand, DestroyJobCommand, GetBoardAtPositionCommand,
		GetBoardPositionCommand, GetJobMachineInfoCommand, GetJobStateCommand,
		JobKeepAliveCommand, ListJobsCommand, ListMachinesCommand,
		NoNotifyJobCommand, NoNotifyMachineCommand, NotifyJobCommand,
		NotifyMachineCommand, PowerOffJobBoardsCommand, PowerOnJobBoardsCommand,
		VersionCommand, WhereIsJobChipCommand,
		WhereIsMachineBoardLogicalCommand, WhereIsMachineBoardPhysicalCommand,
		WhereIsMachineChipCommand, CustomIntCommand, CustomStringCommand {
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

	/** @return The command token. */
	public String getCommand() {
		return command;
	}

	/** @return The positional arguments to the command. */
	public List<A> getArgs() {
		return args;
	}

	/** @return The keyword arguments to the command. */
	public Map<String, Object> getKwargs() {
		return kwargs;
	}
}
