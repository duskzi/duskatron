package duskatron.movement;

import duskatron.Constants;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import robocode.HitByBulletEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/*
    SurferWheel

    Port of the MiniSurfer wave surfing bot (robowiki.net/wiki/MiniSurfer)
    adapted to the Duskatron wheel architecture.

    Unlike the original, radar handling stays with the Radar part, so this
    wheel only feeds on the scan data the radar already stores.
 */
public class SurferWheel extends Wheel implements Constants {

    public static int BINS = 47;
    private final double[] surfStats = new double[BINS];
    private Point2D.Double myLocation;      // our bot's location
    private Point2D.Double enemyLocation;   // enemy bot's location

    private final ArrayList<EnemyWave> enemyWaves = new ArrayList<>();
    private final ArrayList<Integer> surfDirections = new ArrayList<>();
    private final ArrayList<Double> surfAbsBearings = new ArrayList<>();

    // We must keep track of the enemy's energy level to detect EnergyDrop,
    // indicating a bullet is fired
    private double oppEnergy = 100.0;

    // This is a rectangle that represents the battlefield, used for a
    // simple, iterative WallSmoothing method (by Kawigi).
    public static Rectangle2D.Double fieldRect
            = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
    public static double WALL_STICK = 160;

    public SurferWheel(DuskatronContext ctx) {
        super(ctx);
    }

    public void move() {

        // keep the wall smoothing rectangle in sync with the real arena
        fieldRect = new Rectangle2D.Double(
                18, 18,
                bot.arena().getWidth() - 36,
                bot.arena().getHeight() - 36
        );

        Enemy enemy = bot.radar().getClosestEnemy();

        /*  Nothing scanned, skip until we find someone  */
        if (enemy == null || !enemy.exists()) {
            return;
        }

        /*
         * MiniSurfer updates and surfs on every fresh scan only, so we do
         * the same and ignore ticks where the radar had no new data.
         */
        if (enemy.getLastScanTime() != bot.robot().getTime()) {
            return;
        }

        myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

        double lateralVelocity = bot.robot().getVelocity() * Math.sin(enemy.getBearingRadians());
        double absBearing = enemy.getBearingRadians() + bot.robot().getHeadingRadians();

        surfDirections.add(0, (lateralVelocity >= 0) ? 1 : -1);
        surfAbsBearings.add(0, absBearing + Math.PI);

        double bulletPower = oppEnergy - enemy.getEnergy();
        if (bulletPower < 3.01 && bulletPower > 0.09
                && surfDirections.size() > 2) {
            EnemyWave ew = new EnemyWave();
            ew.fireTime = bot.robot().getTime();
            ew.bulletVelocity = bulletVelocity(bulletPower);
            ew.distanceTraveled = bulletVelocity(bulletPower);
            ew.direction = surfDirections.get(2);
            ew.directAngle = surfAbsBearings.get(2);
            ew.fireLocation = (Point2D.Double) enemyLocation.clone(); // last tick

            enemyWaves.add(ew);
        }

        oppEnergy = enemy.getEnergy();

        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        enemyLocation = project(myLocation, absBearing, enemy.getDistance());

        updateWaves();
        doSurfing();
    }

