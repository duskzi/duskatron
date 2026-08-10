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
    public Gun gun =         new Gun(this, radar);
    public Wheel wheel =     new Wheel(this, radar);

    @Override
    public void run() {

        /*
            Allows radar and gun to rotate
            independently for advanced
            scanning and shooting
        */
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        /*  Scans for all robots  */
        radar.init();

        while(true){

            wheel.handleMovement();
            gun.aimAndFire();
            this.execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent e) {

        radar.trackScannedBots(e);
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {

        radar.removeEnemy(e.getName());
    }

    @Override
    public void onPaint(Graphics2D g) {
        radar.onPaint(g);
    }
}
