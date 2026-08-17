package duskatron;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.manager.GunManager;
import duskatron.manager.WheelManager;
import duskatron.manager.RadarManager;
import robocode.*;
import java.awt.*;

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
