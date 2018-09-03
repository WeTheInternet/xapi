package xapi.fu.later;

import xapi.fu.Frozen;
import xapi.fu.Lazy;
import xapi.fu.Log;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/11/17.
 */
public class FrozenLater <Success, Fail> implements Later<Success, Fail>, Frozen {
    private final Lazy<Success> success;
    private final Lazy<Fail> fail;

    public FrozenLater(Success success, Fail fail) {
        this.success = Lazy.immutable1(success);
        this.fail = Lazy.immutable1(fail);
    }

    public FrozenLater(Out1<Success> success, Out1<Fail> fail) {
        this.success = Lazy.deferred1(success);
        this.fail = Lazy.deferred1(fail);
    }

    @Override
    public boolean isFrozen() {
        return true;
    }

    @Override
    public void resolve(Success success, Fail fail) {
        // We will make it legal to call resolve on a frozen Later only when you are supplying the
        // two exact instances of each result; i.e., you have multiple processing paths contributing to the same result,
        // but that result is always stable after it is first used to resolve us.

        if (this.success.isResolved()) {
            if (this.success.out1() != success) {
                Log.loggerFor(Later.class, this)
                    .log(FrozenLater.class, "Attempt to change frozen success of:\n" + this.success + "\nto:\n" + success);
                throw new IllegalStateException("Cannot change state of a frozen Later");
            }
        } else if (success == null){
            this.success.out1();
        } else {

        }
        if (this.fail.out1() != fail) {
            Log.loggerFor(Later.class, this)
                .log(FrozenLater.class, "Attempt to change frozen fail of:\n" + this.fail + "\nto:\n" + fail);
            throw new IllegalStateException("Cannot change state of a frozen Later");
        }
    }
}
