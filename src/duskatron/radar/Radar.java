package duskatron.radar;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Radar {

    private static final double RADAR_OVERSHOOT = 0.05;
    private static final long LOST_CONTACT_TIME = 20;

    private final Map<String, Enemy> targets = new HashMap<>();
    private final Map<String, Long> lastScanTimes = new HashMap<>();

    private final DuskatronContext bot;

    public Radar(DuskatronContext ctx) {
        this.bot = ctx;
    }

    public void init() {
        startSweep();
    }

    public Enemy getClosestEnemy() {
        Enemy closest = null;
        double closestDistSq = Double.MAX_VALUE;

        double x = bot.robot().getX();
        double y = bot.robot().getY();

        for (Enemy enemy : targets.values()) {
            double dx = enemy.getX() - x;
            double dy = enemy.getY() - y;
            double distSq = dx * dx + dy * dy;

            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closest = enemy;
            }
        }

        return closest;
    }

    public void trackScannedBots(ScannedRobotEvent e) {
        updateRadarColor();
        long time = bot.robot().getTime();

        Enemy enemy = targets.computeIfAbsent(e.getName(), k -> new Enemy());
        enemy.update(e, bot.robot());
        lastScanTimes.put(e.getName(), time);

        // If it's a 1v1, instantly lock onto this target
        if (bot.robot().getOthers() == 1) {
            lockOnTarget(enemy);
        }
    }

    public void update() {
        // If we haven't found everyone yet, keep spinning!
        if (targets.size() < bot.robot().getOthers()) {
            startSweep();
            return;
        }

        String oldestTargetName = getOldestTarget();
        if (oldestTargetName == null) {
            startSweep();
            return;
        }

        Enemy oldestTarget = targets.get(oldestTargetName);
        long lastScan = lastScanTimes.getOrDefault(oldestTargetName, Long.MIN_VALUE);

        // If we haven't seen the oldest target in a while, or it's just time to sweep to them
        if (bot.robot().getTime() - lastScan > LOST_CONTACT_TIME || bot.robot().getOthers() > 1) {
            lockOnTarget(oldestTarget);
        }
    }

    private void lockOnTarget(Enemy target) {
        if (target == null) {
            startSweep();
            return;
        }

        double dx = target.getX() - bot.robot().getX();
        double dy = target.getY() - bot.robot().getY();
        double absoluteBearing = Math.atan2(dx, dy);

        double radarTurn = Utils.normalRelativeAngle(absoluteBearing - bot.robot().getRadarHeadingRadians());

        if (Math.abs(radarTurn) < 1e-6) {
            radarTurn = Math.copySign(RADAR_OVERSHOOT, getRadarDirection());
        } else {
            radarTurn += Math.copySign(RADAR_OVERSHOOT, radarTurn);
        }

        bot.robot().setTurnRadarRightRadians(radarTurn);
    }

    private String getOldestTarget() {
        String oldest = null;
        long oldestTime = Long.MAX_VALUE;

        for (String name : targets.keySet()) {
            long lastScan = lastScanTimes.getOrDefault(name, Long.MIN_VALUE);
            if (lastScan < oldestTime) {
                oldestTime = lastScan;
                oldest = name;
            }
        }
        return oldest;
    }

    private void startSweep() {
        bot.robot().setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    private double getRadarDirection() {
        double radarVelocity = bot.robot().getRadarTurnRemainingRadians();
        if (radarVelocity > 0) return 1.0;
        if (radarVelocity < 0) return -1.0;
        return 1.0;
    }

    private void updateRadarColor() {
        float hue = (float) (Math.sin(bot.robot().getTime() * 0.05) * 0.15 + 0.95);
        Color color = Color.getHSBColor(hue, 1.0f, 1.0f);
        bot.robot().setRadarColor(color);
    }

    public void removeEnemy(String name) {
        targets.remove(name);
        lastScanTimes.remove(name);

        if (targets.isEmpty()) {
            startSweep();
        }
    }

    public Map<String, Enemy> getScannedBots() {
        return new HashMap<>(targets);
    }

    public void onPaint(Graphics2D g) {
        g.setColor(Color.MAGENTA);

        for (Enemy enemy : targets.values()) {
            int radius = 20;
            int diameter = radius * 2;
            int drawX = (int) (enemy.getX() - radius);
            int drawY = (int) (enemy.getY() - radius);

            g.drawOval(drawX, drawY, diameter, diameter);
        }
    }
}