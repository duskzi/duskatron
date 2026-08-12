package duskatron.radar;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class Radar {

    private final Map<String, Enemy> targets = new HashMap<>();

    /*  Bot parts inside bot context  */
    private final DuskatronContext bot;

    public Radar(DuskatronContext ctx) { this.bot = ctx; }

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