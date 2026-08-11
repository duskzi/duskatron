package duskatron.enemy;

import static duskatron.movement.Wheel.*;
import duskatron.math.Vec2D;
import java.util.Map;

public class Antigravity {

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

            forceVec.x += force * awayX;
            forceVec.y += force * awayY;
        });

        return forceVec;
    }
    public static Vec2D getWallForce(double x, double y, double width, double height,
                                     double moveDirection) {
        Vec2D force = new Vec2D(0, 0);
        double left   = x;
        double right  = width - x;
        double bottom = y;
        double top    = height - y;

        // Tangent direction is fixed to movement direction — never flips mid-wall
        double td = moveDirection;

        if (left < MARGIN) {
            double strength = WALLS_STRENGTH / Math.max(1, left * left);
            force.x += strength;
            force.y += strength * WALL_TANGENT_FACTOR * td;
        }
        if (right < MARGIN) {
            double strength = WALLS_STRENGTH / Math.max(1, right * right);
            force.x -= strength;
            force.y += strength * WALL_TANGENT_FACTOR * td;
        }
        if (bottom < MARGIN) {
            double strength = WALLS_STRENGTH / Math.max(1, bottom * bottom);
            force.y += strength;
            force.x += strength * WALL_TANGENT_FACTOR * td;
        }
        if (top < MARGIN) {
            double strength = WALLS_STRENGTH / Math.max(1, top * top);
            force.y -= strength;
            force.x += strength * WALL_TANGENT_FACTOR * td;
        }

        return force;
    }
}
