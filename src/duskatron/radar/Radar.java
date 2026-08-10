package duskatron.radar;

import duskatron.enemy.Enemy;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Radar {

    private final AdvancedRobot root;
    private final Map<String, Enemy> targets = new HashMap<>();

    public Radar(AdvancedRobot duskatron) {
        this.root = duskatron;
    }

    public void init() {
        // Initial acquisition.
        root.setTurnRadarRight(Double.POSITIVE_INFINITY);
    }

    public void trackScannedBots(ScannedRobotEvent e) {

        Enemy enemy = targets.computeIfAbsent(
                e.getName(),
                k -> new Enemy()
        );

        enemy.update(e, root);

        root.setTurnRadarRight(Double.POSITIVE_INFINITY);
        /*
        if (root.getOthers() == 1) {
            trackSingleEnemy(e);
        } else {
            pointAtOldestEnemy();
        }*/
    }

    private void trackSingleEnemy(ScannedRobotEvent e) {

        double radarTurn =
                root.getHeading()
                        + e.getBearing()
                        - root.getRadarHeading();

        root.setTurnRadarRight(
                1.9 * Utils.normalRelativeAngleDegrees(radarTurn)
        );
    }

    private void pointAtOldestEnemy() {

        Enemy target = getOldestEnemy();

        if (target == null) {
            root.setTurnRadarRight(Double.POSITIVE_INFINITY);
            return;
        }

        double dx = target.getX() - root.getX();
        double dy = target.getY() - root.getY();

        double absoluteBearing =
                Math.toDegrees(Math.atan2(dx, dy));

        double radarTurn =
                Utils.normalRelativeAngleDegrees(
                        absoluteBearing - root.getRadarHeading()
                );

        // Overshoot to guarantee the radar crosses the target.
        root.setTurnRadarRight(radarTurn * 1.5);
    }

    private Enemy getOldestEnemy() {

        Enemy oldest = null;

        for (Enemy enemy : targets.values()) {

            if (oldest == null ||
                    enemy.getLastScanTime() < oldest.getLastScanTime()) {

                oldest = enemy;
            }
        }

        return oldest;
    }

    public void removeEnemy(String name) {

        targets.remove(name);

        if (targets.size() <= 1) {
            root.setTurnRadarRight(Double.POSITIVE_INFINITY);
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

            g.drawOval(
                    drawX,
                    drawY,
                    diameter,
                    diameter
            );
        }
    }
}