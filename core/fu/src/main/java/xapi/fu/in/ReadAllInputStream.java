package xapi.fu.in;

import xapi.fu.itr.GrowableIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.Semaphore;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/16/16.
 */
public class ReadAllInputStream extends InputStream {

    public static ReadAllInputStream read(InputStream in) {
        if (in instanceof ReadAllInputStream) {
            return (ReadAllInputStream) in;
        }
        return new ReadAllInputStream(in);
    }

    public static ReadAllInputStream read(InputStream in, int limit) {
        if (in instanceof ReadAllInputStream) {
            return (ReadAllInputStream) in;
        }
        return new ReadAllInputStream(in);
    }

    public static byte[] readAll(InputStream in) {
        return readN(in, Integer.MAX_VALUE);
    }

    public static byte[] readN(InputStream in, int limit) {
        try {
            if (limit == -1) {
                limit = Integer.MAX_VALUE;
            }
            int total = 0;
            int chunk;
            GrowableIterator<byte[]> pages = new GrowableIterator<>();
            while ((chunk = in.read()) != -1) {
                int size = in.available();
                byte[] page;
                if (total + size + 1 > limit) {
                    size = limit - total - 1;
                    if (size <= 0) {
                        break;
                    }
                }
                page = new byte[size+1];
                page[0] = (byte) chunk;
                in.read(page, 1, size);
                pages.concat(page);
                total += size+1;
                limit -= size+1;
            }
            byte[] all = new byte[total];
            total = 0;
            for (byte[] bytes : pages.forAll()) {
                System.arraycopy(bytes, 0, all, total, bytes.length);
                total += bytes.length;
            }
            return all;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private final byte[] data;
    private volatile int pos;
    private final Semaphore lock;

    public ReadAllInputStream(byte[] data) {
        this.data = data;
        lock = new Semaphore(1);
    }

    public ReadAllInputStream(InputStream in) {
        data = readAll(in);
        lock = new Semaphore(1);
    }

    public void reset() {
        pos = 0;
    }

    public byte[] all() {
        return data;
    }
    public byte[] copy() {
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, copy.length);
        return copy;
    }

    public void move(int n) {
        pos -= n;
        if (pos < 0) {
            throw new IndexOutOfBoundsException("Cannot backup beyond the beginning of the stream");
        }
        if (pos >= data.length) {
            throw new IndexOutOfBoundsException("Cannot backup beyond the end of the stream");
        }
    }

    @Override
    public int available() {
        return data.length - pos;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        if (len == 0) {
            return 0;
        }
        lock.acquireUninterruptibly();
        try {

            final int total = off + len;
            if (off < 0 || total >= b.length) {
                throw new IndexOutOfBoundsException("Cannot write index " + total + " into array of size " + b.length);
            }

            final int availableFromMe = available() - len;
            int amt = Math.min(availableFromMe, len);
            if (amt < 1) {
                return 0;
            }
            int start = pos;
            pos += amt;
            System.arraycopy(data, start, b, off, len);

            return amt;
        } finally {
            lock.release();
        }
    }

    public int pos() {
        return pos;
    }

    @Override
    public int read() {
        lock.acquireUninterruptibly();
        try {
            int now = pos;
            if (now >= data.length) {
                return -1;
            }
            pos = now+1;
            return data[now];
        } finally {
            lock.release();
        }
    }
}
