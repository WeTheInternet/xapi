package xapi.annotation.compile;

/**
 * Used to denote an entry point into your application,
 * for the purposes of pruning.
 *
 * If the pruner misbehaves, simply annotate an unused method with @Entry,
 * and type in a bunch of code to force the visitor to rescue your types.
 *
 * In order to encourage pruning, there is a conditional mechanism;
 * a list of classes which turn String->Boolean ({@link #condition()},
 * and a list of Strings {@link #value()}.
 *
 * The default conditional uses {@link CheckTruth}, which will return null (no preference),
 * unless the strings "true", "false", "1" or "0" are present.
 *
 *
 * This annotation gives you the power to add useful contextual information
 * (which can be accessed at build time) about whether or not an entry point should be considered.
 *
 * By default, most of the time, you will just add @Entry and leave it there.
 * If you decide to prune something manually, just change to @Entry("false").
 *
 * If you need something more exotic, keep in mind that your EntryCondition classes
 * will likely need to be on the annotation processor classpath to work correctly.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/23/18 @ 10:40 PM.
 */
public @interface Entry {

    Class<? extends EntryCondition>[] condition() default {CheckTruth.class};
    String[] value() default {};

    interface EntryCondition {

        Boolean check(String value);

        default Boolean truth(String value) {
            if (value == null) {
                return null;
            }
            switch (value.toLowerCase()) {
                case "true":
                case "1":
                    return true;
                case "false":
                case "0":
                    return false;
                default:
                    return null;
            }
        }
    }

    final class CheckTruth implements EntryCondition {

        @Override
        public final Boolean check(String value) {
            return truth(value);
        }
    }
    final class DefaultFalse implements EntryCondition {

        @Override
        public final Boolean check(String value) {
            final Boolean result = truth(value);
            return result == null ? false : result;
        }
    }

    final class DefaultTrue implements EntryCondition {

        @Override
        public final Boolean check(String value) {
            final Boolean result = truth(value);
            return result == null ? true : result;
        }
    }

}
