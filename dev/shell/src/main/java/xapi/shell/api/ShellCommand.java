package xapi.shell.api;

import xapi.collect.api.Fifo;
import xapi.util.api.SuccessHandler;


public interface ShellCommand {

	final Integer STATUS_DESTROYED = -2;
	final Integer STATUS_FAILED = -1;
	
	/**
	 * @return The user to initiate the command (not yet used)
	 */
	String owner();
	/**
	 * @return A simple linked list with a join method;
	 * perfect for chaining together command fragments.
	 * (ProcessBuilder uses String[], but it's nice to have add 
	 * without array bounds checks, 
	 */
	Fifo<String> commands();
	/**
	 * @return - The direct in which to launch the command.
	 * Defaults to "."
	 */
	String directory();
	ShellCommand owner(String owner);
	ShellCommand directory(String directory);
	ShellCommand commands(String ... text);
	/**
	 * Starts the command; returns a future and accepts a callback.
	 * 
	 * @param callback - optional success handler, to allow pushing work forward
	 * @param processor - optional argument handler, to manipulate the command being run.
	 * @return - A {@link ShellSession} future, for platforms that can afford to block on results.
	 */
	ShellSession run(SuccessHandler<ShellSession> callback, ArgumentProcessor processor);
	
}
