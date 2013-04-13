package xapi.shell.service;

import xapi.io.api.LineReader;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellResult;

public interface ShellService {

	ShellCommand newCommand(String ... cmds);
	
	ShellResult runInShell(String cmd, LineReader stdIn, LineReader stdErr);
	
}
