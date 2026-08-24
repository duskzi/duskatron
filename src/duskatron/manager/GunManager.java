package duskatron.manager;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.GunUtils;
import duskatron.gun.VirtualBullet;
import duskatron.gun.guns.CircularGun;
import duskatron.gun.guns.Gun;
import duskatron.gun.guns.HeadOnGun;
import duskatron.gun.guns.LinearGun;
import duskatron.math.Vec2D;
import robocode.RobotDeathEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static duskatron.gun.GunUtils.getBestPower;

public class GunManager implements ManagerConstants {

    private final DuskatronContext bot;

    private final List<VirtualBullet> bullets;
    private final List<Gun> guns;

    /*
        Enemy name: statistics for every gun used against that enemy
    */
    private static final HashMap<String, List<GunStats>> enemyGunStats = new HashMap<>();

    public GunManager(DuskatronContext ctx) {
        this.bot = ctx;

        this.bullets =          new ArrayList<>();
        this.guns =             new ArrayList<>();

        /*
            Returns all angles in
            absolute angle in radians
        */
        guns.add(new LinearGun(ctx));
        guns.add(new CircularGun(ctx));
        guns.add(new HeadOnGun(ctx));
    }

    /*
        Updates the gun statistics and virtual bullets
    */
    public void handleGun() {

        Map<String, Enemy> targets = bot.radar().getScannedBots();

        /*  No enemy nothing to aim/shoot at, so skip  */
        Enemy e = bot.radar().getClosestEnemy();
        if (e == null) { return; }

        /*
            Make sure every scanned enemy has a GunStats
            for every gun we have
        */
        for (Enemy enemy : targets.values()) { ensureGunStats(enemy); }

        double power =  GunUtils.getBestPower(e.getDistance(), bot.robot().getEnergy());
        long time =     bot.robot().getTime();

        /*  Choose the best gun for the current enemy  */
        Gun bestGun = getBestGunAgainst(e.getName());

        if (time % VIRTUAL_AIM_DELAY == 0) {
            for (Gun gun : guns) {
                gun.updateAimStatus(e, power);
                createVirtualBullet(gun, e, power);
            }
        } else {
            bestGun.updateAimStatus(e, power);
        }

        /*  Check for virtual bullets hits or misses  */
        checkForVirtualBulletsCollisions();

        double angleInRadians = bestGun.aimstatus.getAngle();

        if(bestGun.aimstatus.isOutside()) {
            /*  Using Head-On  */
            angleInRadians = guns.getFirst().aimstatus.getAngle();
        }

        double gunTurn = Utils.normalRelativeAngle(angleInRadians - bot.robot().getGunHeadingRadians());
        bot.robot().setTurnGunRightRadians(gunTurn);

        /*  Only shoot when pointing to enemy and heat is 0  */
        if (Math.abs(gunTurn) < Math.toRadians(1.0) && bot.robot().getGunHeat() == 0) {

            bot.robot().setFire(power);
        }
    }

    /*  Creates one virtual bullet  */
    public void createVirtualBullet(Gun gun, Enemy enemy, double power) {

        double angle = gun.aimstatus.getAngle();

        bullets.add(
                new VirtualBullet(
                        enemy.getName(),
                        gun.getName(),
                        new Vec2D(
                                bot.robot().getX(),
                                bot.robot().getY()
                        ),
                        power,
                        angle,
                        bot.robot().getTime()));
    }

    /*
        Checks every virtual bullet against the current position
        of its target.

        It is considered a miss when it has traveled farther than
        MISS_DISTANCE_MARGIN
    */
    public void checkForVirtualBulletsCollisions() {

        long currentTime = bot.robot().getTime();

        for (int i = bullets.size() - 1; i >= 0; i--) {

            VirtualBullet bullet = bullets.get(i);

            Enemy enemy = bot.radar().getScannedBots().get(bullet.getTargetName());

            /*  Skip if it doesn't exist  */
            if (enemy == null) { continue; }

            double speed = GunUtils.getBulletSpeed(bullet.getPower());
            double travelTime = currentTime - bullet.getFireTime();

            double travelledDistance = travelTime * speed;

            /*  Calculate the position of the virtual bullet.  */
            double bx = bullet.getOrigin().x + Math.sin(bullet.getAngle()) * travelledDistance;
            double by = bullet.getOrigin().y + Math.cos(bullet.getAngle()) * travelledDistance;

            Vec2D enemyPosition = enemy.getPosition();
            Vec2D bulletPosition = new Vec2D(bx, by);

            /*  Distance from virtual bullet to real enemy  */
            double distance = bulletPosition.distance(enemyPosition);

            if (distance <= ROBOT_RADIUS) {

                registerHit(bullet.getTargetName(), bullet.getGunName());
                bullets.remove(i);
                continue;
            }


            double originDist = bullet.getOrigin().distance(enemyPosition);
            if (travelledDistance > originDist + MISS_DISTANCE_MARGIN) {

                registerMiss(bullet.getTargetName(), bullet.getGunName());
                bullets.remove(i);
            }
        }
    }


