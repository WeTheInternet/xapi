package xapi.shell.service;

import xapi.io.api.LineReader;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;

public interface ShellService {

	ShellCommand newCommand(String ... cmds);
	
	ShellSession runInShell(boolean keepAlive, LineReader stdIn, LineReader stdErr, String ... cmds);
	
}
