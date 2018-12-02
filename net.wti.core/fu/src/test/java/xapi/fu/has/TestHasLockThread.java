package xapi.fu.has;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/10/18 @ 2:07 PM.
 */
public class TestHasLockThread extends Thread {
    public Object result;
    public Object src;
    public Object factory;
    public boolean foreign;

    public TestHasLockThread(Object src, Object factory) {
        this.src = src;
        this.factory = factory;
    }

    @Override
    public void run() {
        foreign = !(src instanceof HasLock);
        System.out.println("src instanceof HasLock? " + !foreign);
        result = HasLock.reflect(src, factory);
    }

    @Override
    public String toString() {
        return "T{" +
            "result=" + result +
            ", src=" + src +
            ", factory=" + factory +
            "} " + super.toString();
    }
}
