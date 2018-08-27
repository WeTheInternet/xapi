package xapi.dev.source;

import xapi.source.write.Template;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/5/18.
 */
public interface PatternPrinter <S extends PatternPrinter<S>> {

    String NEW_LINE_REGEX = "\\r|(?:\\r?\\n)";
    char NEW_LINE = '\n';

    S println();
    S print(CharSequence chars);

    default S printlns(String chars) {
        for (String line : chars.split(NEW_LINE_REGEX)) {
            print(line);
            println();
        }
        return self();
    }

    S self();

    default S pattern(CharSequence source, Object replace1) {
        Template template = new Template(source.toString(), "$1");
        print(template.apply(replace1));
        return self();
    }

    default S patternln(CharSequence source, Object replace1) {
        pattern(source, replace1);
        println();
        return self();
    }

    default S pattern(CharSequence source, Object replace1, Object ... more) {
        final String resolved = resolve(source, replace1, more);
        print(resolved);
        return self();
    }

    default String resolve(CharSequence source, Object replace1, Object[] more) {
        String[] replaceables = new String[more.length + 1];
        Object[] args = new Object[more.length + 1];
        args[0] = replace1;
        System.arraycopy(more, 0, args, 1, more.length);
        for (int i = 0; i < replaceables.length; ) {
            replaceables[i] = "$" + (++i);
        }
        Template template = new Template(source.toString(), replaceables);
        final String resolved = template.apply(args);
        return resolved;
    }

    default S patternln(CharSequence source, Object replace1, Object ... more) {
        pattern(source, replace1, more);
        println();
        return self();
    }

    default S patternlns(CharSequence source, Object replace1, Object ... more) {
        final String resolved = resolve(source, replace1, more);
        printlns(resolved);
        return self();
    }

}
