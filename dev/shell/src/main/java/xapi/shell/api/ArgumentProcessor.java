package xapi.shell.api;

import java.util.ArrayList;

import xapi.util.api.ConvertsValue;

public interface ArgumentProcessor extends ConvertsValue<Iterable<String>, String[]>{

	final ArgumentProcessor NO_OP = new NoOp();
	
}
class NoOp implements ArgumentProcessor {
	@Override
	public String[] convert(Iterable<String> cmds) {
		ArrayList<String> commands = new ArrayList<String>();
		for (String cmd : cmds) {
			commands.add(cmd);
		}
		return commands.toArray(new String[commands.size()]);
	}
};
