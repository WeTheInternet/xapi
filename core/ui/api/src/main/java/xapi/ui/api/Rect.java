package xapi.ui.api;

import static xapi.ui.api.Coord.coord;

import javax.validation.constraints.NotNull;
import java.util.function.IntSupplier;

/**
 * A class meant to contain an x,y position, and a w,h size.
 *
 * Uses ui-standard top,left for x,y,
 * with always positive w,h for width and height.
 *
 * Any negative size supplied will be normalized to a positive size.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public final class Rect {

    private final Coord pos;
    private final Coord size;
    private IntSupplier hash;

    public Rect(@NotNull Coord position, @NotNull Coord size) {
        assert position != null;
        assert size != null;
        if (size.bothPositive()) {
            this.pos = position;
            this.size = size;
        } else {
            final double x, y, w, h;
            if (size.getX() < 0) {
                x = position.getX() + size.getX();
                w = -size.getX();
            } else {
                x = position.getX();
                w = size.getX();
            }
            if (size.getY() < 0) {
                y = position.getY() + size.getY();
                h = -size.getY();
            } else {
                y = position.getY();
                h = size.getY();
            }
            this.pos = coord(x, y);
            this.size = coord(w, h);
        }
        hash = () -> {
            int base = 7;
            base = base*31 + pos.hashCode();
            base = base*31 + this.size.hashCode();
            int value = base;
            hash = ()->value;
            return value;
        };
    }

    public Rect(Coord position, double width, double height) {
        this(position, coord(width, height));
    }

    public Rect(double x, double y, double width, double height) {
        this(coord(x, y), coord(width, height));
    }

    public Rect(double x, double y, Coord size) {
        this(coord(x, y), size);
    }

    public static Rect normalized(Coord point1, Coord point2){
        final double x, y, w, h;
        if (point1.getX() > point2.getX()) {
            x = point2.getX();
            w = point1.getX() - x;
        } else {
            x = point1.getX();
            w = point2.getX() - x;
        }
        if (point1.getY() > point2.getY()) {
            y = point2.getY();
            h = point1.getY() - y;
        } else {
            y = point1.getY();
            h = point2.getY() - y;
        }

        return new Rect(x, y, w, h);
    }

    double getLeft() {
        return pos.getX();
    }

    double getTop() {
        return pos.getY();
    }

    double getRight() {
        return pos.getX() + size.getX();
    }

    double getBottom() {
        return pos.getY() + size.getY();
    }

    double getWidth() {
        return size.getX();
    }

    double getHeight() {
        return size.getY();
    }

    public Rect addPosition(double x, double y) {
        if (x == 0 && y == 0) {
            return this;
        }
        return new Rect(coord(getLeft() + x, getTop() + y), size);
    }

    public Rect subtractPosition(double x, double y) {
        if (x == 0 && y == 0) {
            return this;
        }
        return new Rect(coord(getLeft() - x, getTop() - y), size);
    }

    public Rect addSize(double x, double y) {
        if (x == 0 && y == 0) {
            return this;
        }
        return new Rect(pos, coord(getWidth() + x, getWidth() + y));
    }

    public Rect subtractSize(double x, double y) {
        if (x == 0 && y == 0) {
            return this;
        }
        return new Rect(pos, coord(getWidth() - x, getWidth() - y));
    }

    public boolean contains(double x, double y) {
        if (x < getLeft() || x > getRight()) {
            return false;
        }
        if (y < getTop() || x > getBottom()) {
            return false;
        }
        return true;
    }

    public boolean contains(Coord pos) {
        return contains(pos.getX(), pos.getY());
    }

    public boolean contains(Rect rect) {
        if (rect.getLeft() < getLeft()) {
            return false;
        }
        if (rect.getRight() > getRight()) {
            return false;
        }
        if (rect.getTop() < getTop()) {
            return false;
        }
        if (rect.getBottom() > getBottom()) {
            return false;
        }
        return true;
    }

    public boolean overlaps(Rect rect) {
        if (rect.getRight() < getLeft()) {
            return false;
        }
        if (rect.getLeft() > getRight()) {
            return false;
        }
        if (rect.getBottom() < getTop()) {
            return false;
        }
        if (rect.getTop() > getBottom()) {
            return false;
        }
        return true;
    }

    public boolean isEmpty() {
        return size.isZero();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Rect))
            return false;

        final Rect rect = (Rect) o;

        if (!pos.equals(rect.pos))
            return false;
        return size.equals(rect.size);

    }

    @Override
    public int hashCode() {
        return hash.getAsInt();
    }

    @Override
    public String toString() {
        return "{ x: " + getLeft() + ", y: " + getTop() + ", w: " + getWidth() + ", h: " + getHeight() + "}";
    }
}
