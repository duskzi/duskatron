package duskatron.enemy;

import static duskatron.movement.Wheel.*;
import duskatron.math.Vec2D;
import java.util.Map;

public class Antigravity {

    public static Vec2D getMovementForce(double x, double y, Map<String, Enemy> targets, long time) {
        Vec2D enemyForce = getEnemyForce(x, y, targets);
        Vec2D bulletForce = getBulletForce(x, y, targets, time);

        return enemyForce.add(bulletForce);
    }

    public static Vec2D getEnemyForce(double x, double y, Map<String, Enemy> targets) {
        Vec2D forceVec = new Vec2D(0.0, 0.0);

        targets.forEach((_, enemy) -> {

            double dx = x - enemy.getX();
            double dy = y - enemy.getY();

            double distance = Math.max(1.0, Math.hypot(dx, dy));

            // Direction from enemy -> us
            double awayX = dx / distance;
            double awayY = dy / distance;

            // Enemy movement vector
            double heading = Math.toRadians(enemy.getHeading());

            double enemyVx =
                    Math.sin(heading) * enemy.getVelocity();

            double enemyVy =
                    Math.cos(heading) * enemy.getVelocity();

            // How strongly the enemy is moving toward us
            double closingSpeed =
                    enemyVx * awayX +
                            enemyVy * awayY;

            closingSpeed = Math.max(0.0, closingSpeed);

            // Normal repulsion
            double force =
                    ENEMY_STRENGTH / (distance * distance);

            // Extra force against ramming enemies
            double ramMultiplier =
                    1.0 + closingSpeed / 8.0;

            force *= ramMultiplier;

            // Stronger enemies (more life/energy) repel harder.
            // Scales from ENERGY_WEIGHT_MIN (0 energy) up to 1.0 (100 energy).
            double energyWeight = ENERGY_WEIGHT_MIN
                    + (1.0 - ENERGY_WEIGHT_MIN)
                    * Math.min(1.0, enemy.getEnergy() / 100.0);

            force *= energyWeight;

            forceVec.x += force * awayX;
            forceVec.y += force * awayY;
        });

        return forceVec;
    }

    public static Vec2D getBulletForce(double x, double y, Map<String, Enemy> targets, long time) {
        Vec2D forceVec = new Vec2D(0.0, 0.0);

        for (Enemy enemy : targets.values()) {
            for (Bullet bullet : enemy.getBullets()) {
                Vec2D position = bullet.getPosition(time);

                double dx = x - position.x;
                double dy = y - position.y;
                double distance = Math.hypot(dx, dy);

                if (distance > BULLET_RANGE || distance < 1.0) {
                    continue;
                }

                // Unit vector from the bullet toward us
                double toUsX = dx / distance;
                double toUsY = dy / distance;

                // Bullet travel direction
                double tx = Math.sin(bullet.getHeading());
                double ty = Math.cos(bullet.getHeading());

                // How directly the bullet is heading toward us
                double approach = toUsX * tx + toUsY * ty;

                // Direction perpendicular to the bullet's path, pointing
                // away from it (this is what lets us step sideways to dodge)
                double perpX = toUsX - approach * tx;
                double perpY = toUsY - approach * ty;
                double perpLen = Math.hypot(perpX, perpY);

                if (perpLen < 0.1) {
                    // Bullet heading almost straight at us: dodge sideways
                    perpX = -ty;
                    perpY = tx;
                    perpLen = 1.0;
                }

                perpX /= perpLen;
                perpY /= perpLen;

                double magnitude = BULLET_STRENGTH / (distance * distance);

                forceVec.x += magnitude * (BULLET_RADIAL_WEIGHT * toUsX + BULLET_PERP_WEIGHT * perpX);
                forceVec.y += magnitude * (BULLET_RADIAL_WEIGHT * toUsY + BULLET_PERP_WEIGHT * perpY);
            }
        }

        return forceVec;
    }
}
