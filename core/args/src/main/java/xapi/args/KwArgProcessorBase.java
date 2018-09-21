package xapi.args;

import xapi.fu.In2;
import xapi.fu.Out1;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;

import java.util.HashSet;
import java.util.Set;

/**
 * "keyword arguments" are a python construct whose name has slipped into general use;
 * basically, this processor looks for `-name value` pairs of arguments,
 * removing them from the argument list, which can be passed along to other argument processors.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/20/17.
 */
public class KwArgProcessorBase extends ArgProcessorAbstract {

    public String[] processArgs(String ... args) {
        return processArgs(In2.NULL, args);
    }

    public String[] processArgs(In2<Boolean, String[]> success, String ... args) {
        boolean help = false;
        if (args.length > 0) {
            if ("-help".equalsIgnoreCase(args[0])) {
                help = true;
            } else if ("-?".equals(args[0])) {
                help = true;
            }

            if (help) {
                printHelp();
                success.in(false, args);
                return args;
            }
        }
        Set<ArgHandler> defs = new HashSet<ArgHandler>(argHandlers.values());

        Set<ArgHandler> receivedArg = new HashSet<ArgHandler>();
        ChainBuilder<String> unused = Chain.startChain();
        // Let the args drive the handlers.
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            ArgHandler handler;
            boolean spy = false;
            if (arg.startsWith("-")) {
                // Use the handler registered for this flag.
                handler = argHandlers.get(arg);
                spy = shouldSpy(handler, arg);
            } else {
                unused.add(arg);
                continue;
            }

            if (handler == null) {
                unused.add(arg);
                continue;
            }

            int addtlConsumed = handler.handle(args, i);
            if (addtlConsumed == -1) {
                unused.add(arg);
                help = true;
                continue;
            }


            i += addtlConsumed;
            if (spy) {
                // when spying, put our -name and all the values we just read back.
                unused.add(arg);
                while (addtlConsumed --> 0) {
                    unused.add(args[i-addtlConsumed]);
                }
            }
            if (!handler.isMultiUse()) {
                defs.remove(handler);
            }

            // Record that this handler saw a value
            receivedArg.add(handler);
        }

        // See if any handler didn't get its required argument(s).
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

                help = true;
            }
        }

        // Handle any of our remaining defs who have default args for us to use.
        for (ArgHandler def : defs) {
            Out1<String>[] defArgs = def.getDefaultArgs();
            if (defArgs != null) {
                if (def.handle(defArgs, 0) == -1) {
                    help = true;
                }
            }
        }
        if (help) {
            System.err.println("Some errors detected processing args: " + ArrayIterable.iterate(args));
            printHelp();
        }
        final String[] result = unused.toArray(String[]::new);
        success.in(!help, result);
        return result;
    }

    private boolean shouldSpy(ArgHandler handler, String arg) {
        return handler != null && handler.spyTags().anyMatch(arg::equals);
    }
}
