package duskatron;

/*
    .-------------------------.
    | Duskatron Merge Utility |
    '-------------------------'

    Script: merge.py
    Package: duskatron
    Main class: Duskatron
    Files merged: 22
    Source directory: C:\Users\20260131\IdeaProjects\duskatron\src\duskatron
*/

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import robocode.*;
import robocode.AdvancedRobot;
import robocode.HitByBulletEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import static java.lang.Math.*;


/* ---- Advertise.java ---- */

/*
    |`'. |  | {_´´ |../  /\  "|" |  ) .''. |\ |   [_ `\=-='
    |_.' |..| .__} |  \ /  \  |  |  \ '..' | \|   (.....)
            a robocode bot by Dusk.

    EQUIPE:         DUSKATRON

    INTEGRANTE 1:   Felipe Kühl Pereira
    INTEGRANTE 2:   n/a
    INTEGRANTE 3:   n/a

    ESCLARECIMENTO SOBRE O USO DE IA:

    Durante o desenvolvimento do projeto
    foi utilizada IA (inteligência
    artificial) para as seguintes
    circunstâncias:

        > Script para merge:
            O projeto foi desenvolvido em
            torno de 21 classes.
            O torneio aceita apenas uma
            classe java nomeada
            <NomeDaEquipe>.java, então
            precisei unir as classes em
            apenas um arquivo, utilizando
            um script em python, merge.py.

        > Limpeza de código, como:
            Remover expressões redundantes.
            Facilitar a procura por erros.

        > Auxílio ao portar algoritmos, como:
            Circular Targeting.
            Ajuda na limpeza do código de
            wave surfing.
            Substituir Vec2D por Point2D para
            melhor compatibilidade.

    Todas referências e algoritmos vieram de:

    https://book.robocode.dev/
        > Visão geral sobre bots e física
        > Radar, virtual aim

    https://robowiki.net/wiki/Main_Page
        > Algoritmos avançados
        > Estratégias avançadas
*/

/*
    All of my code is written in English, but I'll
    let the header in PT-BR cuz I'm not confident
    writing important info in English.

    Good classes to learn from:

        DuskatronContext.java
        Duskatron.java
        Manager constants interface
        All 3 managers
        Enemy.java
*/

/*  Just empty to get header message at the top when merging  */
interface Advertise { }


/* ---- arena\Arena.java ---- */

class Arena {

    private final double width;
    private final double height;
    private DuskatronContext bot;
    // private double margin;

    public Arena(DuskatronContext ctx, double w, double h){
        this.width = w;
        this.height = h;
        this.bot = ctx;
        // this.margin = margin;
    }

    /*  TODO: see if I really need margin here  */
    //public void setMargin(double margin) { this.margin = margin; }
    //public double getMargin() { return margin; }

    public double getWidth()    { return width; }
    public double getHeight()   { return height; }
    public boolean is1v1()      { return (bot.robot().getOthers() == 1); }
    public boolean isInsideArena(Vec2D pos) {
        return (0.0 < pos.x && pos.x < width) && (0.0 < pos.y && pos.y < height);
    }
}


/* ---- Constants.java ---- */

/*
    .-----------------------------------------------.
    |    GLOBAL CONSTANTS                           |
    |    Change them, tune them, erase them         |
    |    They're all explained here, good luck!     |
    '_______________________________________________'
*/
interface Constants {

    /*  Wall smoothing  */
    double MARGIN =                     30.0;
    double LOOK_AHEAD_DIST =            140.0;

    /*
        Hawk On Fire movement
    */
    double HOF_ARRIVAL_THRESHOLD =      15;
    double HOF_WALL_MARGIN =            30;
    int HOF_SEARCH_ATTEMPTS =           200;
    double HOF_SEARCH_MIN_DIST =        100;
    double HOF_SEARCH_RANGE =           200;

    /*
        Minimum Risk Movement (MRM)
    */
    int MRM_POINT_COUNT =               32;         /*  How many points to use when sampling  */
    double MRM_DISTANCE =               100.0;      /*  How far risk points are sampled from the bot  */
    double WALL_MARGIN =                40.0;       /*  Desired distance from arena walls  */
    /*
        MRM wall smoothing
        Only applied if you use the 'SmoothHeading' method over 'goTo'
    */
    double WALL_SMOOTH_ANGLE_STEP =     2.0;
    int WALL_SMOOTH_MAX_STEPS =         90;

    /*
        Risk priorities

            60% enemy danger
            10% being the closest target
            15% walls
            15% avoiding our own recent path

        Yeah, I know it sucks being in floating
        decimals but make sense for me
    */
    double ENEMY_RISK_WEIGHT =          0.60;
    double CLOSEST_BOT_RISK_WEIGHT =    0.10;
    double WALL_RISK_WEIGHT =           0.15;
    double TRAIL_RISK_WEIGHT =          0.15;

    /*
        Distance at which an enemy's distance danger start to grow,
        this is a physical distance, not a mysterious multiplier
    */
    double ENEMY_DISTANCE_SCALE =       140.0;
    double TRAIL_DISTANCE_SCALE =       80.0;       /*  Positions farther than this have almost no trail penalty  */
    int TRAIL_LENGTH =                  12;         /* Number of historical positions to be considered  */


    /*
        Surfer Wheel
    */
    double WALL_STICK =                 160;
    double SURF_SMOOTHING_MARGIN =      18;
    int BINS =                          47;
}


/* ---- context\DuskatronContext.java ---- */

class DuskatronContext {

    private GunManager        gun;        /*  Gun part  */
    private RadarManager      radar;      /*  Radar part  */
    private WheelManager      wheel;      /*  Wheel part  */
    private Arena             arena;      /*  Battlefield arena  */
    private AdvancedRobot     robot;      /*  Duskatron itself  */

    public DuskatronContext() {}

