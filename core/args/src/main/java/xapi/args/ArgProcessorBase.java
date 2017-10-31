package xapi.args;

import xapi.fu.Out1;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A basic argument processor, based on the equivalent class in GWT.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public class ArgProcessorBase extends ArgProcessorAbstract {

  public boolean processArgs(String... args) {
    if (args.length > 0) {
      boolean help = false;
      if ("-help".equalsIgnoreCase(args[0])) {
        help = true;
      } else if ("-?".equals(args[0])) {
        help = true;
      }

      if (help) {
        printHelp();
        return false;
      }
    }

    Set<ArgHandler> defs = new HashSet<ArgHandler>(argHandlers.values());
    int extraArgCount = 0;

    Set<ArgHandler> receivedArg = new HashSet<ArgHandler>();

    // Let the args drive the handlers.
    //
    ArgHandler nullHandler = argHandlers.get("");
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      ArgHandler handler;
      if (arg.startsWith("-")) {
        // Use the handler registered for this flag.
        //
        handler = argHandlers.get(arg);
      } else {
        // Use the handler that doesn't have a leading flag.
        //
        handler = nullHandler;
        ++extraArgCount;
      }

      if (handler == null) {
        System.err.println("Unknown argument: " + arg);
        printHelp();
        return false;
      }

      int addtlConsumed = handler.handle(args, i);
      if (addtlConsumed == -1) {
        printHelp();
        return false;
      }

      i += addtlConsumed;

      // We don't need to use this as a default handler.
      //
      defs.remove(handler);

      // Record that this handler saw a value
      //
      receivedArg.add(handler);
    }

    // See if any handler didn't get its required argument(s).
    //
    for (ArgHandler argHandler : argHandlers.values()) {
      if (argHandler.isRequired() && !receivedArg.contains(argHandler)) {
        System.err.print("Missing required argument '");
        String tag = argHandler.getTag();
        if (tag != null) {
          System.err.print(tag);
          System.err.print(" ");
        }

        String tagArg = argHandler.getTagArgs()[0];
        System.err.print(tagArg);
        System.err.println("'");

        printHelp();
        return false;
      }
    }
    if (extraArgCount == 0 && nullHandler != null && nullHandler.isRequired()) {
      System.err.print("Missing required argument '");
      String tagArg = nullHandler.getTagArgs()[0];
      System.err.print(tagArg);
      System.err.println("'");
      printHelp();
      return false;
    }

    // Set if there are any remaining unused handlers with default arguments.
    // Allow the default handlers to pretend there were other arguments.
    //
    for (ArgHandler def : defs) {
      Out1<String>[] defArgs = def.getDefaultArgs();
      if (defArgs != null) {
        if (def.handle(defArgs, 0) == -1) {
          return false;
        }
      }
    }

    return true;
  }

}
