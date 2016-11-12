package java.util.concurrent;

/**
 * Gwt-friendly emulation of a semaphore.
 *
 * While we can't actually make code block,
 * we can implement a count of permits to guard access to resources.
 *
 * See the real implementation in java.util.concurrent for details
 * on how this class behaves in a real JVM; you get the same
 * concept of a set of permits, but no blocking apis.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/16/16.
 */

import java.util.Collection;
import java.util.Collections;

public class Semaphore implements java.io.Serializable {

    int permits;

    public Semaphore(int permits) {
        this.permits = permits;
    }

    public Semaphore(int permits, boolean fair) {
        this(permits);
    }

    public void acquire() throws InterruptedException {
        if (permits < 1) {
            throw interrupted();
        }
        permits--;
    }

    private InterruptedException interrupted() {
        return new InterruptedException("No permits available (GWT cannot block)");
    }

    public void acquireUninterruptibly() {
        try {
            acquire();
        } catch (InterruptedException ignored) {
            permits--;
        }
    }

    public boolean tryAcquire() {
        if (permits > 0) {
            permits--;
            return true;
        }
        return false;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit)
    throws InterruptedException {
        return tryAcquire();
    }

    public void release() {
        // TODO: hold permits in scope somehow so we can check...
        permits++;
    }

    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        if (this.permits < permits) {
            throw interrupted();
        }
        this.permits -= permits;
    }

    public void acquireUninterruptibly(int permits) {
        try {
          acquire(permits);
        } catch (InterruptedException ignored){
            this.permits-=permits;
        }
    }

    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        if (this.permits < permits) {
            return false;
        }
        this.permits -= permits;
        return true;
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit)
    throws InterruptedException {
        return tryAcquire(permits);
    }

    public void release(int permits) {
        this.permits += permits;
    }

    public int availablePermits() {
        return permits;
    }

    public int drainPermits() {
        int all = permits;
        permits = 0;
        return all;
    }

    protected void reducePermits(int reduction) {
        permits = Math.max(0, reduction);
    }

    public boolean isFair() {
        return false;
    }

    public final boolean hasQueuedThreads() {
        return false;
    }

    public final int getQueueLength() {
        return 0;
    }

    protected Collection<Thread> getQueuedThreads() {
        return Collections.EMPTY_LIST;
    }

    public String toString() {
        return "Semaphore[" + permits + "]";
    }
}