    /*
        Bind parts after instancing them to not fall into circular
        dependency, ex.:

        Bot need Gun in its constructor    Bot bot = new Bot(gun);
        Gun need Bot in its constructor    Gun gun = new Gun(bot);

        So after instancing Bot, we bind it passing to a method
        after initialization:

        Bot bot = new Bot(...);
        gun.bind(bot);
    */
    public void bindRobot(AdvancedRobot robot)  { this.robot = robot; }
    public void bindArena(Arena arena)          { this.arena = arena; }
    public void bindManagers(GunManager gun, WheelManager wheel, RadarManager radar) {
        this.gun = gun;
        this.wheel = wheel;
        this.radar = radar;
    }

    public AdvancedRobot robot()            { return robot; }
    public GunManager gun()                 { return gun; }
    public RadarManager radar()             { return radar; }
    public WheelManager wheel()             { return wheel; }
    public Arena arena()                    { return arena; }
}


/* ---- Duskatron.java ---- */

public class Duskatron extends AdvancedRobot {

    /*
        Bot's context holds references to all
        duskatron managers listed below
    */
    DuskatronContext ctx = new DuskatronContext();

    /*
        Robo-parts
            Radar:      Manages find and storing enemy data
            Cannon:     Choose the best enemy and how to fire it
            Wheel:      Handles movement
    */
    public RadarManager radar;
    public GunManager gun;
    public WheelManager wheel;
    /*
        Arena holds battlefield width and height
    */
    public Arena arena;

    @Override
    public void run() {

        arena = new Arena(
                ctx,
                this.getBattleFieldWidth(),
                this.getBattleFieldHeight());

        /*
            Pass all parts references to the
            context to be used later inside
            each part
        */
        ctx.bindRobot(this);
        ctx.bindArena(arena);

        radar =         new RadarManager(ctx);
        gun =           new GunManager(ctx);
        wheel =         new WheelManager(ctx);

        ctx.bindManagers(gun, wheel, radar);

        /*
            Allows radar and gun to rotate
            independently for advanced
            scanning and shooting
        */
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        /*  Bot colors  */
        setRadarColor(Color.ORANGE);
        setBodyColor(Color.BLACK);
        setGunColor(Color.DARK_GRAY);

        /*  Debug  */
        System.out.println("Summary:");
        System.out.println("    Arena: (" +
                arena.getWidth() + " " +
                arena.getHeight() + ")");

        System.out.println("    Total Bots: " + getOthers());

        /*  Main loop  */
        for (;;) {

            radar.handleScanning();
            wheel.handleMovement();
            gun.handleGun();

            execute();
        }
    }

    /*  Components method calls  */
    public void onScannedRobot(ScannedRobotEvent e)     { radar.trackScannedBots(e); }
    public void onRobotDeath(RobotDeathEvent e)         { radar.removeEnemy(e.getName()); }
    public void onHitByBullet(HitByBulletEvent e)       { wheel.onHitByBullet(e); }

    /*  Larping after win  */
    public void onWin(WinEvent event) {

        for(;;) {

            setTurnRight(Double.POSITIVE_INFINITY);
            setTurnGunLeft(Double.POSITIVE_INFINITY);

            float hue = (getTime() * 0.2f) % 1.0f;
            Color rainbowColor = Color.getHSBColor(hue, 0.67f, 0.67f);

            setBodyColor(rainbowColor);

            execute();

        }
    }

    public void onPaint(Graphics2D g) {
        radar.onPaint(g);
        if(getOthers() != 1) gun.onPaint(g);
        wheel.onPaint(g);
    }
}


/* ---- enemy\Enemy.java ---- */

class Enemy {

    private String name;
    private double x, y;
    private double bearing, distance, energy, heading, velocity, bulletPower;
    private long lastScanTime;

    public void update(ScannedRobotEvent e, Robot me) {
        this.name =         e.getName();
        this.bearing =      e.getBearing();
        this.distance =     e.getDistance();
        this.heading =      e.getHeading();
        this.velocity =     e.getVelocity();

        this.bulletPower =  this.energy - e.getEnergy();
        this.energy =       e.getEnergy();

        double absoluteBearing =    Math.toRadians(me.getHeading() + e.getBearing());
        this.x =                    me.getX() + Math.sin(absoluteBearing) * e.getDistance();
        this.y =                    me.getY() + Math.cos(absoluteBearing) * e.getDistance();

        this.lastScanTime = me.getTime();
    }

    public void reset()                     { this.name = ""; }
    public boolean exists()                 { return !this.name.isEmpty(); }

    public boolean hasShoot()               { return bulletPower < 3.01 && bulletPower > 0.09; }
    public double getBulletPower()          { return bulletPower; }

    /*  Getters  */
    public Vec2D  getPosition()             { return new Vec2D(x, y); }
    public String getName()                 { return name; }
    public double getX()                    { return x; }
    public double getY()                    { return y; }
    public double getHeading()              { return heading; }
    public double getVelocity()             { return velocity; }
    public double getDistance()             { return distance; }
    public double getEnergy()               { return energy; }
    public double getBearing()              { return bearing; }
    public double getHeadingRadians()       { return Math.toRadians(heading); }
    public double getBearingRadians()       { return Math.toRadians(bearing); }
    public long   getLastScanTime()         { return lastScanTime; }
}


/* ---- gun\guns\CircularGun.java ---- */

class CircularGun extends Gun {

    private static class EnemyState {
        double heading;
        double velocity;
        long time;

        EnemyState(double heading, double velocity, long time) {
            this.heading = heading;
            this.velocity = velocity;
            this.time = time;
        }
    }

    private final Map<String, EnemyState> enemyStates = new HashMap<>();

    public CircularGun(DuskatronContext ctx) {
        super(ctx);
    }

    /*
        Returns an absolute angle in radians aiming to
        the enemy's predicted position using circular targeting

        Circular targeting assumes the enemy will keep its current
        velocity and keep turning at approximately its most recent
        angular velocity
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();
        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);
        long currentTime = bot.robot().getTime();

        String enemyName = e.getName();
        EnemyState previous = enemyStates.get(enemyName);

        /*  Default movement when we don't have enough history to calculate a turn rate  */
        double turnRate = 0.0;

