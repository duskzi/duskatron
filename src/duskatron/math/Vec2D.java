package duskatron.math;

public class Vec2D {
    public double x, y;

    public Vec2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2D add(Vec2D v) { return new Vec2D(this.x + v.x, this.y + v.y); }
    public Vec2D sub(Vec2D v) { return new Vec2D(this.x - v.x, this.y - v.y); }
    public double distance(Vec2D v) { return Math.hypot(this.x - v.x, this.y - v.y); }
}

