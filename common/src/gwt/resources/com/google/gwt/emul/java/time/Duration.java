package java.time;

/*
A super-simple Duration class based solely on our use in our model serializer.

This does NOT attempt to emulate anything properly.
*/
public class Duration {

    private final long seconds;

    public Duration(final long seconds) {
        this.seconds = seconds;
    }

    public static Duration ofSeconds(long seconds) {
        Duration d = new Duration(seconds);
        return d;
    }
    public long getSeconds() {
        return seconds;
    }
}