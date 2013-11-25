package xapi.shell.api;

import java.util.concurrent.Future;

import xapi.io.api.LineReader;
import xapi.util.api.Destroyable;

public interface ShellSession extends Destroyable{

	/**
	 * @return - The shell command which spawned us.
	 */
	ShellCommand parent();
	/**
	 * @return pid -> not yet implemented
	 */
	int pid();
	/**
	 * Destroy the process.
	 */
	void destroy();
	/**
	 * Wait for the process to die.
	 */
	int join();
	
	/**
	 * @return true if the process is not yet dead
	 */
	boolean isRunning();
	
	/**
	 * @return milliseconds since epoch this command was launched
	 */
	
	double birth();
	/**
	 * @return - A future that will block on the process, then return exit status.
	 */

	Future<Integer> exitStatus();
	
	/**
	 * @return - A future that will block on the process, then return stdIn.
	 */
	ShellSession stdOut(LineReader reader);
	
	/**
	 * @return - A future that will block on the process, then return stdOut.
	 */
	ShellSession stdErr(LineReader reader);
	
	/**
	 * Send a string to the stdin of the given command.
	 * A string like ^C is handy to forward to running shell ;)
	 * 
	 * @param string - Command to pipe to stdIn; buffered if other commands are pending.
	 * @return true if the command was accepted right away, false if it was buffered.
	 */
	boolean stdIn(String string);
	
}