    public void updateWaves() {
        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = enemyWaves.get(x);

            ew.distanceTraveled = (bot.robot().getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                    myLocation.distance(ew.fireLocation) + 50) {
                enemyWaves.remove(x);
                x--;
            }
        }
    }

    public EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000; // I just use some very big number here
        EnemyWave surfWave = null;

        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = enemyWaves.get(x);
            double distance = myLocation.distance(ew.fireLocation)
                    - ew.distanceTraveled;

            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }

        return surfWave;
    }

    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, calculate the index into our stat array for that factor.
    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
                - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

        return (int) limit(0,
                (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
                BINS - 1);
    }

    // Given the EnemyWave that the bullet was on, and the point where we
    // were hit, update our stat array to reflect the danger in that area.
    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    /*
     * Wire this up from the bot's onHitByBullet() to let the surf stats
     * learn from the waves that actually hit us.
     */
    public void onHitByBullet(HitByBulletEvent e) {
        // If the enemyWaves collection is empty, we must have missed the
        // detection of this wave somehow.
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(
                    e.getBullet().getX(), e.getBullet().getY());
            EnemyWave hitWave = null;

            myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

            // look through the EnemyWaves, and find one that could've hit us.
            for (int x = 0; x < enemyWaves.size(); x++) {
                EnemyWave ew = enemyWaves.get(x);

                if (Math.abs(ew.distanceTraveled -
                        myLocation.distance(ew.fireLocation)) < 50
                        && Math.abs(bulletVelocity(e.getBullet().getPower())
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;
                    break;
                }
            }

            if (hitWave != null) {
                logHit(hitWave, hitBulletLocation);

                // We can remove this wave now, of course.
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    // CREDIT: mini sized predictor from Apollon, by rozu
    // http://robowiki.net?Apollon
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double) myLocation.clone();
        double predictedVelocity = bot.robot().getVelocity();
        double predictedHeading = bot.robot().getHeadingRadians();
        double maxTurning, moveAngle, moveDir;

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

        do {
            moveAngle =
                    wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                            predictedPosition) + (direction * (Math.PI / 2)), direction)
                            - predictedHeading;
            moveDir = 1;

            if (Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            // maxTurning is built in like this, you can't turn more then this in one tick
            maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                    + limit(-maxTurning, moveAngle, maxTurning));

            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to break down
            // otherwise you want to accelerate (look at the factor "2")
            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8);

            // calculate the new predicted position
            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);

            counter++;

            if (predictedPosition.distance(surfWave.fireLocation) <
                    surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                            + surfWave.bulletVelocity) {
                intercepted = true;
            }
        } while (!intercepted && counter < 500);

        return predictedPosition;
    }

    public double checkDanger(EnemyWave surfWave, int direction) {
        int index = getFactorIndex(surfWave,
                predictPosition(surfWave, direction));

        return surfStats[index];
    }

    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();

        if (surfWave == null) { return; }

        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);

        double goAngle = absoluteBearing(surfWave.fireLocation, myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(myLocation, goAngle - (Math.PI / 2), -1);
        } else {
            goAngle = wallSmoothing(myLocation, goAngle + (Math.PI / 2), 1);
        }

        setBackAsFront(bot.robot(), goAngle);
    }

    class EnemyWave {
        Point2D.Double fireLocation;
        long fireTime;
        double bulletVelocity, directAngle, distanceTraveled;
        int direction;

        public EnemyWave() { }
    }

    // CREDIT: Iterative WallSmoothing by Kawigi
    //   - return absolute angle to move at after account for WallSmoothing
    // robowiki.net?WallSmoothing
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation * 0.05;
        }
        return angle;
    }

    // CREDIT: from CassiusClay, by PEZ
    //   - returns point length away from sourceLocation, at angle
    // robowiki.net?CassiusClay
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
    }

    // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
    //  - returns the absolute angle (in radians) from source to target points
    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double bulletVelocity(double power) {
        return (20D - (3D * power));
    }

    public static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0 / velocity);
    }

    public static void setBackAsFront(robocode.AdvancedRobot robot, double goAngle) {
        double angle =
                Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI / 2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1 * angle);
            } else {
                robot.setTurnRightRadians(angle);
            }
            robot.setAhead(100);
        }
    }
/*
    public void onPaint(Graphics2D g) {
        if (myLocation == null) {
            return;
        }

        g.setColor(java.awt.Color.red);
        for (EnemyWave w : enemyWaves) {
            Point2D.Double center = w.fireLocation;

            int radius = (int) w.distanceTraveled;

            if (radius - 40 < center.distance(myLocation)) {
                g.drawOval((int) (center.x - radius), (int) (center.y - radius), radius * 2, radius * 2);
            }
        }
    }*/
