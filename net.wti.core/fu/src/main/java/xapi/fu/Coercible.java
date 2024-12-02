package xapi.fu;

import xapi.fu.api.AutoOverload;
import xapi.fu.api.DoNotOverride;
import xapi.fu.api.ShouldOverride;
import xapi.fu.has.HasResolution;

import java.util.Iterator;

import static xapi.fu.Immutable.immutable1;
import static xapi.fu.Out1.EMPTY_STRING;
import static xapi.fu.Out1.NEW_LINE;
import static xapi.fu.X_Fu.STRING_DUPLICATE;

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

  @DoNotOverride
  default String coerce(Object obj) {
    return coerce(false, obj, LEFT_BRACE, RIGHT_BRACE, NEW_LINE, EMPTY_STRING);
  }

  @DoNotOverride
  default String coerce(Object obj, boolean first) {
    StringBuilder b = new StringBuilder();
    if (!first) {
      b.append(perIndent());
      b.append(listSeparator());
    }
    b.append(coerce(first, obj, LEFT_BRACE, RIGHT_BRACE, NEW_LINE, EMPTY_STRING));
    return b.toString();
  }

  /**
   * All other coerce methods funnel to here, where we handle iterables and arrays,
   * along with escaping those list items.
   *
   * You should only override this method if you want direct access to
   * iterables before they are unwrapped.
   *
   * If you want this high-levels of control,
   * it comes at the cost of a higher complexity method signature.
   *
   * @param first A boolean that is passed along for subtypes which want to special-case the first value of an array
   * @param obj The object to perform coercion upon
   * @param before A provider to add text before each coerced value
   * @param after A provider to add text after each coerced value
   * @param newline A provider to print newlines (and additional indent)
   * @param emptyString A provider to print values when array/list is empty
   * @return The final coerced string output
   */
  // @ShouldProbablyNotOverride ;-)
  default String coerce(boolean first, Object obj, Out1<String> before, Out1<String> after, Out1<String> newline, Out1<String> emptyString) {
    if (obj == null) {
      return "null";
    }
    final StringBuilder b = new StringBuilder();
    final String listSeparator = listSeparator();
    final boolean printEdges = !listSeparator.isEmpty();

    // descender is used for nested items.
    final In2Out1<Boolean, Object, String> descend = (first_, next) -> coerce(first_,next,
        before.map(STRING_DUPLICATE),
        after.map(STRING_DUPLICATE),
        // add extra spaces on each indentation.
        newline.map(s -> s + perIndent() ),
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

        b.append(descend.io(first, next));
        for (;i.hasNext();) {
          final Object value = i.next();
          b.append(listSeparator);
          b.append(descend.io(false, value));
        }

      } else {
        // Iterator was empty... print our empty string (notify it was empty)
        b.append(emptyString.out1());
      }

      if (printEdges) {
        b.append(after.out1());
      }

      return b.toString();
    } else if (obj.getClass().isArray()){
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

      b.append(descend.io(true, X_Fu.getValue(obj, 0)));

      for (int i = 1; i < max; i++ ) {
        final Object value = X_Fu.getValue(obj, i);

        if (isAddNewlines(value)) {
          String newlineSurround = newline.out1();
          b.append(newlineSurround);
        }

        b.append(listSeparator);
        b.append(descend.io(false, value));
      }
      if (printEdges) {
        b.append(after.out1());
      }
      return b.toString();
    } else {
      return coerceNonArray(obj, first);
    }
  }

  @ShouldOverride
  default String coerceNonArray(Object obj, boolean first) {
    if (obj instanceof Out1) {
      if (obj instanceof HasResolution) {
        // do not resolve unresolved objects when coercing.
        if (((HasResolution) obj).isResolved()) {
          return coerce(((Out1) obj).out1(), first);
        }
        // falls through to String.valueOf()
      } else {
          return coerce(((Out1) obj).out1(), first);
      }
    } else if (obj instanceof Class) {
      // Log overrides this method to create stack-trace friendly variations of log messages
      return ((Class) obj).getCanonicalName();
    }

    return String.valueOf(obj);
  }

    default boolean isAddNewlines(Object next) {
        if (next == null) {
            return false;
        }
        return next instanceof Iterable ||
            (
                next instanceof String
                    ? ((String) next).startsWith("\n//x")
                    : next.getClass().getComponentType() != null && X_Fu.getLength(next) > 0
            );
    }

  default String perIndent() {
    return "  ";
  }

  class DelegatingCoercible implements Coercible {
    private Coercible coercer = this;

    @Override
    public String coerce(
        boolean first,
        Object obj,
        Out1<String> before,
        Out1<String> after,
        Out1<String> newline,
        Out1<String> emptyString
    ) {
      if (coercer == this) {
        return Coercible.super.coerce(first, obj, before, after, newline, emptyString);
      }
      return coercer.coerce(first, obj, before, after, newline, emptyString);
    }

    @Override
    public String coerceNonArray(Object obj, boolean first) {
      if (coercer == this) {
        return Coercible.super.coerceNonArray(obj, first);
      }
      return coercer.coerceNonArray(obj, first);
    }

    public Coercible getCoercer() {
      return coercer;
    }

    @AutoOverload
    public DelegatingCoercible setCoercer(Coercible coercer) {
      this.coercer = coercer;
      return this;
    }
  }
}
