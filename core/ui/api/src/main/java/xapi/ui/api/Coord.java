package xapi.ui.api;

import xapi.fu.IsImmutable;

import java.util.function.IntSupplier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public final class Coord implements IsImmutable {

    public static final Coord ZERO = new Coord(0.0, 0.0);

    private final double x;

    private final double y;

    public Coord(double x, double y) {
        this.x  = x;
        this.y = y;
    }

    public static Coord coord(double x, double y) {
        if (x == 0 && y == 0) {
            return ZERO;
        }
        return new Coord(x, y);
    }

    public final double getX() {
        return x;
    }


    public final double getY() {
        return y;
    }

    private IntSupplier hash = ()->{
        int bits = 7;
        bits = 31 * bits + Double.hashCode(getX());
        bits = 31 * bits + Double.hashCode(getY());
        int value = bits;
        hash = ()->value;
        return value;
    };

    public final double distance(double x1, double y1) {
        double a = getX() - x1;
        double b = getY() - y1;
        return Math.sqrt(a * a + b * b);
    }

    public final double distance(Coord point) {
        return distance(point.getX(), point.getY());
    }

    public final Coord add(double x, double y) {
        return new Coord(
            getX() + x,
            getY() + y);
    }

    public final Coord add(Coord point) {
        return add(point.getX(), point.getY());
    }

    public final Coord subtract(double x, double y) {
        return new Coord(
            getX() - x,
            getY() - y);
    }

    public final Coord multiply(double factor) {
        return new Coord(getX() * factor, getY() * factor);
    }

    public final Coord subtract(Coord point) {
        return subtract(point.getX(), point.getY());
    }

    public final Coord center(double x, double y) {
        return new Coord(
            x + (getX() - x) / 2.0,
            y + (getY() - y) / 2.0);
    }

    public final Coord center(Coord point) {
        return center(point.getX(), point.getY());
    }

    public final double angle(double x, double y) {
        final double ax = getX();
        final double ay = getY();

        return Math.atan2(y-ay, x-ax);
    }

    public final double angle(Coord point) {
        return angle(point.getX(), point.getY());
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Coord) {
            Coord other = (Coord) obj;
            return getX() == other.getX() && getY() == other.getY();
        } else return false;
    }

    public boolean bothPositive() {
        return x >= 0 && y >= 0;
    }

    public boolean bothNegative() {
        return x <= 0 && y <= 0;
    }

    @Override public final int hashCode() {
        return hash.getAsInt();
    }

    @Override public final String toString() {
        return "{x : " + getX() + ", y : " + getY() + "}";
    }

    public boolean isZero() {
        return getX() == 0 && getY() == 0;
    }
}