        if (previous != null) {
            long deltaTime = currentTime - previous.time;

            if (deltaTime > 0) {
                double headingDelta = Utils.normalRelativeAngle(
                                e.getHeadingRadians() - previous.heading);

                turnRate = headingDelta / deltaTime;

                /*
                    A robot can turn up to 10 degrees/tick while moving
                    at low velocity
                */
                double maxTurnRate = Math.toRadians(10);
                turnRate = Math.clamp(turnRate, -maxTurnRate, maxTurnRate);
            }
        }

        /*  Save the current state for the next scan  */
        enemyStates.put(
                enemyName,
                new EnemyState(
                        e.getHeadingRadians(),
                        e.getVelocity(),
                        currentTime
                )
        );

        /*  Current enemy position  */
        double enemyX = bot.robot().getX()
                + Math.sin(absoluteBearing) * e.getDistance();
        double enemyY = bot.robot().getY()
                + Math.cos(absoluteBearing) * e.getDistance();

        /*  Current enemy heading  */
        double enemyHeading = e.getHeadingRadians();

        double predictedX = enemyX;
        double predictedY = enemyY;

        int ticks = 0;

        while (ticks < 100) {

            double distance = Math.hypot(
                    predictedX - bot.robot().getX(),
                    predictedY - bot.robot().getY()
            );

            double travelTime = distance / bulletSpeed;


            if (ticks >= travelTime) {
                break;
            }

            enemyHeading += turnRate;

            predictedX += Math.sin(enemyHeading) * e.getVelocity();
            predictedY += Math.cos(enemyHeading) * e.getVelocity();


            predictedX = Math.clamp(
                    predictedX,
                    18.0,
                    bot.robot().getBattleFieldWidth() - 18.0);

            predictedY = Math.clamp(
                    predictedY,
                    18.0,
                    bot.robot().getBattleFieldHeight() - 18.0);

            ticks++;
        }

        double dx = predictedX - bot.robot().getX();
        double dy = predictedY - bot.robot().getY();
        double absoluteAim = Math.atan2(dx, dy);

        return Utils.normalAbsoluteAngle(absoluteAim);
    }

    public String getName() {
        return "Circular Gun";
    }
}


/* ---- gun\guns\Gun.java ---- */

abstract class Gun {

    DuskatronContext bot;

    public Gun(DuskatronContext ctx) { this.bot = ctx; }

    public abstract double aimAngleFunction(Enemy e, double bulletPower);
    public abstract String getName();

    public void onPaint(Graphics2D g) {};
}


/* ---- gun\guns\HeadOnGun.java ---- */

class HeadOnGun extends Gun {

    public HeadOnGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns the angle pointing directly to the enemy
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();
        return Utils.normalAbsoluteAngle(absoluteBearing);
    }

    @Override
    public String getName() {
        return "Head-on Gun";
    }
}


/* ---- gun\guns\LinearGun.java ---- */

class LinearGun extends Gun {

    public LinearGun(DuskatronContext ctx) { super(ctx); }

    /*
        Returns an absolute angle in radians aiming to
        enemy next linear position, based on dynamic bullet power.
    */
    @Override
    public double aimAngleFunction(Enemy e, double bulletPower) {

        double absoluteBearing = bot.robot().getHeadingRadians() + e.getBearingRadians();

        // Calculate the exact speed this bullet will travel
        double bulletSpeed = GunUtils.getBulletSpeed(bulletPower);

        /*  Absolute bearing + linear offset using bullet speed  */
        double absoluteAim = absoluteBearing +
                (e.getVelocity() * Math.sin(e.getHeadingRadians() - absoluteBearing) / bulletSpeed);

        return Utils.normalAbsoluteAngle(absoluteAim);
    }

    @Override
    public String getName() {
        return "Linear Gun";
    }
}


/* ---- gun\GunUtils.java ---- */

class GunUtils {

    public static double getBestPower(double distance) {
        if (distance < 50) {
            return 3;
        } else if (distance < 250) {
            return 2.5;
        } else if (distance < 350) {
            return 2;
        } else if (distance < 400) {
            return 1.5;
        } else {
            return 1;
        }
    }

    public static double getBulletSpeed(double power){ return 20.0 - (3.0 * power); }
}


/* ---- gun\VirtualBullet.java ---- */

class VirtualBullet {

    public VirtualBullet(String enemy, String gun, Vec2D initialPos, double power, double angle, long timeWhenFired) {
        this.enemy = enemy;
        this.gun = gun;
        this.initialPos = initialPos;
        this.power = power;
        this.angle = angle;                     /*  Radians  */
        this.timeWhenFired = timeWhenFired;
    }

    String enemy;
    String gun;

    Vec2D initialPos;

    double power;
    double angle;    /*  Radians  */
    long timeWhenFired;

    public String getTargetName()   { return this.enemy; }
    public String getGunName()      { return gun; }
    public Vec2D getOrigin()        { return initialPos; }
    public double getPower()        { return power; }
    public double getAngle()        { return angle; }
    public long getFireTime()       { return timeWhenFired; }
}


/* ---- manager\GunManager.java ---- */

class GunManager implements ManagerConstants {

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

        Enemy e = bot.radar().getClosestEnemy();

        if (bot.robot().getTime() % 4 == 0) {
            testVirtualAim(e);
        }

        /*
            Make sure every scanned enemy has a GunStats
            for every gun we have
        */
        for (Enemy enemy : targets.values()) { ensureGunStats(enemy); }

        checkForVirtualBulletsCollisions();

        /*  No enemy -> nothing to aim/shoot at, so skip  */
        if (e == null) { return; }

        Gun bestGun = getBestGunAgainst(e.getName());

        double power = GunUtils.getBestPower(e.getDistance());
        double angleInRadians = bestGun.aimAngleFunction(e, power);

        double gunTurn = Utils.normalRelativeAngle(angleInRadians - bot.robot().getGunHeadingRadians());

        bot.robot().setTurnGunRightRadians(gunTurn);

