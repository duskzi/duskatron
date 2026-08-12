package duskatron.enemy;

import duskatron.math.Vec2D;

public class Bullet {

    private static final double MAX_TRAVEL_DISTANCE = 2000.0;

    private final Vec2D start;
    private final double heading;   // radians, direction of travel
    private final double speed;
    private final long fireTime;

    public Bullet(Vec2D start, double heading, double power, long fireTime) {
        this.start = start;
        this.heading = heading;
        this.speed = 20 - 3 * power;
        this.fireTime = fireTime;
    }

    public Vec2D getPosition(long time) {
        double traveled = speed * (time - fireTime);

        return new Vec2D(
                start.x + Math.sin(heading) * traveled,
                start.y + Math.cos(heading) * traveled
        );
    }

    public boolean isAlive(long time) {
        return speed * (time - fireTime) <= MAX_TRAVEL_DISTANCE;
    }

    public double getHeading() {
        return heading;
    }
}
