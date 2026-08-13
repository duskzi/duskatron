package duskatron.gun;

public class GunUtils {

    public static double getBestPower(double distance) {
        if (distance < 50) {
            return 3;
        } else if (distance < 100) {
            return 2.5;
        } else if (distance < 200) {
            return 2;
        } else if (distance < 300) {
            return 1.5;
        } else {
            return 1;
        }
    }
}
