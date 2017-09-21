package xapi.io.api;

import xapi.dev.source.CharBuffer;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Out2;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/19/17.
 */
public interface StreamDelegate {

    static Out2<In1<Integer>, Do> fromLineReader(LineReader r) {
        return fromLineReader(r, true, '\n', '\r');
    }

    static Out2<In1<Integer>, Do> fromLineReader(LineReader r, int ... delimiters) {
        return fromLineReader(r, false, delimiters);
    }

    static Out2<In1<Integer>, Do> fromLineReader(LineReader r, boolean compressEmpty, int ... delimiters) {

        final In1<Integer> onRead;
        boolean[] sawDelimiter = {false};
        Do[] start = {r::onStart};
        CharBuffer buffer = new CharBuffer();
        onRead = i->{
            // Kinda ugly, but whatevs...
            start[0].done();
            start[0] = Do.NOTHING;

            final char[] chars = Character.toChars(i);

            for (int delimiter : delimiters) {
                if (i == delimiter) {
                    // we've hit a delimiter.
                    if (!sawDelimiter[0] || !compressEmpty) {
                        // If we are not compressing, we always send line on delimiter
                        // If we are compressing, only send line first time we saw delimiter
                        r.onLine(buffer.toSource());
                        buffer.clear();
                    }
                    sawDelimiter[0] = true;
                    return; // don't append delimiters
                }
            }
            buffer.append(chars);
            sawDelimiter[0] = false;

        };
        final Do onDone = ()->{
            // If there are any queued up chars, feed them as last line to line reader
            if (!buffer.isEmpty() || sawDelimiter[0]) {
                r.onLine(buffer.toSource());
            }
            r.onEnd();
        };

        return Out2.out2Immutable(onRead, onDone);
    }
}