        if (bot.robot().getGunHeat() == 0) { bot.robot().setFire(power); }
    }

    /*
        Creates one virtual bullet for every gun
    */
    public void testVirtualAim(Enemy enemy) {

        if (enemy == null) {
            return;
        }

        double power = GunUtils.getBestPower(enemy.getDistance());

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


/* ---- manager\ManagerConstants.java ---- */

interface ManagerConstants {

    /*
        RADAR/SCAN
    */
    double RADAR_OVERSHOOT =        Math.toRadians(15);     /*  How much radar will overshoot when scanning  */
    long   LOST_CONTACT_TIME =      20;                     /*  Maximum lost contact time (in turns?)  */

    /*
        GUN/CANNON
    */
    double ROBOT_RADIUS =          18.0;                    /*  Radius of a bot when testing virtual bullets  */
    double MISS_DISTANCE_MARGIN =  60.0;                    /*  Extra distance to the enemy when checking for collisions  */

    /*
        WHEEL/MOVEMENT
    */
    int NUMBER_OF_RECORDS =         16;                      /*  Number of last position records  */
    int TICKS_BETWEEN_RECORD =      2;
}


/* ---- manager\RadarManager.java ---- */

class RadarManager implements ManagerConstants {

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

        // Keep refreshing targets in melee.
        // In 1v1, only redirect when contact becomes stale.
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


/* ---- manager\WheelManager.java ---- */

class WheelManager implements ManagerConstants {

    private final List<Vec2D> pastPositions = new ArrayList<>();
    DuskatronContext bot;

    private final Wheel surfer;
    private final Wheel MRM;

    private Wheel wheel;

    public WheelManager(DuskatronContext ctx) {

        this.bot =      ctx;
        this.MRM =      new MrmWheel(ctx);
        this.surfer =   new SurferWheel(ctx);

        /*  Using MRM at first  */
        this.wheel = this.MRM;
    }

    public void handleMovement() {

        recordPositions();

        /*
            If there's only one bot, use wave surfing
            otherwise use minimum risk movement
        */
        if(bot.arena().is1v1())    { wheel = surfer; }

        /*  Actually use the movement strategy  */
        wheel.move();
    }

    /*  Records previous places that we passed  */
    public void recordPositions() {
        if(bot.robot().getTime() % TICKS_BETWEEN_RECORD == 0) {

            pastPositions.add(new Vec2D(bot.robot().getX(), bot.robot().getY()));
            if (pastPositions.size() > NUMBER_OF_RECORDS) { pastPositions.removeFirst(); }
        }
    }

    public List<Vec2D> getRecordedPositions() { return pastPositions; }

    public void onPaint(Graphics2D g)               { wheel.onPaint(g); }
    public void onHitByBullet(HitByBulletEvent e)   { wheel.onHitByBullet(e); }
}


/* ---- math\AngleUtil.java ---- */

class AngleUtil {
    public static double normalizeAngle(double angle) {
        double normalized = angle % 360;

        if (normalized > 180) {
            normalized -= 360;
        } else if (normalized <= -180) {
            normalized += 360;
        }

        return normalized;
    }
}


/* ---- math\Vec2D.java ---- */

class Vec2D {
    public double x, y;

    public Vec2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2D add(Vec2D v)           { return new Vec2D(this.x + v.x, this.y + v.y); }
    public Vec2D sub(Vec2D v)           { return new Vec2D(this.x - v.x, this.y - v.y); }
    public double distance(Vec2D v)     { return Math.hypot(this.x - v.x, this.y - v.y); }

    public double distanceSq(Vec2D v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        return dx * dx + dy * dy;
    }

}


/* ---- movement\HawkOnFireWheel.java ---- */

/*
    HawkOnFireWheel

    Based on HawkOnFire rozu's movement adapted to Duskatron wheel
    architecture, see: https://robowiki.net/wiki/HawkOnFire/

    Movement samples random points within a search radius, scores each
    with a cheap antigravity evaluation.
*/
class HawkOnFireWheel extends Wheel implements Constants {

    private Vec2D nextDestination;
    private Vec2D lastDestination;

    public HawkOnFireWheel(DuskatronContext ctx) {
        super(ctx);
    }

    @Override
    public void move() {

        Vec2D myPos = new Vec2D(bot.robot().getX(), bot.robot().getY());

        if (nextDestination == null) {
            nextDestination = myPos;
            lastDestination = myPos;
        }


        List<Enemy> enemies = new ArrayList<>(bot.radar().getScannedBots().values());
        Enemy closest = bot.radar().getClosestEnemy();

        /*  Nothing scanned, skip until find someone  */
        if (closest == null) {
            return;
        }

        double distanceToTarget = sqrt(myPos.distanceSq(closest.getPosition()));
        double distanceToDestination = sqrt(myPos.distanceSq(nextDestination));

        if (distanceToDestination < HOF_ARRIVAL_THRESHOLD) {
            nextDestination = pickDestination(enemies, myPos, distanceToTarget);
            lastDestination = myPos;
            distanceToDestination = sqrt(myPos.distanceSq(nextDestination));
        }

        moveTowards(myPos, distanceToDestination);
    }

    /*
        Samples random points around the current
        position and choose the low-risk one to
        move to
    */
    private Vec2D pickDestination(
            List<Enemy> enemies,
            Vec2D myPos,
            double distanceToTarget
    ) {
        double myEnergy = bot.robot().getEnergy();

        double maxX = bot.arena().getWidth() - HOF_WALL_MARGIN;
        double maxY = bot.arena().getHeight() - HOF_WALL_MARGIN;

        int liveEnemies = 0;
        for (Enemy enemy : enemies) {
            if (enemy.exists()) {
                liveEnemies++;
            }
        }

        /*  Fancy math that makes rozu's bot so goated  */
        double addLast = 1 - rint(pow(Math.random(), max(liveEnemies, 1)));

        Vec2D best = nextDestination;
        double bestScore = score(best, enemies, myPos, myEnergy, addLast);

        /*  Search and score for the best candidate  */
        for (int i = 0; i < HOF_SEARCH_ATTEMPTS; i++) {
            double searchDist = min(
                    distanceToTarget * 0.8,
                    HOF_SEARCH_MIN_DIST + HOF_SEARCH_RANGE * Math.random()
            );
            double angle = 2 * PI * Math.random();

            Vec2D candidate = new Vec2D(
                    myPos.x + searchDist * sin(angle),
                    myPos.y + searchDist * cos(angle)
            );

            if (candidate.x < HOF_WALL_MARGIN || candidate.x > maxX
                    || candidate.y < HOF_WALL_MARGIN || candidate.y > maxY) {
                continue;
            }

            double candidateScore = score(candidate, enemies, myPos, myEnergy, addLast);

            if (candidateScore < bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }

        return best;
    }

    /*
        Lower is better, antigravity from each enemy, weighted by how
        dangerous it is and how angled the point is relative to that
        enemy, plus a term that rewards distance from the past recorded
        positions
    */
    private double score(Vec2D p, List<Enemy> enemies,
        Vec2D myPos,double myEnergy, double addLast) {

        double s = addLast * 0.08 / p.distanceSq(lastDestination);

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            double energyRatio = min(enemy.getEnergy() / myEnergy, 2.0);

            double angleToMe = atan2(myPos.x - p.x, myPos.y - p.y);
            double angleToEnemy = atan2(enemy.getPosition().x - p.x, enemy.getPosition().y - p.y);
            double orbitalPenalty = 1 + abs(cos(angleToMe - angleToEnemy));

            s += energyRatio * orbitalPenalty / p.distanceSq(enemy.getPosition());
        }

        return s;
    }

    /*
        Turns and moves toward the destination, flipping into reverse when
        the target is behind us so we never need more than a 90-degree turn
    */
    private void moveTowards(Vec2D myPos, double distanceToDestination) {
        double heading = bot.robot().getHeadingRadians();
        double angle = atan2(nextDestination.x - myPos.x, nextDestination.y - myPos.y) - heading;
        double direction = 1;

        if (cos(angle) < 0) {
            angle += PI;
            direction = -1;
        }

        angle = Utils.normalRelativeAngle(angle);

        bot.robot().setAhead(distanceToDestination * direction);
        bot.robot().setTurnRightRadians(angle);
        bot.robot().setMaxVelocity(abs(angle) > 1 ? 0 : 8.0);
    }
}


/* ---- movement\MrmWheel.java ---- */

/*
    Minimum Risk Movement Wheel

    Combine a bunch of constants to get the better sample
    point based on closer bots, wall, energy level
*/
class MrmWheel extends Wheel implements Constants {

    private final ArrayList<RiskPoint> currentRiskPoints = new ArrayList<>();

    public MrmWheel(DuskatronContext ctx) { super(ctx); }

    @Override
    public void move() {

        ArrayList<Enemy> enemies = new ArrayList<>(
                bot.radar().getScannedBots().values()
        );

        ArrayList<RiskPoint> points = new ArrayList<>(MRM_POINT_COUNT);

        double robotX = bot.robot().getX();
        double robotY = bot.robot().getY();

        for (int i = 0; i < MRM_POINT_COUNT; i++) {

            /*
              Robocode uses:
                0 radians =     north
                PI / 2 =        east
            */
            double angle = (PI * 2.0) * i / MRM_POINT_COUNT;
            double RANDOM_OFFSET = 50;

            double randomX  = (Math.random() * (RANDOM_OFFSET * 2.0)) - RANDOM_OFFSET;
            double randomY  = (Math.random() * (RANDOM_OFFSET * 2.0)) - RANDOM_OFFSET;

            Vec2D destination = new Vec2D(
                    robotX + sin(angle) * MRM_DISTANCE + randomX,
                    robotY + cos(angle) * MRM_DISTANCE + randomY
            );

            points.add(evaluateRisk(enemies, destination, angle));
        }

        currentRiskPoints.clear();
        currentRiskPoints.addAll(points);

        RiskPoint best = points.getFirst();

        for (RiskPoint point : points) {
            if (point.totalRisk() < best.totalRisk()) {
                best = point;
            }
        }

        /*  TODO: choose the best method here  */
        goTo(best.location());
    }

    private RiskPoint evaluateRisk(List<Enemy> enemies, Vec2D destination, double movementAngle) {

        /*  Get risk of enemies, closest bots and walls  */
        double enemyRisk = getEnemyRisk(enemies, destination, movementAngle);
        double closestBotRisk = getClosestBotRisk(enemies, destination);
        double wallRisk = getWallRisk(destination);

        double trailRisk = getTrailRisk(
                destination
        );

        double totalRisk =
                ENEMY_RISK_WEIGHT * enemyRisk
                        + CLOSEST_BOT_RISK_WEIGHT * closestBotRisk
                        + WALL_RISK_WEIGHT * wallRisk
                        + TRAIL_RISK_WEIGHT * trailRisk;

        return new RiskPoint(
                destination,
                enemyRisk,
                closestBotRisk,
                wallRisk,
                trailRisk,
                totalRisk);
    }

    /*
        Enemy risk combines:

        - distance
        - perpendicularity
        - enemy energy

        Individual enemy risks are combined
        into a bounded [0, 1] value
    */
    private double getEnemyRisk(
            List<Enemy> enemies,
            Vec2D destination,
            double movementAngle) {

        if (enemies.isEmpty()) { return 0.0; }

        double risk = 0.0;

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            Vec2D enemyPosition = enemy.getPosition();

            /*  Distance from the candidate destination to the enemy  */
            double distance = destination.distance(enemyPosition);

            /*  Close enemies are more dangerous  */
            double distanceRisk = exp(-distance / ENEMY_DISTANCE_SCALE);

            /* Direction from our candidate point to the enemy  */
            double enemyBearing = atan2(
                    enemyPosition.x - destination.x,
                    enemyPosition.y - destination.y);

            /*
                Diamond-style orbital movement
                perpendicular = safer, toward = more dangerous
             */
            double angleDifference = Utils.normalRelativeAngle(movementAngle - enemyBearing);
            double orbitalRisk = 0.25 + 0.75 * abs(cos(angleDifference));

            risk += distanceRisk * orbitalRisk;
        }

        return Math.clamp(risk, 0.0, 1.0);
    }

    private double getClosestBotRisk(List<Enemy> enemies, Vec2D destination) {

        if (enemies.size() < 2) {
            return 0.0;
        }

        int validEnemies = 0;
        int threatenedEnemies = 0;

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            validEnemies++;

            double ourDistanceSq = destination.distanceSq(enemy.getPosition());

            boolean weAreClosest = true;

            for (Enemy other : enemies) {
                if (!other.exists() || other == enemy) {
                    continue;
                }

                double otherDistanceSq = other.getPosition().distanceSq(enemy.getPosition());

                if (otherDistanceSq < ourDistanceSq) {
                    weAreClosest = false;
                    break;
                }
            }

            if (weAreClosest) { threatenedEnemies++; }
        }
        if (validEnemies == 0) { return 0.0;}

        return (double) threatenedEnemies / validEnemies;
    }

    /*  Wall risk  */
    private double getWallRisk(Vec2D position) {

        double fieldWidth = bot.arena().getWidth();
        double fieldHeight = bot.arena().getHeight();

        double distanceToWall = min(
                min(position.x, fieldWidth - position.x),
                min(position.y, fieldHeight - position.y));


        if (distanceToWall <= 0.0) { return Double.POSITIVE_INFINITY; }
        if (distanceToWall >= WALL_MARGIN) { return 0.0; }

        /*  Quadratic increase as we approach the wall  */
        double closeness = (WALL_MARGIN - distanceToWall) / WALL_MARGIN;
        return closeness * closeness;
    }

    /*  Avoid recently visited positions  */
    private double getTrailRisk(Vec2D destination) {

        List<Vec2D> history = bot.wheel().getRecordedPositions();

        if (history.isEmpty()) { return 0.0; }

        int count = min(history.size(), TRAIL_LENGTH);

        double weightedRisk = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < count; i++) {

            Vec2D oldPosition = history.get(history.size() - 1 - i);
            double distance = destination.distance(oldPosition);
            double recency = (double) (count - i) / count;

            /*
                1 near the trail,
                approaches 0 as distance increases.
            */
            double distanceRisk = exp(-distance / TRAIL_DISTANCE_SCALE);

            weightedRisk += recency * distanceRisk;
            totalWeight += recency;
        }

        if (totalWeight == 0.0) { return 0.0; }

        return weightedRisk / totalWeight;
    }

    /*
        USE OF AI:
    */
    public void onPaint(Graphics2D g) {
        if (currentRiskPoints.isEmpty()) {
            return;
        }

        int radius = 6;

        for (RiskPoint point : currentRiskPoints) {
            double risk = point.totalRisk();

            /*
             * Infinity means outside the arena.
             */
            double normalized;

            if (!Double.isFinite(risk)) {
                normalized = 1.0;
            } else {
                normalized = Math.clamp(
                        risk,
                        0.0,
                        1.0
                );
            }

            int red =
                    (int) (255 * normalized);

            int blue =
                    (int) (255 * (1.0 - normalized));

            g.setColor(
                    new Color(
                            red,
                            0,
                            blue,
                            190
                    )
            );

            int x =
                    (int) point.location().x - radius;

            int y =
                    (int) point.location().y - radius;

            g.fillOval(
                    x,
                    y,
                    radius * 2,
                    radius * 2
            );
        }

        /*
         * Draw the MRM search circle.
         */
        g.setColor(
                new Color(
                        255,
                        255,
                        255,
                        100
                )
        );

        int radiusAroundRobot =
                (int) MRM_DISTANCE;

        int robotX =
                (int) bot.robot().getX();

        int robotY =
                (int) bot.robot().getY();

        g.drawOval(
                robotX - radiusAroundRobot,
                robotY - radiusAroundRobot,
                radiusAroundRobot * 2,
                radiusAroundRobot * 2
        );

        /*
         * Highlight the best point.
         */
        RiskPoint best =
                currentRiskPoints.get(0);

        for (RiskPoint point : currentRiskPoints) {
            if (point.totalRisk() < best.totalRisk()) {
                best = point;
            }
        }

        g.setColor(Color.GREEN);

        g.fillOval(
                (int) best.location().x - 8,
                (int) best.location().y - 8,
                16,
                16
        );

        g.drawLine(
                robotX,
                robotY,
                (int) best.location().x,
                (int) best.location().y
        );
    }


    public record RiskPoint(
            Vec2D location,
            double enemyRisk,
            double closestBotRisk,
            double wallRisk,
            double trailRisk,
            double totalRisk
    ) { }
}


