package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static duskatron.gun.GunUtils.getBestPower;

public class GunManager extends Cannon {

    private static final double ROBOT_RADIUS =          18.0;
    private static final double MISS_DISTANCE_MARGIN =  60.0;

    private final DuskatronContext bot;

    private final List<VirtualBullet> bullets;
    private final List<Gun> guns;

    /*
        Enemy name: statistics for every gun used against that enemy
    */
    private final HashMap<String, List<GunStats>> enemyGunStats;

    public GunManager(DuskatronContext ctx) {
        this.bot = ctx;

        this.bullets =          new ArrayList<>();
        this.guns =             new ArrayList<>();
        this.enemyGunStats =    new HashMap<>();

        guns.add(new LinearGun(ctx)); // Returns in Radians
        guns.add(new CircularGun(ctx)); // Returns in Radians
    }

    /*
        Updates the gun statistics and virtual bullets
    */
    public void handleGun() {

        Map<String, Enemy> targets = bot.radar().getScannedBots();

        if(bot.robot().getTime() % 4 == 0) {
            Enemy e = bot.radar().getClosestEnemy();
            testVirtualAim(e);
        }

        /*
            Make sure every scanned enemy has a GunStats
            for every gun we have
        */
        for (Enemy enemy : targets.values()) {
            ensureGunStats(enemy);
        }

        checkForVirtualBulletsCollisions();
    }

    /*
        Creates one virtual bullet for every gun
    */
    public void testVirtualAim(Enemy enemy) {

        if (enemy == null) {
            return;
        }

        double power = getBestPower(enemy.getDistance());

        for (Gun gun : guns) {

            double angle = gun.aimAngleFunction(enemy, power);

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
                            bot.robot().getTime()
                    )
            );
        }
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

            Enemy enemy =
                    bot.radar()
                            .getScannedBots()
                            .get(bullet.getTargetName());


            /*  Skip if it doesn't exist  */
            if (enemy == null) { continue; }

            /*  TODO: add getSpeed on GunUtils  */
            double speed = 20.0 - 3.0 * bullet.getPower();
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
        Checks if each enemy has it own guns stats
    */
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

    private void registerHit(
            String enemyName,
            String gunName
    ) {

        List<GunStats> stats =
                enemyGunStats.get(enemyName);

        if (stats == null) {
            return;
        }

        for (GunStats stat : stats) {

            if (stat.gunName.equals(gunName)) {
                stat.hit++;
                return;
            }
        }
    }

    private void registerMiss(
            String enemyName,
            String gunName
    ) {

        List<GunStats> stats =
                enemyGunStats.get(enemyName);

        if (stats == null) {
            return;
        }

        for (GunStats stat : stats) {

            if (stat.gunName.equals(gunName)) {
                stat.misses++;
                return;
            }
        }
    }

    /**
     * Returns the statistics for an enemy.
     */
    public List<GunStats> getGunStats(String enemyName) {
        return enemyGunStats.get(enemyName);
    }

    /**
     * Returns the best-performing gun against an enemy.
     */
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

            if (total == 0) {
                continue;
            }

            double accuracy =
                    (double) stat.hit / total;

            if (accuracy > bestAccuracy) {

                bestAccuracy = accuracy;
                best = stat;
            }
        }

        return best;
    }

    public class GunStats {

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

    public void onPaint(Graphics2D g) {

        for (VirtualBullet bullet : bullets) {

            long currentTime = bot.robot().getTime();

            double speed = 20.0 - 3.0 * bullet.getPower();
            double travelledDistance =
                    (currentTime - bullet.getFireTime()) * speed;

            double angle = bullet.getAngle();
            double x = bullet.getOrigin().x + Math.sin(angle) * travelledDistance;
            double y = bullet.getOrigin().y + Math.cos(angle) * travelledDistance;

            g.setColor(Color.RED);
            g.fillOval((int) x - 4, (int) y - 4, 4, 4);
            g.setColor(new Color(15, 255, 0, 70));
            g.drawLine((int) bullet.getOrigin().x, (int) bullet.getOrigin().y, (int) x, (int) y);
        }
    }
}