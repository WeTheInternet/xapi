package xapi.dev.source;

import xapi.fu.ReturnSelf;

import java.lang.reflect.Modifier;
import java.util.Iterator;

import static xapi.dev.source.PrintBuffer.NEW_LINE;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/18.
 */
public interface VarBuffer <Self extends CharBuffer & VarBuffer<Self>> extends ReturnSelf<Self> {

    default String toVarDefinition() {

        Self self = self();
        final StringBuilder b = new StringBuilder(Character.toString(NEW_LINE));
        PrintBuffer javaDoc = getJavaDoc();
        if (javaDoc != null && javaDoc.isNotEmpty()) {
            b.append(javaDoc.toString());
        }

        String origIndent = getOrigIndent();
        b.append(origIndent);
        Iterable<String> annotations = getAnnotations();
        for (final String anno : annotations) {
            b.append('@').append(anno).append(NEW_LINE).append(origIndent);
        }
        int modifier = getModifier();
        String mods = Modifier.toString(modifier);
        if (self instanceof MemberBuffer) {
            final MemberBuffer enclosing = ((MemberBuffer) self).getEnclosing();
            if (enclosing instanceof ClassBuffer && (((ClassBuffer)enclosing).isInterface())) {
                mods = mods.replaceAll("(public|static|final)\\s*", "");
            }
            if (self instanceof LocalVariable) {
                mods = mods.replaceAll("(public|protected|private|transient|static)\\s*", "");
            }
        }
        if (!mods.isEmpty()) {
            b.append(mods).append(" ");
        }
        // generics
        Iterable<String> generics = getGenerics();
        final Iterator<String> itr = generics.iterator();
        if (itr.hasNext()) {
            b.append("<");
            String prefix = "";
            do {
                String generic = itr.next();
                b.append(prefix).append(generic);
                prefix = ", ";
            } while (itr.hasNext());
            b.append("> ");
        }
        // field type
        String simpleType = getSimpleType();
        b.append(simpleType).append(" ");
        // field name
        String fieldName = getName();
        b.append(fieldName);
        PrintBuffer initializer = getInitializer();
        final String init = initializer == null ? "" : initializer.toSource();
        final String trimmed = init.trim();
        if (trimmed.length() > 0) {
            b.append(" = ").append(init);
            if (!trimmed.endsWith(";")) {
                b.append(";");
            }
        } else {
            b.append(";");
        }
        b.append("\n");
        return b.toString();
    }

    PrintBuffer getInitializer();

    String getName();

    String getSimpleType();

    Iterable<String> getGenerics();

    int getModifier();

    Iterable<String> getAnnotations();

    String getOrigIndent();

    PrintBuffer getJavaDoc();
}