/* ---- movement\SurferWheel.java ---- */

/*
    SurferWheel

    Port of the MiniSurfer wave surfing bot, see:
    (robowiki.net/wiki/MiniSurfer), adapted to
    Duskatron

    I'm not using Vec2D because the original
    implementation uses Point2D, so I'm sticking
    with it

    All comments in // double slash was made by
    AI to help me to understand MiniSurfer
    architecture, so I'll keep the // for you
    guys know where I used AI

    Radar handling stays with RadarManager.java
*/
class SurferWheel extends Wheel implements Constants {

    private final double[] surfStats =                  new double[BINS];
    private Point2D.Double myLocation;
    private Point2D.Double enemyLocation;

    private final ArrayList<EnemyWave> enemyWaves =     new ArrayList<>();
    private final ArrayList<Integer> surfDirections =   new ArrayList<>();
    private final ArrayList<Double> surfAbsBearings =   new ArrayList<>();
    public static Rectangle2D.Double fieldRect;

    public SurferWheel(DuskatronContext ctx) {
        super(ctx);

        /*  Represents battlefield, used in wall smoothing  */
        fieldRect = new java.awt.geom.Rectangle2D.Double(
                SURF_SMOOTHING_MARGIN,
                SURF_SMOOTHING_MARGIN,
                bot.arena().getWidth()  -SURF_SMOOTHING_MARGIN * 2,
                bot.arena().getHeight() -SURF_SMOOTHING_MARGIN * 2);
    }

