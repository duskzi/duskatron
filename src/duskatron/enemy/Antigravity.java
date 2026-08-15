package duskatron.enemy;

import static duskatron.movement.Wheel.*;
import duskatron.math.Vec2D;
import java.util.Map;

public class Antigravity {

    public static Vec2D getMovementForce(double x, double y, Map<String, Enemy> targets, long time) {

        return getEnemyForce(x, y, targets);
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
}
