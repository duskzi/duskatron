package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.manager.ManagerConstants;

public class GunUtils implements ManagerConstants {

    public static double getBestPower(DuskatronContext ctx, Enemy e) {
        double myEnergy = ctx.robot().getEnergy();

        if (myEnergy < 0.2) {
            return 0.0;
        }

        double maxSafePower = Math.clamp(myEnergy - 0.1, 0.1, 3.0);
        double power;
        double distance = e.getDistance();

        if (!ctx.arena().is1v1()) {
            power = distance < 200 ? 3.0 : (distance < 400 ? 2.0 : 1.0);
        } else {
            power = distance < 150 ? 3.0 : (distance < 300 ? 2.0 : (distance < 500 ? 1.5 : 0.8));
            double killPower = e.getEnergy() / 4.0;
            power = Math.clamp(killPower, 0.1, power);
        }

        return Math.clamp(power, 0.1, maxSafePower);
    }

    public static double getBulletSpeed(double power){ return 20.0 - (3.0 * power); }
}
