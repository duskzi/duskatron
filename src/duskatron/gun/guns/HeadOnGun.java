package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import robocode.util.Utils;

public class HeadOnGun extends Gun {

    public HeadOnGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns the angle pointing directly to the enemy
    */
    @Override
    public void updateAimStatus(Enemy e, double bulletPower) {

        this.aimstatus.setOutside(false);
        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();

        this.aimstatus.setAngle(Utils.normalAbsoluteAngle(absoluteBearing));
    }

    @Override
    public String getName() {
        return "Head-on Gun";
    }
}