    @Override
    public void move() {

        Enemy enemy = bot.radar().getClosestEnemy();

        /*  Nothing scanned, skip until we find someone  */
        if (enemy == null || !enemy.exists()) { return; }
        if (enemy.getLastScanTime() != bot.robot().getTime()) { return; }

        myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

        double lateralVelocity = bot.robot().getVelocity() * Math.sin(enemy.getBearingRadians());
        double absBearing = enemy.getBearingRadians() + bot.robot().getHeadingRadians();

        surfDirections.addFirst((lateralVelocity >= 0) ? 1 : -1);
        surfAbsBearings.addFirst(absBearing + Math.PI);

        if (enemy.hasShoot() && surfDirections.size() > 2) {
            EnemyWave ew =          new EnemyWave();
            ew.bulletVelocity =     GunUtils.getBulletSpeed(enemy.getBulletPower());
            ew.fireTime =           bot.robot().getTime() - 1;  /*  Subtract 1 ticks to correct the radius of the wave  */
            ew.distanceTraveled =   2 * ew.bulletVelocity;      /*  Same here  */
            ew.direction =          surfDirections.get(2);
            ew.directAngle =        surfAbsBearings.get(2);
            ew.fireLocation = (Point2D.Double) enemyLocation.clone();

            enemyWaves.add(ew);
        }

        // update after EnemyWave detection, because that needs the previous
        // enemy location as the source of the wave
        enemyLocation = project(myLocation, absBearing, enemy.getDistance());

        updateWaves();
        doSurfing();
    }

