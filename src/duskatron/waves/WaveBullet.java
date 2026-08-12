package duskatron.waves;

import duskatron.math.Vec2D;
import robocode.util.Utils;

public class WaveBullet {

    private final Vec2D start;
    private final double startBearing;
    private final double power;
    private final long fireTime;
    private final int direction;
    private final int[] returnSegment;

    public WaveBullet(Vec2D start, double bearing, double power,
                      int direction, long time, int[] segment) {
        this.start = start;
        this.startBearing = bearing;
        this.power = power;
        this.direction = direction;
        this.fireTime = time;
        this.returnSegment = segment;
    }

    public double getBulletSpeed()
    {
        return 20 - power * 3;
    }

    public double maxEscapeAngle()
    {
        return Math.asin(8 / getBulletSpeed());
    }

    public boolean checkHit(Vec2D enemyPosition, long currentTime)
    {
        // if the distance from the wave origin to our enemy has passed
        // the distance the bullet would have traveled...
        if (start.distance(enemyPosition) <=
                (currentTime - fireTime) * getBulletSpeed())
        {
            double desiredDirection = Math.atan2(enemyPosition.x - start.x, enemyPosition.y - start.y);
            double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
            double guessFactor = Math.clamp(angleOffset / maxEscapeAngle(), -1, 1) * direction;
            int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));
            returnSegment[index]++;
            return true;
        }
        return false;
    }

    public Vec2D getStart() {
        return start;
    }

    public long getFireTime() {
        return fireTime;
    }
}
