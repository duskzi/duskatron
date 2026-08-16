package duskatron.movement;

import duskatron.Constants;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import robocode.HitByBulletEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/*
    SurferWheel

    Port of the MiniSurfer wave surfing bot, see:
    (robowiki.net/wiki/MiniSurfer), adapted to
    Duskatron

    I'm not using Vec2D because the original
    implementation uses Point2D, so I'm sticking
    with it

    All comments in // double slash was made by
    AI to help me to understand MiniSurfer
    architecture, so I'll keep the // for you
    guys know where I used AI

    Radar handling stays with RadarManager.java
*/
public class SurferWheel extends Wheel implements Constants {

    private final double[] surfStats =                  new double[BINS];
    private Point2D.Double myLocation;
    private Point2D.Double enemyLocation;

    private final ArrayList<EnemyWave> enemyWaves =     new ArrayList<>();
    private final ArrayList<Integer> surfDirections =   new ArrayList<>();
    private final ArrayList<Double> surfAbsBearings =   new ArrayList<>();
    public static Rectangle2D.Double fieldRect;

    public SurferWheel(DuskatronContext ctx) {
        super(ctx);

        /*  Represents battlefield, used in wall smoothing  */
        fieldRect = new java.awt.geom.Rectangle2D.Double(
                SURF_SMOOTHING_MARGIN,
                SURF_SMOOTHING_MARGIN,
                bot.arena().getWidth()  -SURF_SMOOTHING_MARGIN * 2,
                bot.arena().getHeight() -SURF_SMOOTHING_MARGIN * 2);
    }

    @Override
    public void move() {

        Enemy enemy = bot.radar().getClosestEnemy();

        /*  Nothing scanned, skip until we find someone  */
        if (enemy == null || !enemy.exists()) { return; }
        if (enemy.getLastScanTime() != bot.robot().getTime()) { return; }

        myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

        double lateralVelocity = bot.robot().getVelocity() * Math.sin(enemy.getBearingRadians());
        double absBearing = enemy.getBearingRadians() + bot.robot().getHeadingRadians();

        surfDirections.addFirst((lateralVelocity >= 0) ? 1 : -1);
        surfAbsBearings.addFirst(absBearing + Math.PI);

        if (enemy.hasShoot() && surfDirections.size() > 2) {
            EnemyWave ew =          new EnemyWave();
            ew.bulletVelocity =     GunUtils.getBulletSpeed(enemy.getBulletPower());
            ew.fireTime =           bot.robot().getTime() - 2;  /*  Subtract 2 ticks to correct the radius of the wave  */
            ew.distanceTraveled =   2 * ew.bulletVelocity;      /*  Same here  */
            ew.direction =          surfDirections.get(2);
            ew.directAngle =        surfAbsBearings.get(2);
            ew.fireLocation = (Point2D.Double) enemyLocation.clone();

            enemyWaves.add(ew);
        }

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

        double closestDistance = Double.POSITIVE_INFINITY;
        EnemyWave surfWave = null;

        for (EnemyWave ew : enemyWaves) {

            double distance = myLocation.distance(ew.fireLocation) - ew.distanceTraveled;

            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }

        return surfWave;
    }


    /*
         CREDIT: mini sized predictor from Apollon, by rozu
         See: http://robowiki.net?Apollon
    */
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

            // maxTurning is built in like this, you can't turn more than this in one tick
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

    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
                - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

        return (int) limit(0,
                (factor * ((double) (BINS - 1) / 2)) + ((double) (BINS - 1) / 2),
                BINS - 1);
    }

    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    /*  Let the surf stats learn from the waves that actually hit us  */
    public void onHitByBullet(HitByBulletEvent e) {

        /*
            If the enemyWaves collection is empty, we must
            have missed the detection of this wave somehow
        */
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());

            EnemyWave hitWave = null;

            myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

            /*  look through the EnemyWaves, and find one that could've hit us  */
            for (EnemyWave ew : enemyWaves) {
                if (Math.abs(ew.distanceTraveled -
                        myLocation.distance(ew.fireLocation)) < 50
                        && Math.abs(GunUtils.getBulletSpeed(e.getBullet().getPower())
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;

                    break;
                }
            }

            if (hitWave != null) {

                logHit(hitWave, hitBulletLocation);
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    public double checkDanger(EnemyWave surfWave, int direction) {

        int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));
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

    /*  Iterative WallSmoothing by Kawigi  */
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation * 0.05;
        }
        return angle;
    }

    /*
        CREDIT: from CassiusClay, by PEZ
        Returns point length away from sourceLocation, at angle
        See: robowiki.net?CassiusClay
    */
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
    }

    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double limit(double min, double value, double max) { return Math.clamp(value, min, max); }
    public static double maxEscapeAngle(double velocity) { return Math.asin(8.0 / velocity); }

    public static void setBackAsFront(robocode.AdvancedRobot robot, double goAngle) {

        double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
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

    /*  Debug stuff  */
    public void onPaint(Graphics2D g) {
        if (myLocation == null) {
            return;
        }

        g.setColor(java.awt.Color.red);
        for (EnemyWave w : enemyWaves) {
            Point2D.Double center = w.fireLocation;

            int radius = (int) w.distanceTraveled;

            if (radius - 40 < center.distance(myLocation) ) {
                g.drawOval((int) (center.x - radius), (int) (center.y - radius), radius * 2, radius * 2);
            }
        }
    }
}

class EnemyWave {
    Point2D.Double fireLocation;
    long fireTime;
    double bulletVelocity, directAngle, distanceTraveled;
    int direction;

    public EnemyWave() { }
}