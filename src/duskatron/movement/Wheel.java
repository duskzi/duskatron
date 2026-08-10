package duskatron.movement;

import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import duskatron.radar.Radar;
import robocode.AdvancedRobot;

import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class Wheel {

    public static final double ENEMY_STRENGTH  = 90;
    public static final double WALLS_STRENGTH  = 40;
    public static double MARGIN = 90.0;

    private final AdvancedRobot root;
    private final Radar radar;

    public Wheel(AdvancedRobot root, Radar radar) {
        this.root  = root;
        this.radar = radar;
    }

    public void handleMovement() {

        Map<String, Enemy> scannedBots = radar.getScannedBots();

        Vec2D enemiesForce  = getEnemyForce(scannedBots);
        Vec2D wallsForce    = getWallForce();
        Vec2D finalForce    = enemiesForce.add(wallsForce);

        double targetAngle  = Math.atan2(finalForce.x, finalForce.y);
        double angleToTurn  = normalizeAngle(targetAngle - root.getHeadingRadians());

        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;
            root.setTurnRightRadians(normalizeAngle(angleToTurn));
            root.setAhead(-100);
        } else {
            root.setTurnRightRadians(angleToTurn);
            root.setAhead(100);
        }
    }

    public Vec2D getEnemyForce(Map<String, Enemy> targets) {
        Vec2D forceVec = new Vec2D(0.0, 0.0);

        targets.forEach((_, enemy) -> {
            double dx =         root.getX() - enemy.getX();
            double dy =         root.getY() - enemy.getY();
            double distance =   Math.max(1, Math.sqrt((dx * dx) + (dy * dy)));

            double force =      ENEMY_STRENGTH / (distance * distance);

            forceVec.x +=       force * (dx / distance);
            forceVec.y +=       force * (dy / distance);
        });

        return forceVec;
    }
    public Vec2D getWallForce() {

        Vec2D force = new Vec2D(0, 0);

        double x = root.getX();
        double y = root.getY();

        double width = root.getBattleFieldWidth();
        double height = root.getBattleFieldHeight();

        double left   = x;
        double right  = width - x;
        double bottom = y;
        double top    = height - y;

        /*
         * Wall force becomes increasingly strong as we
         * approach the wall.
         */
        if (left < MARGIN) {

            double strength =
                    WALLS_STRENGTH /
                            Math.max(1, left * left);

            force.x += strength;
        }

        if (right < MARGIN) {

            double strength =
                    WALLS_STRENGTH /
                            Math.max(1, right * right);

            force.x -= strength;
        }

        if (bottom < MARGIN) {

            double strength =
                    WALLS_STRENGTH /
                            Math.max(1, bottom * bottom);

            force.y += strength;
        }

        if (top < MARGIN) {

            double strength =
                    WALLS_STRENGTH /
                            Math.max(1, top * top);

            force.y -= strength;
        }

        return force;
    }
}