    public void updateWaves() {
        for (int x = 0; x < enemyWaves.size(); x++) {
            EnemyWave ew = enemyWaves.get(x);

            ew.distanceTraveled = (bot.robot().getTime() - ew.fireTime) * ew.bulletVelocity;
            if (ew.distanceTraveled >
                    myLocation.distance(ew.fireLocation) + 50) {
                enemyWaves.remove(x);
                x--;
            }
        }
    }

    public EnemyWave getClosestSurfableWave() {

        double closestDistance = Double.POSITIVE_INFINITY;
        EnemyWave surfWave = null;

        for (EnemyWave ew : enemyWaves) {

            double distance = myLocation.distance(ew.fireLocation) - ew.distanceTraveled;

            if (distance > ew.bulletVelocity && distance < closestDistance) {
                surfWave = ew;
                closestDistance = distance;
            }
        }

        return surfWave;
    }


    /*
         CREDIT: mini sized predictor from Apollon, by rozu
         See: http://robowiki.net?Apollon
    */
    public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
        Point2D.Double predictedPosition = (Point2D.Double) myLocation.clone();
        double predictedVelocity = bot.robot().getVelocity();
        double predictedHeading = bot.robot().getHeadingRadians();
        double maxTurning, moveAngle, moveDir;

        int counter = 0; // number of ticks in the future
        boolean intercepted = false;

        do {
            moveAngle =
                    wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
                            predictedPosition) + (direction * (Math.PI / 2)), direction)
                            - predictedHeading;
            moveDir = 1;

            if (Math.cos(moveAngle) < 0) {
                moveAngle += Math.PI;
                moveDir = -1;
            }

            moveAngle = Utils.normalRelativeAngle(moveAngle);

            // maxTurning is built in like this, you can't turn more than this in one tick
            maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(predictedVelocity));
            predictedHeading = Utils.normalRelativeAngle(predictedHeading
                    + limit(-maxTurning, moveAngle, maxTurning));

            // this one is nice ;). if predictedVelocity and moveDir have
            // different signs you want to break down
            // otherwise you want to accelerate (look at the factor "2")
            predictedVelocity += (predictedVelocity * moveDir < 0 ? 2 * moveDir : moveDir);
            predictedVelocity = limit(-8, predictedVelocity, 8);

            // calculate the new predicted position
            predictedPosition = project(predictedPosition, predictedHeading, predictedVelocity);

            counter++;

            if (predictedPosition.distance(surfWave.fireLocation) <
                    surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
                            + surfWave.bulletVelocity) {
                intercepted = true;
            }
        } while (!intercepted && counter < 500);

        return predictedPosition;
    }

    public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
        double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
                - ew.directAngle);
        double factor = Utils.normalRelativeAngle(offsetAngle)
                / maxEscapeAngle(ew.bulletVelocity) * ew.direction;

        return (int) limit(0,
                (factor * ((double) (BINS - 1) / 2)) + ((double) (BINS - 1) / 2),
                BINS - 1);
    }

    public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
        int index = getFactorIndex(ew, targetLocation);

        for (int x = 0; x < BINS; x++) {
            // for the spot bin that we were hit on, add 1;
            // for the bins next to it, add 1 / 2;
            // the next one, add 1 / 5; and so on...
            surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
        }
    }

    /*  Let the surf stats learn from the waves that actually hit us  */
    public void onHitByBullet(HitByBulletEvent e) {

        /*
            If the enemyWaves collection is empty, we must
            have missed the detection of this wave somehow
        */
        if (!enemyWaves.isEmpty()) {
            Point2D.Double hitBulletLocation = new Point2D.Double(e.getBullet().getX(), e.getBullet().getY());

            EnemyWave hitWave = null;

            myLocation = new Point2D.Double(bot.robot().getX(), bot.robot().getY());

            /*  look through the EnemyWaves, and find one that could've hit us  */
            for (EnemyWave ew : enemyWaves) {
                if (Math.abs(ew.distanceTraveled -
                        myLocation.distance(ew.fireLocation)) < 50
                        && Math.abs(GunUtils.getBulletSpeed(e.getBullet().getPower())
                        - ew.bulletVelocity) < 0.001) {
                    hitWave = ew;

                    break;
                }
            }

            if (hitWave != null) {

                logHit(hitWave, hitBulletLocation);
                enemyWaves.remove(enemyWaves.lastIndexOf(hitWave));
            }
        }
    }

    public double checkDanger(EnemyWave surfWave, int direction) {

        int index = getFactorIndex(surfWave, predictPosition(surfWave, direction));
        return surfStats[index];
    }

    public void doSurfing() {
        EnemyWave surfWave = getClosestSurfableWave();

        if (surfWave == null) { return; }

        double dangerLeft = checkDanger(surfWave, -1);
        double dangerRight = checkDanger(surfWave, 1);

        double goAngle = absoluteBearing(surfWave.fireLocation, myLocation);
        if (dangerLeft < dangerRight) {
            goAngle = wallSmoothing(myLocation, goAngle - (Math.PI / 2), -1);
        } else {
            goAngle = wallSmoothing(myLocation, goAngle + (Math.PI / 2), 1);
        }

        setBackAsFront(bot.robot(), goAngle);
    }

    /*  Iterative WallSmoothing by Kawigi  */
    public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
        while (!fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
            angle += orientation * 0.05;
        }
        return angle;
    }

    /*
        CREDIT: from CassiusClay, by PEZ
        Returns point length away from sourceLocation, at angle
        See: robowiki.net?CassiusClay
    */
    public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
                sourceLocation.y + Math.cos(angle) * length);
    }

    public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
        return Math.atan2(target.x - source.x, target.y - source.y);
    }

    public static double limit(double min, double value, double max) { return Math.clamp(value, min, max); }
    public static double maxEscapeAngle(double velocity) { return Math.asin(8.0 / velocity); }

    public static void setBackAsFront(robocode.AdvancedRobot robot, double goAngle) {

        double angle = Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
        if (Math.abs(angle) > (Math.PI / 2)) {
            if (angle < 0) {
                robot.setTurnRightRadians(Math.PI + angle);
            } else {
                robot.setTurnLeftRadians(Math.PI - angle);
            }
            robot.setBack(100);
        } else {
            if (angle < 0) {
                robot.setTurnLeftRadians(-1 * angle);
            } else {
                robot.setTurnRightRadians(angle);
            }
            robot.setAhead(100);
        }
    }

    /*  Debug stuff  */
    public void onPaint(Graphics2D g) {
        if (myLocation == null) {
            return;
        }

        g.setColor(java.awt.Color.red);
        for (EnemyWave w : enemyWaves) {
            Point2D.Double center = w.fireLocation;

            int radius = (int) w.distanceTraveled;

            if (radius - 40 < center.distance(myLocation) ) {
                g.drawOval((int) (center.x - radius), (int) (center.y - radius), radius * 2, radius * 2);
            }
        }
    }
}

