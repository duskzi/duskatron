package duskatron.gun;

import duskatron.manager.ManagerConstants;

public class GunUtils implements ManagerConstants {

    public static double getBestPower(double distance, double life) {

        double lifeFactor = life / MAX_LIFE;

        if (distance < 50) {
            return 3;
        } else if (distance < 250) {
            return 2.5;
        } else if (distance < 350) {
            return 2;
        } else if (distance < 400) {
            return 1.5;
        } else {
            return 1;
        }
    }

    public static double getBulletSpeed(double power){ return 20.0 - (3.0 * power); }
}
