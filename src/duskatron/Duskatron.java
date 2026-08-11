package duskatron;

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
    public Radar radar =     new Radar(this);
    public Gun gun =         new Gun(this);
    public Wheel wheel =     new Wheel(this);

    @Override
    public void run() {

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
