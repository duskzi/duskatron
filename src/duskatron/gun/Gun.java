package duskatron.gun;

import duskatron.Duskatron;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import duskatron.radar.Radar;
import robocode.util.Utils;

import java.util.Map;

public class Gun {

    private final Duskatron root;
    private final Radar     radar;

    public static final double  FIREPOWER =     1;

    public Gun(Duskatron root) {
        this.root = root;
        this.radar = root.radar;
    }
    public void aimAndFire() {
        Enemy target = getBestTarget(radar.getScannedBots());

        if (target == null)
            return;

        double absoluteBearing =
                root.getHeadingRadians()
                        + Math.toRadians(target.getBearing());

        double gunTurn =
                Utils.normalRelativeAngle(
                        absoluteBearing - root.getGunHeadingRadians()
                );

        root.setTurnGunRightRadians(gunTurn);

        if (Math.abs(gunTurn) < Math.toRadians(3)
                && root.getGunHeat() == 0
                && root.getEnergy() > FIREPOWER + 0.1) {

            root.setFire(FIREPOWER);
        }
    }

    public Enemy getBestTarget(Map<String, Enemy> targets) {
        Enemy bestTarget = null;

        double closestDistance = Double.MAX_VALUE;

        for (Enemy e : targets.values()) {
            double toBotDistance = getDistance(e.getX(), e.getY(), root.getX(), root.getY());

            if (toBotDistance < closestDistance) {
                closestDistance = toBotDistance;
                bestTarget = e;
            }
        }

        return bestTarget;
    }


    public static double getDistance(double x1, double y1, double x2, double y2) {

        return Math.sqrt((x2 - x1) *  (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

}
