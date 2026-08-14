package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import robocode.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class CircularGun extends Gun {

    private static class EnemyState {
        double heading;
        double velocity;
        long time;

        EnemyState(double heading, double velocity, long time) {
            this.heading = heading;
            this.velocity = velocity;
            this.time = time;
        }
    }

    private final Map<String, EnemyState> enemyStates = new HashMap<>();

    public CircularGun(DuskatronContext ctx) {
        super(ctx);
    }

    /*
        Returns an absolute angle in radians aiming to
        the enemy's predicted position using circular targeting.

        Circular targeting assumes the enemy will:
          - keep its current velocity
          - keep turning at approximately its most recent
            angular velocity
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing =
                bot.robot().getHeadingRadians() + e.getBearingRadians();

        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);

        long currentTime = bot.robot().getTime();

        String enemyName = e.getName();

        EnemyState previous = enemyStates.get(enemyName);

        /*
         * Default to straight-line movement when we don't have
         * enough history to calculate a turn rate.
         */
        double turnRate = 0.0;

        if (previous != null) {
            long deltaTime = currentTime - previous.time;

            if (deltaTime > 0) {
                double headingDelta =
                        Utils.normalRelativeAngle(
                                e.getHeadingRadians() - previous.heading
                        );

                turnRate = headingDelta / deltaTime;

                /*
                 * Limit turn rate to a reasonable Robocode value.
                 * A robot can turn up to 10 degrees/tick while moving
                 * at low velocity.
                 */
                double maxTurnRate = Math.toRadians(10);

                turnRate = Math.clamp(turnRate,
                        -maxTurnRate, maxTurnRate);
            }
        }

        /*
         * Save the current state for the next scan.
         */
        enemyStates.put(
                enemyName,
                new EnemyState(
                        e.getHeadingRadians(),
                        e.getVelocity(),
                        currentTime
                )
        );

        /*
         * Current enemy position.
         */
        double enemyX =
                bot.robot().getX()
                        + Math.sin(absoluteBearing) * e.getDistance();

        double enemyY =
                bot.robot().getY()
                        + Math.cos(absoluteBearing) * e.getDistance();

        /*
         * Current enemy heading.
         */
        double enemyHeading = e.getHeadingRadians();

        /*
         * Estimate how many ticks the bullet needs to reach
         * the enemy. Recalculate the enemy position during the
         * simulation because its heading is changing.
         */
        double predictedX = enemyX;
        double predictedY = enemyY;

        int ticks = 0;

        while (ticks < 100) {

            double distance = Math.hypot(
                    predictedX - bot.robot().getX(),
                    predictedY - bot.robot().getY()
            );

            double travelTime = distance / bulletSpeed;

            /*
             * Stop when our simulation has reached approximately
             * the bullet's arrival time.
             */
            if (ticks >= travelTime) {
                break;
            }

            /*
             * Circular movement:
             *
             * heading += turnRate
             *
             * Robocode coordinates:
             *   sin(heading) -> X
             *   cos(heading) -> Y
             */
            enemyHeading += turnRate;

            predictedX +=
                    Math.sin(enemyHeading) * e.getVelocity();

            predictedY +=
                    Math.cos(enemyHeading) * e.getVelocity();

            /*
             * Keep prediction inside the battlefield.
             */
            predictedX = Math.clamp(
                    predictedX
                    ,
                    18.0,
                    bot.robot().getBattleFieldWidth() - 18.0);

            predictedY = Math.clamp(
                    predictedY
                    ,
                    18.0,
                    bot.robot().getBattleFieldHeight() - 18.0);

            ticks++;
        }

        /*
         * Calculate the absolute bearing from our robot to the
         * predicted enemy position.
         */
        double dx = predictedX - bot.robot().getX();
        double dy = predictedY - bot.robot().getY();

        double absoluteAim = Math.atan2(dx, dy);

        return Utils.normalAbsoluteAngle(absoluteAim);
    }

    @Override
    public String getName() {
        return "Circular Gun";
    }
}
