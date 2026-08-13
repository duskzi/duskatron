package duskatron.gun;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
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

        Enemy enemy = bot.radar().getClosestEnemy();

        if (enemy != null) {

            /*  Calculate the absolute angle to the enemy  */
            double enemyAbsAngle = bot.robot().getHeadingRadians() + enemy.getBearingRadians();

            /*  Calculate the shortest turn angle for the gun  */
            double gunTurnAngle = robocode.util.Utils.normalRelativeAngle(
                    enemyAbsAngle - bot.robot().getGunHeadingRadians());

            double distToEnemy = enemy.getDistance();

            bot.robot().setTurnGunRightRadians(gunTurnAngle);

            if (bot.robot().getGunHeat() == 0
                    && Math.abs(gunTurnAngle) < Math.toRadians(5)) {

                bot.robot().setFire(GunUtils.getBestPower(distToEnemy));
            }
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (bot.arena().is1v1()) waveManager.onScannedRobot(e);
    }

    public void onPaint(Graphics2D g) { waveManager.onPaint(g); }
}
