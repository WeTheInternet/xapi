package xapi.fu.api;

import xapi.fu.Filter.Filter1;
import xapi.fu.Immutable;
import xapi.fu.Maybe;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.Out2;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/29/16.
 */
public interface Prioritizable {

    interface IsPrioritizable extends Prioritizable {
        default int getPriority() {return 0;}
    }

    @SuppressWarnings("all")
    class ImmutablePrioritizable extends Immutable<Integer> implements Prioritizable {

        public ImmutablePrioritizable(int priority) {
            super(priority);
        }

        /**
         * Do not override this; instead override out1().
         * @return a null check of the return value of Out1;
         * null == Integer.MIN_VALUE
         *
         */
        @Override
        public final int getPriority() {
            final Integer p = out1();
            return p == null ? Integer.MIN_VALUE : out1();
        }

        private Object writeReplace() {
            return new MutablePrioritizable(out1());
        }

    }

    class MutablePrioritizable extends Mutable<Integer> implements Serializable, Prioritizable {

        public MutablePrioritizable() {
            this(0);
        }

        public MutablePrioritizable(Integer val) {
            super(val == null ? Integer.MIN_VALUE : val);
        }

        public final MutablePrioritizable setPriority(Integer value) {
            in(value == null ? Integer.MIN_VALUE : value);
            return this;
        }

        @Override
        public final int getPriority() {
            final Integer p = out1();
            return p == null ? Integer.MIN_VALUE : out1(); // Null safe, if you use constructor or setPriority().
            // Use of .in1() directly allows null via out1(), but not the getter.
        }

        public Object readResolve() {
            return new ImmutablePrioritizable(out1());
        }
    }


    Prioritizable WORST = () -> Integer.MIN_VALUE;
    Prioritizable BEST = () -> Integer.MAX_VALUE;
    Prioritizable DEFAULT = () -> 0;

    int getPriority();

    static Prioritizable selectBest(Iterable<Prioritizable> all, Out1<Prioritizable> dflt) {

        for (Prioritizable prioritizable : all) {

        }

        return dflt.out1();
    }
    static Out2<Maybe<Prioritizable>, Object> selectUnknown(Iterable<?> all, Filter1<Prioritizable> filter) {

        Object last = null;
        for(Object o : all) {
            if (o instanceof Prioritizable) {
                if (filter.filter1((Prioritizable) o)) {
                    return Out2.out2Immutable(Maybe.immutable((Prioritizable)o), o);
                }
                if (!(last instanceof Prioritizable)) {
                    last = o;
                }
            }
            if (last == null && o != null) {
                last = o;
            }
        }

        return Out2.out2Immutable(Maybe.not(), last);
    }

    static Out2<Maybe<Prioritizable>, Object> selectUnknown(Filter1<Prioritizable> filter, Object ... all) {
        return selectUnknown(Arrays.asList(all), filter);
    }

}
