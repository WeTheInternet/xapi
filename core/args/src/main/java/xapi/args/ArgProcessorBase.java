package xapi.args;

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
public class ArgProcessorBase {

  /**
   * Use a linked hash map to preserve the declaration order.
   */
  private final Map<String, ArgHandler> argHandlers = new LinkedHashMap<String, ArgHandler>();

  protected String getDescription() {
    return null;
  }

  protected String getName() {
    return getClass().getName();
  }

  protected void printHelp() {

    ArgHandler nullHandler = null;
    int widest = 0;
    for (ArgHandler handler : argHandlers.values()) {
      if (handler.isUndocumented()) {
        continue;
      }
      String tag = handler.getTag();
      if (tag != null) {
        if (tag.length() > widest) {
          widest = tag.length();
        }
      } else {
        nullHandler = handler;
        int len = nullHandler.getTagArgs()[0].length();
        if (len > widest) {
          widest = len;
        }
      }
    }

    // Print the name.
    //
    String name = getName();
    int i = name.lastIndexOf('.');
    if (i != -1) {
      name = name.substring(i + 1);
    }
    System.err.print(name);

    // Print the command-line template.
    //
    for (ArgHandler handler : argHandlers.values()) {
      if (handler.isUndocumented()) {
        continue;
      }
      String tag = handler.getTag();
      if (tag != null) {
        System.err.print(handler.isRequired() ? " " : " [");
        System.err.print(tag);
        String[] tagArgs = handler.getTagArgs();
        for (String tagArg : tagArgs) {
          System.err.print(" " + tagArg);
        }
        System.err.print(handler.isRequired() ? "" : "]");
      }
    }

    // Print the flagless args.
    //
    if (nullHandler != null && !nullHandler.isUndocumented()) {
      String[] tagArgs = nullHandler.getTagArgs();
      for (String element : tagArgs) {
        System.err.print(nullHandler.isRequired() ? " " : " [");
        System.err.print(element);
        System.err.print(nullHandler.isRequired() ? " " : "]");
      }
      System.err.println();
    }

    System.err.println();
    String description = getDescription();
    if (description != null) {
      System.err.println(description);
      System.err.println();
    }

    System.err.println("where ");

    // Print the details.
    //
    for (ArgHandler handler : argHandlers.values()) {
      if (handler.isUndocumented()) {
        continue;
      }
      String tag = handler.getTag();
      if (tag != null) {
        int len = tag.length();
        System.err.print("  ");
        System.err.print(tag);
        for (i = len; i < widest; ++i) {
          System.err.print(' ');
        }
        System.err.print("  ");
        System.err.print(handler.getPurpose());
        System.err.println();
      }
    }

    // And details for the "extra" args, if any.
    //
    if (nullHandler != null && !nullHandler.isUndocumented()) {
      System.err.println("and ");
      String tagArg = nullHandler.getTagArgs()[0];
      int len = tagArg.length();
      System.err.print("  ");
      System.err.print(tagArg);
      for (i = len; i < widest; ++i) {
        System.err.print(' ');
      }
      System.err.print("  ");
      System.err.print(nullHandler.getPurpose());
      System.err.println();
    }
  }

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
      String[] defArgs = def.getDefaultArgs();
      if (defArgs != null) {
        if (def.handle(defArgs, 0) == -1) {
          return false;
        }
      }
    }

    return true;
  }

  protected void registerHandler(ArgHandler handler) {
    String tag = handler.getTag();
    argHandlers.put(tag != null ? tag : "", handler);
  }  
  
}
