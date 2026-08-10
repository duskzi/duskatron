package duskatron.gun;

import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import duskatron.radar.Radar;
import robocode.AdvancedRobot;
import robocode.util.Utils;

import java.util.Map;

public class Gun {

    private static final double MIN_POWER = 1.0;
    private static final double MAX_POWER = 6.0;
    private static final double FIRE_TOLERANCE = 0.02;

    private final AdvancedRobot root;
    private final Radar radar;

    public Gun(AdvancedRobot root, Radar radar) {
        this.root = root;
        this.radar = radar;
    }

    public void aimAndFire() {

        Enemy target = pickTarget();
        if (target == null) return;

        double distance = meTo(target);
        double power = Math.max(MIN_POWER, Math.min(MAX_POWER, 500 / distance));

        double bulletSpeed = 20 - 3 * power;
        double flightTime = distance / bulletSpeed;

        double predictedX = target.getX() + Math.sin(Math.toRadians(target.getHeading())) * target.getVelocity() * flightTime;
        double predictedY = target.getY() + Math.cos(Math.toRadians(target.getHeading())) * target.getVelocity() * flightTime;

        double aimAngle = Math.atan2(predictedX - root.getX(), predictedY - root.getY());
        double gunTurn = Utils.normalRelativeAngle(aimAngle - root.getGunHeadingRadians());

        root.setTurnGunRightRadians(gunTurn);

        if (root.getGunHeat() == 0 && Math.abs(gunTurn) < FIRE_TOLERANCE) {
            root.setFire(power);
        }
    }

    private double meTo(Enemy enemy) {
        return new Vec2D(root.getX(), root.getY()).distance(new Vec2D(enemy.getX(), enemy.getY()));
    }

    private Enemy pickTarget() {

        Map<String, Enemy> targets = radar.getScannedBots();

        Enemy closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Enemy enemy : targets.values()) {
            if (!enemy.exists()) continue;

            double distance = meTo(enemy);
            if (distance < closestDistance) {
                closestDistance = distance;
                closest = enemy;
            }
        }

        return closest;
    }
}
