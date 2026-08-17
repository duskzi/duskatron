package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import robocode.util.Utils;

public class LinearGun extends Gun {

    public LinearGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns an absolute angle in radians aiming to
        enemy next linear position, based on dynamic bullet power.
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();

        // Calculate the exact speed this bullet will travel
        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);

        /*  Absolute bearing + linear offset using bullet speed  */
        double absoluteAim = absoluteBearing +
                (e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing) / bulletSpeed);

        return Utils.normalAbsoluteAngle(absoluteAim);
    }

    @Override
    public String getName() {
        return "Linear Gun";
    }
}