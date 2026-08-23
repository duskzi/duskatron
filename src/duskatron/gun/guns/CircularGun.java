package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import duskatron.math.Vec2D;
import robocode.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class CircularGun extends Gun {

    private static class EnemyState {
        double heading;
        long time;

        EnemyState(double heading, long time) {
            this.heading = heading;
            this.time = time;
        }
    }

    private final Map<String, EnemyState> enemyStates = new HashMap<>();
    public CircularGun(DuskatronContext ctx) { super(ctx); }

    /*
        Predicts the enemy's interception point assuming it maintains
        its current velocity and approximately maintains its latest
        angular velocity.
    */
    @Override
    public void updateAimStatus(Enemy e, double bulletPower) {

        Vec2D enemyPos = new Vec2D(
                e.getPosition().x,
                e.getPosition().y);

        Vec2D robotPos = new Vec2D(
                bot.robot().getX(),
                bot.robot().getY());

        double bulletSpeed =    GunUtils.getBulletSpeed(bulletPower);
        long currentTime =      bot.robot().getTime();
        String enemyName =      e.getName();
        EnemyState previous =   enemyStates.get(enemyName);
        double turnRate =       0.0;

        if (previous != null) {

            long deltaTime = currentTime - previous.time;

            if (deltaTime > 0) {

                double headingDelta = Utils.normalRelativeAngle(
                        e.getHeadingRadians() - previous.heading
                );

                turnRate = headingDelta / deltaTime;

                double maxTurnRate = Math.toRadians(10);

                turnRate = Math.clamp(
                        turnRate,
                        -maxTurnRate,
                        maxTurnRate
                );
            }
        }

        enemyStates.put(enemyName, new EnemyState(e.getHeadingRadians(), currentTime));

        double enemyHeading = e.getHeadingRadians();
        double enemyVelocity = e.getVelocity();

        this.aimstatus.setOutside(false);

        for (int tick = 1; tick < 100; tick++) {

            /*
                Predict enemy's next position.
            */
            enemyHeading += turnRate;
            enemyPos.x += Math.sin(enemyHeading) * enemyVelocity;
            enemyPos.y += Math.cos(enemyHeading) * enemyVelocity;

            /*
                Enemy prediction has left the battlefield.
            */
            if (bot.arena().isOutsideArena(enemyPos)) {

                this.aimstatus.setOutside(true);
                break;
            }

            /*
                Can the bullet reach this position yet?
            */
            double distance = enemyPos.distance(robotPos);
            double bulletDistance = tick * bulletSpeed;

            if (bulletDistance >= distance) { break; }
        }

        double dx = enemyPos.x - robotPos.x;
        double dy = enemyPos.y - robotPos.y;

        double absoluteAim = Math.atan2(dx, dy);

        this.aimstatus.setAngle(Utils.normalAbsoluteAngle(absoluteAim));
    }

    @Override
    public String getName() {
        return "Circular Gun";
    }
}