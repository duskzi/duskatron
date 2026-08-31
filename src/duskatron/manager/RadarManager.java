package duskatron.manager;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class RadarManager implements ManagerConstants {

    private final Map<String, Enemy> targets = new HashMap<>();
    private final DuskatronContext bot;

    public RadarManager(DuskatronContext ctx) { this.bot = ctx; }

    /*
        Returns the closest enemy or
        null if there isn't one
    */
    public Enemy getClosestEnemy() {

        Vec2D currentPos = new Vec2D(
                bot.robot().getX(),
                bot.robot().getY());

        Enemy closest =         null;
        double closestDist =    Double.MAX_VALUE;

        for (Enemy enemy : targets.values()) {

            double distSq = currentPos.distanceSq(
                    enemy.getPosition());

            if (distSq < closestDist) {
                closestDist = distSq;
                closest = enemy;
            }
        }

        return closest;
    }

    /*  Save enemies data on scan  */
    public void trackScannedBots(ScannedRobotEvent e) {
        updateRadarColor();

        Enemy enemy = targets.computeIfAbsent(e.getName(), _ -> new Enemy());
        enemy.update(e, bot.robot());

        if (bot.robot().getOthers() == 1) {
            lockOnTarget(enemy);
        }
    }

    public void handleScanning() {
        int others = bot.robot().getOthers();

        /*  Sweeps if we don't have all enemies scanned yet  */
        if (targets.size() < others) {
            startSweep();
            return;
        }

        Enemy oldest = getOldestTarget();

        if (oldest == null) {
            startSweep();
            return;
        }

        long age = bot.robot().getTime() - oldest.getLastScanTime();

        /*  Keep refreshing targets in melee  */
        if (others > 1 || age > LOST_CONTACT_TIME) {
            lockOnTarget(oldest);
        }
    }

    /*  Return the oldest target or null  */
    private Enemy getOldestTarget() {
        Enemy oldest = null;

        for (Enemy enemy : targets.values()) {
            if (oldest == null ||
                    enemy.getLastScanTime() < oldest.getLastScanTime()) {
                oldest = enemy;
            }
        }

        return oldest;
    }


    /*  Locks on target with a slight overshoot  */
    private void lockOnTarget(Enemy target) {
        if (target == null) {
            startSweep();
            return;
        }

        double dx = target.getX() - bot.robot().getX();
        double dy = target.getY() - bot.robot().getY();

        double absoluteBearing = Math.atan2(dx, dy);

        double radarTurn = Utils.normalRelativeAngle(
                absoluteBearing - bot.robot().getRadarHeadingRadians());

        if (Math.abs(radarTurn) < 1e-6) {
            radarTurn = Math.copySign(
                    RADAR_OVERSHOOT,
                    getRadarDirection()
            );
        } else {
            radarTurn += Math.copySign(
                    RADAR_OVERSHOOT,
                    radarTurn
            );
        }

        bot.robot().setTurnRadarRightRadians(radarTurn);
    }

    /*  Scans non-stoppable  */
    private void startSweep() {
        bot.robot().setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    private double getRadarDirection() {
        double remaining = bot.robot().getRadarTurnRemainingRadians();

        if (remaining > 0) return 1.0;
        if (remaining < 0) return -1.0;

        return 1.0;
    }

    /*  Set radar color to a red-purple-orange gradient  */
    private void updateRadarColor() {
        float hue = (float) (
                Math.sin(bot.robot().getTime() * 0.05) * 0.15 + 0.95
        );

        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
        bot.robot().setRadarColor(color);
    }

    public void removeEnemy(String name) {
        targets.remove(name);

        if (targets.isEmpty()) {
            startSweep();
        }
    }

    /*  Return scanned bots  */
    public Map<String, Enemy> getScannedBots() { return new HashMap<>(targets); }

    /*  Draw circles around scanned bots  */
    public void onPaint(Graphics2D g) {
        g.setColor(Color.MAGENTA);

        for (Enemy enemy : targets.values()) {
            int radius =    20;
            int diameter =  radius * 2;
            int drawX =     (int) (enemy.getX() - radius);
            int drawY =     (int) (enemy.getY() - radius);

            g.drawOval(drawX, drawY, diameter, diameter);
        }
    }
}