package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.math.Vec2D;
import duskatron.waves.WaveBullet;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaveManager {

    public static final int NUM_BINS = 31;
    public static final double FIREPOWER = 1.0;

    private final DuskatronContext bot;
    private final List<WaveBullet> waves = new ArrayList<>();
    private final int[] stats = new int[NUM_BINS];
    private int direction = 1;

    public WaveManager(DuskatronContext ctx) {
        this.bot = ctx;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double absoluteBearing =
                bot.robot().getHeadingRadians() + e.getBearingRadians();

        Vec2D enemyPosition = new Vec2D(
                bot.robot().getX() + Math.sin(absoluteBearing) * e.getDistance(),
                bot.robot().getY() + Math.cos(absoluteBearing) * e.getDistance()
        );

        processWaveHits(enemyPosition);

        updateDirection(e, absoluteBearing);

        WaveBullet newWave = new WaveBullet(
                new Vec2D(bot.robot().getX(), bot.robot().getY()),
                absoluteBearing,
                FIREPOWER,
                direction,
                bot.robot().getTime(),
                stats
        );

        double guessFactor = getGuessFactor();

        double angleOffset =
                direction * guessFactor * newWave.maxEscapeAngle();

        double gunAdjust = Utils.normalRelativeAngle(
                absoluteBearing
                        - bot.robot().getGunHeadingRadians()
                        + angleOffset
        );
        bot.robot().setTurnGunRightRadians(gunAdjust);

        if (bot.robot().setFireBullet(FIREPOWER) != null) {
            waves.add(newWave);
        }
    }

    private void processWaveHits(Vec2D enemyPosition) {
        for (int i = 0; i < waves.size(); i++) {
            WaveBullet currentWave = waves.get(i);
            if (currentWave.checkHit(enemyPosition, bot.robot().getTime())) {
                waves.remove(currentWave);
                i--;
            }
        }
    }

    private void updateDirection(ScannedRobotEvent e, double absoluteBearing) {
        // don't try to figure out the direction they're moving,
        // they're not moving, just use the direction we had before
        if (e.getVelocity() != 0) {
            if (Math.sin(e.getHeadingRadians() - absoluteBearing) * e.getVelocity() < 0) {
                direction = -1;
            } else {
                direction = 1;
            }
        }
    }

    private double getGuessFactor() {
        int bestIndex = NUM_BINS / 2;

        for (int i = 0; i < NUM_BINS; i++) {
            if (stats[bestIndex] < stats[i]) {
                bestIndex = i;
            }
        }

        // this should do the opposite of the math in the WaveBullet:
        return (double) (bestIndex - (stats.length - 1) / 2)
                / ((double) (stats.length - 1) / 2);
    }

    public void onPaint(Graphics2D g) {
        g.setColor(Color.YELLOW);

        for (WaveBullet wave : waves) {
            // 1. Calculate how many ticks have passed
            long timeElapsed = bot.robot().getTime() - wave.getFireTime();

            // 2. Calculate the actual radius based on Robocode bullet physics
            // Bullet velocity = 20 - (3 * bulletPower)
            double bulletVelocity = 20.0 - (3.0 * FIREPOWER);
            int radius = (int) (timeElapsed * bulletVelocity);
            int diameter = radius * 2;

            // 3. Offset the coordinates by the radius so the circle draws from the center
            int drawX = (int) wave.getStart().x - radius;
            int drawY = (int) wave.getStart().y - radius;

            // 4. Draw the actual wave
            g.drawOval(drawX, drawY, diameter, diameter);
        }
    }
}