class EnemyWave {
    Point2D.Double fireLocation;
    long fireTime;
    double bulletVelocity, directAngle, distanceTraveled;
    int direction;

    public EnemyWave() { }
}


/* ---- movement\Wheel.java ---- */

/*
    Wheel have some utils methods to help subclasses
    implementing it
*/
abstract class Wheel implements Constants {

    DuskatronContext bot;

    public Wheel(DuskatronContext ctx) { this.bot = ctx; }

    public abstract void move();

    protected void goTo(Vec2D location) {
        double dx = location.x - bot.robot().getX();
        double dy = location.y - bot.robot().getY();

        double angleToTarget = Math.atan2(dx, dy);
        double turnAngle = Utils.normalRelativeAngle(angleToTarget - bot.robot().getHeadingRadians());

        double turnRadians = Math.atan(Math.tan(turnAngle));
        bot.robot().setTurnRightRadians(turnRadians);

        double distance = Math.hypot(dx, dy);
        bot.robot().setAhead(Math.cos(turnAngle) * distance);
    }

    public void optimalTurnAndGo(double angleToTurn, double ahead) {


        double tune = Math.sin(bot.robot().getTime() / 2.0) / 2.0;

        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;

            bot.robot().setTurnRightRadians(
                    AngleUtil.normalizeAngle(angleToTurn)
            );
            bot.robot().setAhead(-ahead + tune);
        } else {
            bot.robot().setTurnRightRadians(angleToTurn + tune);
            bot.robot().setAhead(ahead);
        }
    }

    public double smoothHeading(
            double desiredAngle,
            double x,
            double y,
            Arena arena,
            double margin) {

        final double angleStep = toRadians(WALL_SMOOTH_ANGLE_STEP);
        double heading = bot.robot().getHeadingRadians();

        if (isSafeAtOffset(
                desiredAngle,
                x,
                y,
                heading,
                arena,
                margin
        )) { return desiredAngle; }

        for (int i = 1; i <= WALL_SMOOTH_MAX_STEPS; i++) {

            double offset = angleStep * i;
            double left = desiredAngle - offset;

            if (isSafeAtOffset(
                    left,
                    x,
                    y,
                    heading,
                    arena,
                    margin
            )) { return left; }

            double right = desiredAngle + offset;

            if (isSafeAtOffset(
                    right,
                    x,
                    y,
                    heading,
                    arena,
                    margin)) {

                return right;
            }
        }

        return desiredAngle;
    }

    private boolean isSafeAtOffset(
            double offset,
            double x,
            double y,
            double currentHeading,
            Arena arena,
            double margin) {

        double absoluteHeading = currentHeading + offset;
        double projectedX = x + LOOK_AHEAD_DIST * sin(absoluteHeading);
        double projectedY = y + LOOK_AHEAD_DIST * cos(absoluteHeading);

        return isInsideSafeRect(
                projectedX,
                projectedY,
                arena.getWidth(),
                arena.getHeight(),
                margin
        );
    }

    public boolean isInsideSafeRect(
            double x,
            double y,
            double width,
            double height,
            double margin) {

        return margin <= x
                && x <= width - margin
                && margin <= y
                && y <= height - margin;}

    public void onPaint(Graphics2D g) {}
    public void onHitByBullet(HitByBulletEvent e) {}
}
