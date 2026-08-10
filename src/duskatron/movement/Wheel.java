package duskatron.movement;

import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Wheel {

    public static final int BINS = 47;
    public static final double WALL_STICK = 160;
    private static final int HISTORY_SIZE = 30;
    private static final double ZERO_VELOCITY = 1.0;

    private final AdvancedRobot root;
    private final double[] surfStats = new double[BINS];

    private final Map<String, EnemySurf> enemies = new HashMap<>();

    private Point2D.Double myLocation;

    public Wheel(AdvancedRobot root) {
        this.root = root;
    }

    /*  Per-enemy scan data: detects bullets via energy drop  */
    public void update(ScannedRobotEvent e) {
        myLocation = new Point2D.Double(root.getX(), root.getY());

        EnemySurf es = enemies.computeIfAbsent(e.getName(), k -> new EnemySurf());

        double absBearing = e.getBearingRadians() + root.getHeadingRadians();
        double enemyViewBearing = absBearing + Math.PI;
        double lateralVelocity = root.getVelocity() * Math.sin(e.getBearingRadians());

        int direction = (Math.abs(root.getVelocity()) < ZERO_VELOCITY)
                ? es.direction
                : (lateralVelocity >= 0 ? 1 : -1);

        if (es.lastScanTime != -1) {
            double bulletPower = es.energy - e.getEnergy();
            if (bulletPower > 0.09 && bulletPower < 3.01) {
                EnemyWave ew = new EnemyWave();
                ew.fireTime = es.lastScanTime;
                ew.bulletVelocity = bulletVelocity(bulletPower);
                ew.distanceTraveled = (root.getTime() - ew.fireTime) * ew.bulletVelocity;
                ew.fireLocation = new Point2D.Double(es.lastX, es.lastY);

                SurfHistory aim = findHistoryAt(es, ew.fireTime - 1);
                if (aim != null) {
                    ew.direction = aim.direction;
                    ew.directAngle = aim.bearing;
                } else {
                    ew.direction = es.direction;
                    ew.directAngle = es.enemyViewBearing;
                }
                es.waves.add(ew);
            }
        }

        double enemyX = myLocation.x + Math.sin(absBearing) * e.getDistance();
        double enemyY = myLocation.y + Math.cos(absBearing) * e.getDistance();

        es.energy = e.getEnergy();
        es.lastScanTime = root.getTime();
        es.lastX = enemyX;
        es.lastY = enemyY;
        es.enemyViewBearing = enemyViewBearing;
        es.direction = direction;

        es.history.addFirst(new SurfHistory(root.getTime(), direction, enemyViewBearing));
        while (es.history.size() > HISTORY_SIZE) {
            es.history.removeLast();
        }
    }

    /*  Advances all waves and picks the safest orbit direction  */
    public void handleMovement() {
        myLocation = new Point2D.Double(root.getX(), root.getY());

        updateWaves();
        doSurfing();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        if (myLocation == null || enemies.isEmpty()) return;

        Point2D.Double hitBulletLocation =
                new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());
        double hitVelocity = bulletVelocity(e.getBullet().getPower());

        for (EnemySurf es : enemies.values()) {
            for (Iterator<EnemyWave> it = es.waves.iterator(); it.hasNext(); ) {
                EnemyWave ew = it.next();

                if (Math.abs(ew.distanceTraveled - myLocation.distance(ew.fireLocation)) < 50
                        && Math.abs(hitVelocity - ew.bulletVelocity) < 0.001) {
                    logHit(ew, hitBulletLocation);
                    it.remove();
                    return;
                }
            }
        }
    }

    public void removeEnemy(String name) {
        enemies.remove(name);
    }

    private void updateWaves() {
        for (EnemySurf es : enemies.values()) {
            for (Iterator<EnemyWave> it = es.waves.iterator(); it.hasNext(); ) {
                EnemyWave ew = it.next();

                ew.distanceTraveled = (root.getTime() - ew.fireTime) * ew.bulletVelocity;

                if (ew.distanceTraveled > myLocation.distance(ew.fireLocation) + 50) {
                    it.remove();
                }
            }
        }
    }

    private EnemyWave getClosestSurfableWave() {
        double closestDistance = 50000;
        EnemyWave surfWave = null;

        for (EnemySurf es : enemies.values()) {
            for (EnemyWave ew : es.waves) {
                double distance = myLocation.distance(ew.fireLocation) - ew.distanceTraveled;

                if (distance > ew.bulletVelocity && distance < closestDistance) {
                    surfWave = ew;
                    closestDistance = distance;
                }
            }
        }

        return surfWave;
    }

    private int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = absoluteBearing(ew.fireLocation, targetLocation) - ew.directAngle;
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

        return (int) limit(0,
                (factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
                BINS - 1);
    }

    private void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    private Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double) myLocation.clone();
        double predictedVelocity = root.getVelocity();
        double predictedHeading = root.getHeadingRadians();
        double maxTurning, moveAngle, moveDir;

        int counter = 0;
        boolean intercepted = false;

        do {
            moveAngle = wallSmoothing(predictedPosition,
                    absoluteBearing(surfWave.fireLocation, predictedPosition)
                            + (direction * (Math.PI / 2)),
                    direction) - predictedHeading;
            moveDir = 1;

            if (Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                    + limit(-maxTurning, moveAngle, maxTurning));

            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8);

            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);

            counter++;

            if (predictedPosition.distance(surfWave.fireLocation)
                    < surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                    + surfWave.bulletVelocity) {
                intercepted = true;
            }
        } while (!intercepted && counter < 500);

        return predictedPosition;
    }

    private double checkDanger(EnemyWave surfWave, int direction) {
        Point2D.Double predicted = predictPosition(surfWave, direction);

        double danger = 0;
        for (EnemySurf es : enemies.values()) {
            for (EnemyWave ew : es.waves) {
                double distanceToFront = predicted.distance(ew.fireLocation) - ew.distanceTraveled;
                if (distanceToFront <= 0) continue;

                int index = getFactorIndex(ew, predicted);
                double weight = 1.0 / (1.0 + (distanceToFront / ew.bulletVelocity));
                danger += weight * surfStats[index];
            }
        }
        return danger;
    }

    private void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();
        double goAngle;

        if (surfWave == null) {
            EnemySurf es = getNearestEnemy();
            if (es == null) return;

            int orbit = (root.getTime() / 20) % 2 == 0 ? 1 : -1;
            goAngle = wallSmoothing(myLocation,
                    absoluteBearing(new Point2D.Double(es.lastX, es.lastY), myLocation)
                            + (orbit * (Math.PI / 2)),
                    orbit);
        } else {
            double dangerLeft = checkDanger(surfWave, -1);
            double dangerRight = checkDanger(surfWave, 1);

            goAngle = absoluteBearing(surfWave.fireLocation, myLocation);
            if (dangerLeft < dangerRight) {
                goAngle = wallSmoothing(myLocation, goAngle - (Math.PI / 2), -1);
            } else {
                goAngle = wallSmoothing(myLocation, goAngle + (Math.PI / 2), 1);
            }
        }

        setBackAsFront(goAngle);
    }

    private EnemySurf getNearestEnemy() {
        EnemySurf nearest = null;
        double closest = Double.MAX_VALUE;

        for (EnemySurf es : enemies.values()) {
            if (es.lastScanTime == -1) continue;

            double distance = myLocation.distance(es.lastX, es.lastY);
            if (distance < closest) {
                closest = distance;
                nearest = es;
            }
        }

        return nearest;
    }

    private SurfHistory findHistoryAt(EnemySurf es, long time) {
        for (SurfHistory h : es.history) {
            if (h.time == time) return h;
        }
        return null;
    }

    private void setBackAsFront(double goAngle) {
        double angle = Utils.normalRelativeAngle(goAngle - root.getHeadingRadians());

        if (Math.abs(angle) > (Math.PI / 2)) {
            if (angle < 0) {
                root.setTurnRightRadians(Math.PI + angle);
            } else {
                root.setTurnLeftRadians(Math.PI - angle);
            }
            root.setBack(100);
        } else {
            if (angle < 0) {
                root.setTurnLeftRadians(-1 * angle);
            } else {
                root.setTurnRightRadians(angle);
            }
            root.setAhead(100);
        }
    }

    private double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect().contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation * 0.05;
        }
        return angle;
    }

    private Rectangle2D.Double fieldRect() {
        return new Rectangle2D.Double(18, 18,
                root.getBattleFieldWidth() - 36,
                root.getBattleFieldHeight() - 36);
    }

    private static Point2D.Double project(Point2D.Double sourceLocation,
                                          double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
    }

    private static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    private static double limit(double min, double value, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private static double bulletVelocity(double power) {
        return 20.0 - (3.0 * power);
    }

    private static double maxEscapeAngle(double velocity) {
        return Math.asin(8.0 / velocity);
    }

    public void onPaint(Graphics2D g) {
        g.setColor(Color.CYAN);

        for (EnemySurf es : enemies.values()) {
            for (EnemyWave ew : es.waves) {
                int radius = (int) ew.distanceTraveled;
                int diameter = radius * 2;

                g.drawOval((int) (ew.fireLocation.x - radius),
                        (int) (ew.fireLocation.y - radius),
                        diameter, diameter);
            }
        }
    }

    private static class EnemySurf {
        double energy = 100.0;
        long lastScanTime = -1;
        double lastX, lastY;
        double enemyViewBearing;
        int direction = 1;
        final Deque<SurfHistory> history = new ArrayDeque<>();
        final List<EnemyWave> waves = new ArrayList<>();
    }

    private static class SurfHistory {
        final long time;
        final int direction;
        final double bearing;

        SurfHistory(long time, int direction, double bearing) {
            this.time = time;
            this.direction = direction;
            this.bearing = bearing;
        }
    }
}