    /*
        Checks every virtual bullet to see if its
        target no longer exists
    */
    public void checkLostShots(RobotDeathEvent e) {

        String enemyName = e.getName();

        for (int i = bullets.size() - 1; i >= 0; i--) {

            VirtualBullet bullet = bullets.get(i);
            if (enemyName.equals(bullet.getTargetName())) { bullets.remove(i); }
        }
    }

    /*  Checks if each enemy has it own guns stats  */
    private void ensureGunStats(Enemy enemy) {

        List<GunStats> stats = enemyGunStats.computeIfAbsent(enemy.getName(), _ -> new ArrayList<>());

        for (Gun gun : guns) {

            boolean alreadyExists = stats.stream().anyMatch(s -> s.gunName.equals(gun.getName()));

            if (!alreadyExists) {

                GunStats gunStats = new GunStats(gun.getName());
                stats.add(gunStats);
            }
        }
    }

    private void registerHit(String enemyName, String gunName) {

        List<GunStats> stats = enemyGunStats.get(enemyName);

        if (stats == null) { return; }

        for (GunStats stat : stats) {

            if (stat.gunName.equals(gunName)) {
                stat.hit++;
                return;
            }
        }
    }

    private void registerMiss(String enemyName, String gunName) {

        List<GunStats> stats = enemyGunStats.get(enemyName);

        if (stats == null) { return; }

        for (GunStats stat : stats) {

            if (stat.gunName.equals(gunName)) {
                stat.misses++;
                return;
            }
        }
    }

    /*  Returns the statistics for an enemy  */
    public List<GunStats> getGunStats(String enemyName) {
        return enemyGunStats.get(enemyName);
    }

    /*  Returns the best-performing gun against an enemy  */
    public GunStats getBestGunStats(String enemyName) {

        List<GunStats> stats =
                enemyGunStats.get(enemyName);

        if (stats == null || stats.isEmpty()) {
            return null;
        }

        GunStats best = null;
        double bestAccuracy = -1.0;

        for (GunStats stat : stats) {

            int total = stat.hit + stat.misses;

            if (total == 0) { continue; }

            double accuracy = (double) stat.hit / total;

            if (accuracy > bestAccuracy) {

                bestAccuracy = accuracy;
                best = stat;
            }
        }

        return best;
    }

    public Gun getBestGunAgainst(String enemyName) {

        GunStats bestStats = getBestGunStats(enemyName);
        if (bestStats == null) { return new HeadOnGun(bot); }

        for (Gun gun : guns) {
            if (gun.getName().equals(bestStats.getGunName())) { return gun; }
        }

        return null;
    }

    public static class GunStats {

        String gunName;
        int hit, misses;

        public GunStats(String gunName) { this.gunName = gunName; }

        public String getGunName()  { return gunName; }
        public int getHit()         { return hit; }
        public int getMisses()      { return misses; }
        public int getShots()       { return hit + misses; }

        public double getAccuracy() {
            int shots = getShots();
            if (shots == 0) { return 0.0;}
            return (double) hit / shots;
        }
    }

    private Color getGunColor(String gunName) {
        int hash = gunName.hashCode();

        int r = (hash >> 16) & 0xFF;
        int g = (hash >> 8) & 0xFF;
        int b = hash & 0xFF;

        return new Color(r, g, b);
    }
    public void onPaint(Graphics2D g) {

        long currentTime = bot.robot().getTime();

        /*  Virtual bullets  */
        for (VirtualBullet bullet : bullets) {

            double speed = GunUtils.getBulletSpeed(bullet.getPower());

            double travelledDistance =
                    (currentTime - bullet.getFireTime()) * speed;

            double angle = bullet.getAngle();

            double x =
                    bullet.getOrigin().x
                            + Math.sin(angle) * travelledDistance;

            double y =
                    bullet.getOrigin().y
                            + Math.cos(angle) * travelledDistance;

            Color color = getGunColor(bullet.getGunName());

            g.setColor(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    70
            ));

            g.drawLine(
                    (int) bullet.getOrigin().x,
                    (int) bullet.getOrigin().y,
                    (int) x,
                    (int) y
            );

            g.setColor(color);

            g.fillOval(
                    (int) x - 4,
                    (int) y - 4,
                    8,
                    8
            );
        }

        /*
         * Gun statistics HUD
         */
        Enemy enemy = bot.radar().getClosestEnemy();

        if (enemy != null) {

            GunStats best = getBestGunStats(enemy.getName());

            if (best != null) {

                int x = 10;
                int y = 20;

                g.setFont(new Font("Arial", Font.BOLD, 14));

                /*
                 * Background
                 */
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(x - 5, y - 15, 180, 75);

                /*
                 * Gun color
                 */
                Color gunColor = getGunColor(best.getGunName());
                g.setColor(gunColor);

                g.drawString(
                        "Best Gun: " + best.getGunName(),
                        x,
                        y
                );

                /*
                 * Statistics
                 */
                g.setColor(Color.WHITE);

                g.drawString(
                        "Hits: " + best.getHit(),
                        x,
                        y + 18
                );

                g.drawString(
                        "Misses: " + best.getMisses(),
                        x,
                        y + 36
                );

                g.drawString(
                        String.format(
                                "Accuracy: %.1f%%",
                                best.getAccuracy() * 100.0
                        ),
                        x,
                        y + 54
                );
            }
        }
    }

}