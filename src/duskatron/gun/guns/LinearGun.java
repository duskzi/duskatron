package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import duskatron.math.Vec2D;
import robocode.util.Utils;

public class LinearGun extends Gun {

    public LinearGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns an absolute angle in radians aiming to
        enemy next linear position, based on dynamic bullet power.
    */
    @Override
    public void updateAimStatus(Enemy e, double bulletPower) {

        Vec2D enemyPos = new Vec2D(e.getPosition().x, e.getPosition().y);
        Vec2D robotPos = new Vec2D(bot.robot().getX(), bot.robot().getY());

        double enemyHeading = e.getHeadingRadians();
        double enemyVelocity = e.getVelocity();

        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);

        this.aimstatus.setOutside(false);

        for (int tick = 1; tick < 100; tick++) {

            enemyPos.x += Math.sin(enemyHeading) * enemyVelocity;
            enemyPos.y += Math.cos(enemyHeading) * enemyVelocity;

            if (bot.arena().isOutsideArena(enemyPos)) {
                this.aimstatus.setOutside(true);
                break;
            }

            double distance = enemyPos.distance(robotPos);
            double bulletDistance = tick * bulletSpeed;

            if (bulletDistance >= distance) {
                break;
            }
        }

        double dx = enemyPos.x - robotPos.x;
        double dy = enemyPos.y - robotPos.y;

        this.aimstatus.setAngle(Utils.normalAbsoluteAngle(Math.atan2(dx, dy)));
    }

    @Override
    public String getName() {
        return "Linear Gun";
    }
}