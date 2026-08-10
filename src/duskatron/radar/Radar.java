package duskatron.radar;

import duskatron.enemy.Enemy;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Radar {

    /*
        Using hashmap to track all enemies in arena, so:
            "WallsRobot"  ->  {x, y, ...}
    */
    private final AdvancedRobot root;
    private final Map<String, Enemy> targets = new HashMap<>();

    public Radar(AdvancedRobot duskatron) { this.root = duskatron; }

    public void init() { root.setTurnRadarRight(Double.POSITIVE_INFINITY); }

    public void trackScannedBots(ScannedRobotEvent e) {

        Enemy enemy = targets.computeIfAbsent(e.getName(), k -> new Enemy());

        enemy.update(e, this.root);

        if (root.getOthers() == 1) {
            double radarTurn = root.getHeading() + e.getBearing() - root.getRadarHeading();
            // Multiplied by 1.9 to "overshoot" slightly and keep the target locked
            root.setTurnRadarRight(1.9 * Utils.normalRelativeAngleDegrees(radarTurn));
        } else {
            // In Melee, keep spinning infinitely to scan everyone
            root.setTurnRadarRight(Double.POSITIVE_INFINITY);
        }
    }

    public void removeEnemy(String name) {
        targets.remove(name);

        // If we drop back down to 1 enemy, re-trigger infinite scan to re-aquire lock
        if (targets.size() <= 1) {
            root.setTurnRadarRight(Double.POSITIVE_INFINITY);
        }
    }

    /**
     * Returns a copy of the targets map to prevent ConcurrentModificationException.
     */
    public Map<String, Enemy> getScannedBots() {
        return new HashMap<>(targets);
    }

    /* DEBUG ONLY!!! */
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
