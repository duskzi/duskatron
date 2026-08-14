package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import robocode.util.Utils;

public class LinearGun extends Gun implements Shooter {

    public LinearGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns an ABSOLUTE angle in radians aiming to
        enemy next linear position, based on dynamic bullet power.
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();

        // Calculate the exact speed this bullet will travel
        double bulletSpeed = 20.0 - (3.0 * bulletPower);

        /* Absolute bearing + linear lead offset using dynamic speed */
        double absoluteAim = absoluteBearing +
                (e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing) / bulletSpeed);

        return Utils.normalAbsoluteAngle(absoluteAim);
    }

    @Override
    public String getName() {
        return "Linear Gun";
    }
}