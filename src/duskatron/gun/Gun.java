package duskatron.gun;

import duskatron.context.DuskatronContext;
import robocode.ScannedRobotEvent;

import java.awt.*;

public class Gun {

    private final WaveManager waveManager;
    DuskatronContext bot;

    public Gun(DuskatronContext ctx) {
        this.bot = ctx;
        this.waveManager = new WaveManager(ctx);
    }

    public void handleGun() {

        if()bot.radar().getClosestEnemy()
    }

    public void onScannedRobot(ScannedRobotEvent e) {

        waveManager.onScannedRobot(e);
    }

    public void onPaint(Graphics2D g) {
        waveManager.onPaint(g);
    }
}
