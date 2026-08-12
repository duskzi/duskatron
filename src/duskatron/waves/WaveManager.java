package duskatron.waves;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.Rules;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WaveManager {

    public static final int NUM_BINS = 31;

    private final DuskatronContext bot;

    private final int[] guessFactorBins = new int[NUM_BINS];
    private final List<Wave> waves = new ArrayList<>();

    private Enemy enemy;

    public WaveManager(DuskatronContext ctx) {
        this.bot = ctx;
    }

    public void setEnemy(Enemy enemy) {
        this.enemy = enemy;
    }

    public int gfToBin(double guessFactor) {

        double clampedGuessFactor = Math.clamp(guessFactor, -1.0, 1.0);
        double index = (clampedGuessFactor + 1.0) / 2.0 * (NUM_BINS - 1);

        return (int) Math.round(index);
    }

    public double binToGf(int binIndex) {
        int clampedIndex = Math.clamp(binIndex, 0, NUM_BINS - 1);

        return ((double) clampedIndex / (NUM_BINS - 1))
                * 2.0
                - 1.0;
    }

    public void createWave(double power, double firingAngle) {
        Wave wave = new Wave(
                new Vec2D(
                        bot.robot().getX(),
                        bot.robot().getY()
                ),
                bot.robot().getTime(),
                Rules.getBulletSpeed(power),
                firingAngle
        );

        waves.add(wave);
    }

    public void trackWaves() {
        if (enemy == null || waves.isEmpty()) {
            return;
        }

        long currentTime = bot.robot().getTime();

        Iterator<Wave> iterator = waves.iterator();

        while (iterator.hasNext()) {
            Wave wave = iterator.next();

            double waveRadius =
                    wave.bulletSpeed
                            * (currentTime - wave.fireTime);

            double distanceToEnemy =
                    wave.origin.distance(
                            enemy.getPosition()
                    );

            if (distanceToEnemy <= waveRadius) {
                recordWaveHit(wave);
                iterator.remove();
            }
        }
    }

    private void recordWaveHit(Wave wave) {
        double enemyX = enemy.getX();
        double enemyY = enemy.getY();

        double dx =
                enemyX - wave.origin.x;

        double dy =
                enemyY - wave.origin.y;

        double enemyBearing =
                Math.atan2(dx, dy);

        double bearingOffset =
                Utils.normalRelativeAngle(
                        enemyBearing - wave.fireAngle
                );

        double maxEscapeAngle =
                Math.asin(
                        8.0 / wave.bulletSpeed
                );

        double guessFactor;

        if (maxEscapeAngle == 0.0) {
            guessFactor = 0.0;
        } else {
            guessFactor =
                    bearingOffset / maxEscapeAngle;
        }

        guessFactor = Math.clamp(guessFactor, -1.0, 1.0);
        int binIndex = gfToBin(guessFactor);

        guessFactorBins[binIndex]++;
    }

    public int[] getGuessFactorBins() {
        return guessFactorBins.clone();
    }

    public List<Wave> getWaves() {
        return List.copyOf(waves);
    }


    public void onPaint(Graphics2D g) {
        g.setColor(Color.CYAN);

        for (Wave wave : waves) {
            double radius =
                    wave.bulletSpeed
                            * (bot.robot().getTime() - wave.fireTime);

            int diameter = (int) (radius * 2);

            int x = (int) (wave.origin.x - radius);
            int y = (int) (wave.origin.y - radius);

            g.drawOval(
                    x,
                    y,
                    diameter,
                    diameter
            );
        }
    }
}