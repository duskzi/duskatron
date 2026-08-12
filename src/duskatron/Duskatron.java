package duskatron;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.gun.Gun;
import duskatron.movement.Wheel;
import duskatron.radar.Radar;
import duskatron.waves.WaveBullet;
import robocode.*;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;

public class Duskatron extends AdvancedRobot {

    /*
        Bot's context, it holds references to all
        duskatron parts listed below
    */
    DuskatronContext ctx =  new DuskatronContext();

    /*
        Robo-parts
            Radar:  Manages aim
            Gun:    Shoots when aim find something
            Wheel:  Handles movement
    */
    public Radar radar =    new Radar(ctx);
    public Gun gun =        new Gun(ctx);
    public Wheel wheel =    new Wheel(ctx);

    /*
        Arena holds battlefield width and height
    */
    public Arena arena;


    ArrayList<WaveBullet> waves = new ArrayList<>();

    static int[] stats = new int[31]; // 31 is the number of unique GuessFactors we're using
    // Note: this must be odd number so we can get
    // GuessFactor 0 at middle.
    int direction = 1;

    @Override
    public void run() {

         arena = new Arena(
                this.getBattleFieldWidth(),
                this.getBattleFieldHeight());

        /*
            Pass all parts references to the
            context to be used later inside
            each part
        */
        ctx.bindParts(this, radar, gun, wheel, arena);

        /*
            Allows radar and gun to rotate
            independently for advanced
            scanning and shooting
        */
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        /*  Bot colors  */
        setRadarColor   (Color.ORANGE);
        setBodyColor    (Color.BLACK);
        setGunColor     (Color.DARK_GRAY);

        /*  Scans for all robots  */
        radar.init();

        System.out.println("Arena: (" + arena.getWidth() + " " + arena.getHeight() + ")");

        /*  Main loop  */
        while (true) {

            wheel.handleMovement();
            gun.aimAndFire();
            this.execute();
        }

    }

    /*  Radar's method calls  */
    public void onScannedRobot(ScannedRobotEvent e)     { radar.trackScannedBots(e);


        // Enemy absolute bearing, you can use your one if you already declare it.
        double absBearing = getHeadingRadians() + e.getBearingRadians();

        // find our enemy's location:
        double ex = getX() + Math.sin(absBearing) * e.getDistance();
        double ey = getY() + Math.cos(absBearing) * e.getDistance();

        // Let's process the waves now:
        for (int i=0; i < waves.size(); i++)
        {
            WaveBullet currentWave = (WaveBullet)waves.get(i);
            if (currentWave.checkHit(ex, ey, getTime()))
            {
                waves.remove(currentWave);
                i--;
            }
        }

        double power = 1;
        // don't try to figure out the direction they're moving
        // they're not moving, just use the direction we had before
        if (e.getVelocity() != 0)
        {
            if (Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() < 0)
                direction = -1;
            else
                direction = 1;
        }
        int[] currentStats = stats; // This seems silly, but I'm using it to
        // show something else later
        WaveBullet newWave = new WaveBullet(getX(), getY(), absBearing, power,
                direction, getTime(), currentStats);

        int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
        for (int i=0; i<31; i++)
            if (currentStats[bestindex] < currentStats[i])
                bestindex = i;

        // this should do the opposite of the math in the WaveBullet:
        double guessfactor = (double)(bestindex - (stats.length - 1) / 2)
                / ((stats.length - 1) / 2);
        double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
        double gunAdjust = Utils.normalRelativeAngle(
                absBearing - getGunHeadingRadians() + angleOffset);
        setTurnGunRightRadians(gunAdjust);

        if (setFireBullet(power) != null)
            waves.add(newWave);

    }          // Look and turn radar
    public void onRobotDeath(RobotDeathEvent e)         { radar.removeEnemy(e.getName()); }     // Removes from target map
    public void onPaint(Graphics2D g)                   { radar.onPaint(g); gun.onPaint(g); }   // Debug painting
}
