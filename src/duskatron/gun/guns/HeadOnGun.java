package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import duskatron.math.Vec2D;
import robocode.util.Utils;

public class HeadOnGun extends Gun {

    public HeadOnGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns the angle pointing directly to the enemy
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();
        return Utils.normalAbsoluteAngle(absoluteBearing);
    }

    @Override
    public String getName() {
        return "Head-on Gun";
    }
}