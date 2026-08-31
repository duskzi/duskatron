package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import duskatron.gun.VirtualBullet;
import duskatron.math.Vec2D;
import robocode.util.Utils;

import java.util.HashMap;
import java.util.Map;

public class GuessFactorGun extends Gun {

    private static final int NUM_BINS = 31;
    private static final int CENTER_BIN = (NUM_BINS - 1) / 2;

    private final Map<String, int[]> bins = new HashMap<>();

    public GuessFactorGun(DuskatronContext ctx) { super(ctx); }

    @Override
    public void updateAimStatus(Enemy e, double bulletPower) {

        String enemyName = e.getName();
        int[] enemyBins = bins.computeIfAbsent(enemyName, _ -> new int[NUM_BINS]);

        double absBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();
        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);
        double maxEscapeAngle = Math.asin(Math.min(e.getVelocity() / bulletSpeed, 1.0));

        int bestBin = getBestBin(enemyBins);
        double guessFactor = binToGuessFactor(bestBin);

        double aimAngle = Utils.normalAbsoluteAngle(absBearing + guessFactor * maxEscapeAngle);

        this.aimstatus.setOutside(false);
        this.aimstatus.setAngle(aimAngle);
    }

    @Override
    public String getName() {
        return "Guess Factor";
    }

    public void onVirtualBulletResult(VirtualBullet bullet, Enemy enemy, boolean hit) {

        if (!enemy.exists()) { return; }

        String enemyName = enemy.getName();
        int[] enemyBins = bins.computeIfAbsent(enemyName, _ -> new int[NUM_BINS]);

        double absBearing = Math.atan2(
                enemy.getX() - bot.robot().getX(),
                enemy.getY() - bot.robot().getY()
        );
        double distance = new Vec2D(bot.robot().getX(), bot.robot().getY()).distance(enemy.getPosition());
        double bulletSpeed = GunUtils.getBulletSpeed(bullet.getPower());

        double angleOffset = Utils.normalRelativeAngle(absBearing - bullet.getAngle());
        double lateralDistance = Math.sin(angleOffset) * distance;

        double travelTime = bot.robot().getTime() - bullet.getFireTime();
        double maxLateral = enemy.getVelocity() * travelTime;

        double guessFactor;
        if (maxLateral < 1e-9) {
            guessFactor = 0;
        } else {
            guessFactor = Math.clamp(lateralDistance / maxLateral, -1.0, 1.0);
        }

        int bin = guessFactorToBin(guessFactor);

        if (hit) {
            enemyBins[bin]++;
        }
    }

    private int getBestBin(int[] enemyBins) {

        int bestBin = CENTER_BIN;
        int bestValue = -1;

        for (int i = 0; i < enemyBins.length; i++) {
            if (enemyBins[i] > bestValue) {
                bestValue = enemyBins[i];
                bestBin = i;
            }
        }

        return bestBin;
    }

    private int guessFactorToBin(double guessFactor) {
        guessFactor = Math.clamp(guessFactor, -1.0, 1.0);
        return Math.clamp(
                (int) Math.round((guessFactor + 1.0) / (2.0 / (NUM_BINS - 1)))
                , 0, NUM_BINS - 1);
    }

    private double binToGuessFactor(int bin) {
        return (bin - CENTER_BIN) * (2.0 / (NUM_BINS - 1));
    }
}