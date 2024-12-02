package xapi.ui.point;

import xapi.fu.IsImmutable;

import java.util.function.IntSupplier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public final class Point implements IsImmutable {

    public static final Point ZERO = new Point(0.0, 0.0);
    public static final Point INFINITY = new Point(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    /**
     * allows for rounding errors up to 1/2^24
     * (absolute differences up to 0.00000006 are tolerated)
     */
    private static final double ROUNDING_ERROR = 1./(1<<24);

    private final double x;

    private final double y;

    public Point(double x, double y) {
        this.x  = x;
        this.y = y;
    }

    public static Point coord(double x, double y) {
        if (x == 0 && y == 0) {
            return ZERO;
        }
        return new Point(x, y);
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

    public final double distance(Point point) {
        return distance(point.getX(), point.getY());
    }

    public final Point add(double x, double y) {
        return new Point(
            getX() + x,
            getY() + y);
    }

    public final Point add(Point point) {
        return add(point.getX(), point.getY());
    }

    public final Point subtract(double x, double y) {
        return new Point(
            getX() - x,
            getY() - y);
    }

    public final Point multiply(double factor) {
        return new Point(getX() * factor, getY() * factor);
    }

    public final Point subtract(Point point) {
        return subtract(point.getX(), point.getY());
    }

    public final Point center(double x, double y) {
        return new Point(
            x + (getX() - x) / 2.0,
            y + (getY() - y) / 2.0);
    }

    public final Point center(Point point) {
        return center(point.getX(), point.getY());
    }

    public final double angle(double x, double y) {
        final double ax = getX();
        final double ay = getY();

        return Math.atan2(y-ay, x-ax);
    }

    public final double angle(Point point) {
        return angle(point.getX(), point.getY());
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Point) {
            Point other = (Point) obj;
            return getX() == other.getX() && getY() == other.getY();
        } else return false;
    }

    public boolean almostEquals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Point) {
            Point other = (Point) obj;
            return Math.abs(Math.max(
                getX() - other.getX(),
                getY() - other.getY()
            )) < ROUNDING_ERROR; // an absolute difference less than 1/2^24 on either edge
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

    ///
    /// @param range - A Coord to check
    /// @return - true if this coords wholly contains the coord to check.
    /// ```
    ///  range.getX() >= getX()
    ///  &&
    ///  range.getY() <= getY()
    /// ```
    public boolean contains(Point range) {
        return range.getX() >= getX() && range.getY() <= getY();
    }

    ///
    /// @param range - A Coord to check
    /// @return - true if these coords overlap at all.
    ///      ```! (range.getY() < getX() || range.getX() > getY());```
    ///
    public boolean intersects(Point range) {
        return ! (range.getY() < getX() || range.getX() > getY());
    }

    public double size() {
        return Math.abs(getY() - getX());
    }
}
