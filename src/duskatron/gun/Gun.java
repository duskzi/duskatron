package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.waves.WaveManager;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;
import java.util.Map;

public class Gun {

    public static final double FIREPOWER = 2.0;

    private final DuskatronContext bot;
    private final WaveManager waveManager;

    public Gun(DuskatronContext ctx) {
        this.bot = ctx;
        this.waveManager = new WaveManager(ctx);
    }

    public void aimAndFire() {
        waveManager.trackWaves();

        Enemy target = getBestTarget(
                bot.radar().getScannedBots()
        );

        if (target == null) {
            return;
        }

        waveManager.setEnemy(target);

        double absoluteBearing = getAimAngle(target);

        double gunTurn = Utils.normalRelativeAngle(
                absoluteBearing - bot.robot().getGunHeadingRadians()
        );

        bot.robot().setTurnGunRightRadians(gunTurn);

        if (Math.abs(gunTurn) < Math.toRadians(3)
                && bot.robot().getGunHeat() == 0
                && bot.robot().getEnergy() > FIREPOWER + 0.1) {

            bot.robot().setFire(FIREPOWER);

            waveManager.createWave(
                    FIREPOWER,
                    absoluteBearing
            );
        }
    }

    private double getAimAngle(Enemy enemy) {
        double absoluteBearing =
                bot.robot().getHeadingRadians()
                        + enemy.getBearing();

        if (bot.robot().getOthers() != 1) {
            return absoluteBearing;
        }

        return getGuessFactorAngle(
                enemy,
                absoluteBearing
        );
    }

    private double getGuessFactorAngle(
            Enemy enemy,
            double absoluteBearing
    ) {
        int[] bins = waveManager.getGuessFactorBins();

        int bestBin = getBestBin(bins);

        double guessFactor =
                waveManager.binToGf(bestBin);

        double bulletSpeed =
                Rules.getBulletSpeed(FIREPOWER);

        double maxEscapeAngle =
                Math.asin(8.0 / bulletSpeed);

        double lateralDirection =
                getLateralDirection(
                        enemy,
                        absoluteBearing
                );

        double angleOffset =
                lateralDirection
                        * guessFactor
                        * maxEscapeAngle;

        return Utils.normalAbsoluteAngle(
                absoluteBearing + angleOffset
        );
    }

    private double getLateralDirection(
            Enemy enemy,
            double absoluteBearing
    ) {
        double lateralVelocity =
                enemy.getVelocity()
                        * Math.sin(
                        enemy.getHeading()
                                - absoluteBearing
                );

        if (Math.abs(lateralVelocity) < 1e-6) {
            return 1.0;
        }

        return Math.signum(lateralVelocity);
    }

    private int getBestBin(int[] bins) {
        int bestBin = WaveManager.NUM_BINS / 2;
        int bestValue = -1;

        for (int i = 0; i < bins.length; i++) {
            if (bins[i] > bestValue) {
                bestValue = bins[i];
                bestBin = i;
            }
        }

        return bestBin;
    }

    public Enemy getBestTarget(Map<String, Enemy> targets) {
        Enemy bestTarget = null;
        double closestDistance = Double.MAX_VALUE;

        for (Enemy enemy : targets.values()) {
            double distance = getDistance(
                    enemy.getX(),
                    enemy.getY(),
                    bot.robot().getX(),
                    bot.robot().getY()
            );

            if (distance < closestDistance) {
                closestDistance = distance;
                bestTarget = enemy;
            }
        }

        return bestTarget;
    }

    public static double getDistance(
            double x1,
            double y1,
            double x2,
            double y2
    ) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        return Math.hypot(dx, dy);
    }

    public void onPaint(Graphics2D g) {
        waveManager.onPaint(g);
    }
}