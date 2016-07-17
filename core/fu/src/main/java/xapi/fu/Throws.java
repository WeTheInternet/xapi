package xapi.fu;

import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface Throws extends Rethrowable {
    static <O> Out1<O> throwAsOut1(Out1<Throwable> toThrow) {
        return ()->{
            throw toThrow.rethrow(toThrow.out1());
        };
    }

    static <O> Out1<O> throwAsOut1Immediate(Out1<Throwable> toThrow) {
        return throwAsOut1(immutable1(toThrow.out1()));
    }

    static <I1> In1<I1> throwAsIn1(Out1<Throwable> toThrow) {
        return i1->{
            throw toThrow.rethrow(toThrow.out1());
        };
    }

    static <I1> In1<I1> throwAsIn1Immediate(Out1<Throwable> toThrow) {
        return throwAsIn1(immutable1(toThrow.out1()));
    }

    static <O1, O2> Out2<O1, O2> throwAsOut2(Out1<Throwable> toThrow) {
        return ()->{
            throw toThrow.rethrow(toThrow.out1());
        };
    }

    static <O1, O2> Out2<O1, O2> throwAsOut2Immediate(Out1<Throwable> toThrow) {
        return throwAsOut2(immutable1(toThrow.out1()));
    }

    static <I1, I2> In2<I1, I2> throwAsIn2(Out1<Throwable> toThrow) {
        return (i1, i2)->{
            throw toThrow.rethrow(toThrow.out1());
        };
    }

    static <I1, I2> In2<I1, I2> throwAsIn2Immediate(Out1<Throwable> toThrow) {
        return throwAsIn2(immutable1(toThrow.out1()));
    }

    static <I1, O1> In1Out1<I1, O1> throwAsIn1Out1(Out1<Throwable> toThrow) {
        return i1->{
            throw toThrow.rethrow(toThrow.out1());
        };
    }

    static <I1, O1> In1Out1<I1, O1> throwAsIn1Out1Immediate(Out1<Throwable> toThrow) {
        return throwAsIn1Out1(immutable1(toThrow.out1()));
    }

}
