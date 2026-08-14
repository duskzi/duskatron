package duskatron;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.gun.Cannon;
import duskatron.gun.Gun;
import duskatron.gun.GunManager;
import duskatron.movement.HawkOnFireWheel;
import duskatron.movement.Wheel;
import duskatron.radar.Radar;
import robocode.*;

import java.awt.*;

public class Duskatron extends AdvancedRobot {

    /*
        Bot's context, it holds references to all
        duskatron parts listed below
    */
    DuskatronContext ctx = new DuskatronContext();

    /*
        Robo-parts
            Radar:  Manages aim
            Gun:    Shoots when aim find something
            Wheel:  Handles movement
    */
    public Radar radar =        new Radar(ctx);
    public Cannon gun =         new GunManager(ctx);
    public Wheel wheel =        new HawkOnFireWheel(ctx);

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
        ctx.bindParts(this, radar, gun, wheel, arena);

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

        /*  Scans for all robots  */
        radar.init();

        System.out.println("Arena: (" + arena.getWidth() + " " + arena.getHeight() + ")");

        /*  Main loop  */
        while (true) {
            wheel.handleMovement();
            wheel.recordPositions();

            gun.handleGun();
            this.execute();
        }
    }

    /*  Radar's method calls  */
    public void onScannedRobot(ScannedRobotEvent e) {
        radar.trackScannedBots(e);
        //gun.onScannedRobot(e);
    }

    public void onRobotDeath(RobotDeathEvent e) {
        radar.removeEnemy(e.getName());
    }

    public void onPaint(Graphics2D g) {
        radar.onPaint(g);
        gun.onPaint(g);
        wheel.onPaint(g);
    }
}
