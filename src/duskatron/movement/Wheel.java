package duskatron.movement;

import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import duskatron.radar.Radar;
import robocode.AdvancedRobot;

import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class Wheel {

    public static final double ENEMY_STRENGTH  = 35000;
    public static final double WALLS_STRENGTH  = 42000;
    public static double MARGIN = 90.0;

    private final AdvancedRobot root;
    private final Radar radar;

    public Wheel(AdvancedRobot root, Radar radar) {
        this.root  = root;
        this.radar = radar;
    }

    public void handleMovement() {

        Map<String, Enemy> scannedBots = radar.getScannedBots();

        Vec2D enemiesForce = getEnemyForce(scannedBots);
        Vec2D wallsForce   = getWallForce();

        // Sum everything up!
        Vec2D finalForce = enemiesForce.add(wallsForce);

        double targetAngle = Math.atan2(finalForce.x, finalForce.y);
        double angleToTurn = normalizeAngle(targetAngle - root.getHeadingRadians());

        // Apply movement (optimize turn angle to take shortest path)
        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;
            root.setTurnRightRadians(normalizeAngle(angleToTurn));
            root.setAhead(-100); // Drive backwards
        } else {
            root.setTurnRightRadians(angleToTurn);
            root.setAhead(100);  // Drive forwards
        }
    }

    public Vec2D getEnemyForce(Map<String, Enemy> targets) {
        Vec2D forceVec = new Vec2D(0.0, 0.0);

        targets.forEach((_, enemy) -> {
            double dx = root.getX() - enemy.getX();
            double dy = root.getY() - enemy.getY();
            double distance = Math.max(1, Math.sqrt((dx * dx) + (dy * dy)));

            double force = ENEMY_STRENGTH / (distance * distance);

            forceVec.x += force * (dx / distance);
            forceVec.y += force * (dy / distance);
        });

        return forceVec;
    }

    public Vec2D getWallForce() {

        Vec2D forceVec = new Vec2D(0.0, 0.0);

        double x = root.getX();
        double y = root.getY();
        double width = root.getBattleFieldWidth();
        double height = root.getBattleFieldHeight();

        if (x < MARGIN)             forceVec.x += WALLS_STRENGTH / Math.max(1, x * x);
        if (x > width - MARGIN)     forceVec.x -= WALLS_STRENGTH / Math.max(1, (width - x) * (width - x));
        if (y < MARGIN)             forceVec.y += WALLS_STRENGTH / Math.max(1, y * y);
        if (y > height - MARGIN)    forceVec.y -= WALLS_STRENGTH / Math.max(1, (height - y) * (height - y));

        return forceVec;
    }
}