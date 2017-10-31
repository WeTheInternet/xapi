package xapi.args;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/20/17.
 */
public class ArgProcessorAbstract {

    /**
     * Use a linked hash map to preserve the declaration order.
     */
    protected final Map<String, ArgHandler> argHandlers = new LinkedHashMap<String, ArgHandler>();

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

    protected void registerHandler(ArgHandler handler) {
        String tag = handler.getTag();
        argHandlers.put(tag != null ? tag : "", handler);
    }

}