public void onPaint(Graphics2D g) {
    if (myLocation == null) {
        return;
    }

    EnemyWave surfWave = getClosestSurfableWave();

    /*
     * Draw all active enemy waves.
     */
    for (EnemyWave w : enemyWaves) {

        Point2D.Double center = w.fireLocation;
        int radius = (int) w.distanceTraveled;

        if (radius <= 0) {
            continue;
        }

        // Main wave
        g.setColor(new Color(255, 50, 50, 100));

        g.drawOval(
                (int) (center.x - radius),
                (int) (center.y - radius),
                radius * 2,
                radius * 2
        );

        // Mark wave origin
        g.fillOval(
                (int) center.x - 4,
                (int) center.y - 4,
                8,
                8
        );
    }

    /*
     * Nothing currently surfable.
     */
    if (surfWave == null) {
        return;
    }

    /*
     * Calculate EXACTLY the same predictions used by doSurfing().
     */
    Point2D.Double leftPrediction =
            predictPosition(surfWave, -1);

    Point2D.Double rightPrediction =
            predictPosition(surfWave, 1);

    double dangerLeft =
            checkDanger(surfWave, -1);

    double dangerRight =
            checkDanger(surfWave, 1);

    /*
     * Determine which side the surfer will actually choose.
     *
     * This mirrors doSurfing():
     *
     * if dangerLeft < dangerRight -> LEFT
     * otherwise                    -> RIGHT
     */
    boolean goingLeft = dangerLeft < dangerRight;

    Point2D.Double chosenPrediction =
            goingLeft ? leftPrediction : rightPrediction;

    /*
     * ------------------------------------------------------------
     * LEFT PREDICTION
     * ------------------------------------------------------------
     */

    g.setColor(new Color(0, 150, 255, 180));

    g.drawLine(
            (int) myLocation.x,
            (int) myLocation.y,
            (int) leftPrediction.x,
            (int) leftPrediction.y
    );

    g.fillOval(
            (int) leftPrediction.x - 7,
            (int) leftPrediction.y - 7,
            14,
            14
    );

    /*
     * ------------------------------------------------------------
     * RIGHT PREDICTION
     * ------------------------------------------------------------
     */

    g.setColor(new Color(255, 180, 0, 180));

    g.drawLine(
            (int) myLocation.x,
            (int) myLocation.y,
            (int) rightPrediction.x,
            (int) rightPrediction.y
    );

    g.fillOval(
            (int) rightPrediction.x - 7,
            (int) rightPrediction.y - 7,
            14,
            14
    );

    /*
     * ------------------------------------------------------------
     * CHOSEN PREDICTION
     * ------------------------------------------------------------
     *
     * Draw this much more prominently.
     */
    g.setColor(new Color(0, 255, 80, 230));

    g.drawLine(
            (int) myLocation.x,
            (int) myLocation.y,
            (int) chosenPrediction.x,
            (int) chosenPrediction.y
    );

    g.fillOval(
            (int) chosenPrediction.x - 10,
            (int) chosenPrediction.y - 10,
            20,
            20
    );

    /*
     * Draw a small cross at the chosen destination.
     */
    int cx = (int) chosenPrediction.x;
    int cy = (int) chosenPrediction.y;

    g.drawLine(cx - 15, cy, cx + 15, cy);
    g.drawLine(cx, cy - 15, cx, cy + 15);

    g.setFont(new Font("Arial", Font.BOLD, 14));

    int textX = 10;
    int textY = 30;

    g.setColor(new Color(0, 0, 0, 170));
    g.fillRect(textX - 5, textY - 18, 230, 85);

    g.setColor(new Color(0, 150, 255));
    g.drawString(
            String.format("LEFT danger: %.2f", dangerLeft),
            textX,
            textY
    );

    g.setColor(new Color(255, 180, 0));
    g.drawString(
            String.format("RIGHT danger: %.2f", dangerRight),
            textX,
            textY + 20
    );

    g.setColor(Color.GREEN);
    g.drawString(
            "SURF: " + (goingLeft ? "LEFT" : "RIGHT"),
            textX,
            textY + 40
    );

    g.setColor(Color.WHITE);
    g.drawString(
            String.format(
                    "Wave: %.0f px away",
                    myLocation.distance(surfWave.fireLocation)
                            - surfWave.distanceTraveled
            ),
            textX,
            textY + 60
    );
}

}
