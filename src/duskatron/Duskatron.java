package duskatron;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.gun.Gun;
import duskatron.movement.Wheel;
import duskatron.radar.Radar;
import robocode.*;

import java.awt.*;

public class Duskatron extends AdvancedRobot {

    /*
        Robo-parts
            Radar:  Manages aim
            Gun:    Shoots when aim find something
            Wheel:  Handles movement
    */

    DuskatronContext ctx =  new DuskatronContext();

    public Radar radar =    new Radar(ctx);
    public Gun gun =        new Gun(ctx);
    public Wheel wheel =    new Wheel(ctx);

    /*
        Arena holds battlefield width and height
    */
    public Arena arena =    new Arena(
            this.getBattleFieldWidth(),
            this.getBattleFieldHeight());

    @Override
    public void run() {

        /*
            Pass all parts references to the
            context to be used later inside
            each part
        */
        ctx.bindParts(radar, gun, wheel, arena);

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

        /*  Main loop  */
        while (true) {

            wheel.handleMovement();
            gun.aimAndFire();
            this.execute();
        }

    }

    @Override
    public void onHitWall(HitWallEvent event) {
        setAhead(-80);
    }

    /*  Radar's method calls  */
    public void onScannedRobot(ScannedRobotEvent e)     { radar.trackScannedBots(e); }          // Look and turn radar
    public void onRobotDeath(RobotDeathEvent e)         { radar.removeEnemy(e.getName()); }     // Removes from target map
    public void onPaint(Graphics2D g)                   { radar.onPaint(g); }                   // Debug painting
}
