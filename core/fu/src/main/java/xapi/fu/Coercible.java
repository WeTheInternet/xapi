package xapi.fu;

import static xapi.fu.Immutable.immutable1;
import static xapi.fu.Out1.EMPTY_STRING;
import static xapi.fu.Out1.NEW_LINE;
import static xapi.fu.X_Fu.STRING_DUPLICATE;

import java.util.Iterator;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface Coercible {

  Out1<String> LEFT_BRACE = immutable1("[");
  Out1<String> RIGHT_BRACE = immutable1("]");

  default String listSeparator() {
    return "";
  }

  default String coerce(Object obj) {
    return coerce(obj, LEFT_BRACE, RIGHT_BRACE, NEW_LINE, EMPTY_STRING);
  }

  default String coerce(Object obj, boolean first) {
    StringBuilder b = new StringBuilder();
    if (!first) {
      b.append(perIndent());
      b.append(listSeparator());
    }
    b.append(coerce(obj, LEFT_BRACE, RIGHT_BRACE, NEW_LINE, EMPTY_STRING));
    return b.toString();
  }

  default String coerce(Object obj, Out1<String> before, Out1<String> after, Out1<String> newline, Out1<String> emptyString) {
    final StringBuilder b = new StringBuilder();
    final String listSeparator = listSeparator();
    final boolean printEdges = !listSeparator.isEmpty();

    final In1Out1<Object, String> descend = next -> coerce(next,
        before.map(STRING_DUPLICATE),
        after.map(STRING_DUPLICATE),
        newline.map(s ->
            ( s.startsWith("\n//") ?
                // This will print n repetitions of "//x\\" on each descended newline
                "\n////x\\\\\n" + s.substring(3).replaceFirst("\n", "//x\\\\\n") : s + perIndent() )
        ),
        emptyString
    );

    if (obj instanceof Iterable){
      Iterator i = ((Iterable)obj).iterator();
      if (i.hasNext()) {
        if (printEdges) {
          b.append(before.out1());
        }

        final Object next = i.next();
        if (isAddNewlines(next)) {
          String newlineSurround = newline.out1();
          b.append(newlineSurround);
        }

        b.append(descend.io(next));
        for (;i.hasNext();) {
          final Object value = i.next();
          b.append(listSeparator);
          b.append(descend.io(value));
        }

      } else {
        // Iterator was empty... print our empty string (notify it was empty)
        b.append(emptyString.out1());
      }

      if (printEdges) {
        b.append(after.out1());
      }

      return b.toString();
    } else if (obj != null && obj.getClass().isArray()){
      int max = X_Fu.getLength(obj);
      if (max == 0) {
        if (listSeparator.isEmpty()) {
          return emptyString.out1();
        }
        return b
            .append(before.out1())
            .append(emptyString.out1())
            .append(after.out1())
            .toString();
      }

      if (printEdges) {
        b.append(before.out1());
      }

      b.append(descend.io(X_Fu.getValue(obj, 0)));

      for (int i = 1; i < max; i++ ) {
        final Object value = X_Fu.getValue(obj, i);

        if (isAddNewlines(value)) {
          String newlineSurround = newline.out1();
          b.append(newlineSurround);
        }

        b.append(listSeparator);
        b.append(descend.io(value));
      }
      if (printEdges) {
        b.append(after.out1());
      }
      return b.toString();
    } else {
      return String.valueOf(obj);
    }
  }

  default boolean isAddNewlines(Object next) {
    return next instanceof Iterable || X_Fu.getLength(next) > 0 ||
        (next instanceof String && ((String)next).startsWith("\n//x"));
  }

  default String perIndent() {
    return "  ";
  }
}
