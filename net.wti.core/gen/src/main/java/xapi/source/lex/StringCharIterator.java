package xapi.source.lex;

public class StringCharIterator implements CharIterator {
    final CharSequence content;
    final int length;
    int current;

    public StringCharIterator(final String content) {
        this.content = content;
        this.length = content.length();
    }

    @Override
    public char next() {
        return content.charAt(current++);
    }

    @Override
    public char peek() {
        return content.charAt(current);
    }

    public CharSequence peekSeq() {
        return content.subSequence(current, current + 1);
    }

    @Override
    public boolean hasNext() {
        return current < length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(content).append('\n');
        for (int i = 0; i < current; i++) {
            sb.append(' ');
        }
        sb.append('^');
        return sb.toString();
    }

    @Override
    public CharSequence consume(final int size) {
        final int was = current;
        // Defensive checks to surface stream misalignment early and clearly
        if (size < 0) {
            throw new IllegalStateException(
                    "StringCharIterator.consume called with negative size: " + size +
                            " at position " + was + " of " + length +
                            "; remaining='" + safeRemainderPreview(64) + "'"
            );
        }
        final int next = was + size;
        if (next > length) {
            throw new IllegalStateException(
                    "StringCharIterator.consume overflow: requested " + size +
                            " chars at position " + was + " exceeds length " + length +
                            "; remaining=" + (length - was) +
                            "; remainder='" + safeRemainderPreview(64) + "'"
            );
        }
        current = next;
        return content.subSequence(was, current);
    }

    private String safeRemainderPreview(int max) {
        final int start = Math.max(0, current);
        final int end = Math.min(length, start + Math.max(0, max));
        try {
            return content.subSequence(start, end).toString();
        } catch (Throwable t) {
            return "<unavailable>";
        }
    }

    @Override
    public CharSequence consumeAll() {
        return consume(content.length() - current);
    }

}
