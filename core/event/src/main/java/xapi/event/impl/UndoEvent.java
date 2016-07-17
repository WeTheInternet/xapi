package xapi.event.impl;

import xapi.event.api.HasBeforeAfter;
import xapi.event.api.IsEvent;
import xapi.event.api.IsEventType;
import xapi.fu.Out1;
import xapi.fu.Out3;

import static xapi.fu.Out3.out3;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/16/16.
 */
public interface UndoEvent<Source, Type> extends IsEvent<Source>, HasBeforeAfter<Type> {

    Out3<Source, Type, Type> values();

    @Override
    default Source getSource() {
        return values().out1();
    }

    @Override
    default Type getBefore() {
        return values().out2();
    }

    @Override
    default Type getAfter() {
        return values().out3();
    }

    @Override
    default IsEventType getType() {
        return EventTypes.Undo;
    }

    default ChangeEvent<Source, Type> toChange() {
        return ChangeEvent.change(getSource(), getAfter(), getBefore());
    }

    static <S, T> UndoEvent<S, T> undo(S source, T before, T after) {
        return () -> out3(source, before, after);
    }

    static <S, T> UndoEvent<S, T> undo(Out1<S> source, Out1<T> before, Out1<T> after) {
        return () -> out3(source, before, after);
    }
}
