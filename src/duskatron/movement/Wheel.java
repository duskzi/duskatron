package duskatron.movement;

import duskatron.Duskatron;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Antigravity;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import duskatron.radar.Radar;
import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class Wheel {

    /* Wheel constants  */
    public static final double  ENEMY_STRENGTH      = 50;
    public static final double  WALLS_STRENGTH      = 40;
    public static double        MARGIN              = 50.0;
    public static double        WALL_TANGENT_FACTOR = 0.78;

    /*  Bot parts inside bot context  */
    private final DuskatronContext bot;

    public Wheel(DuskatronContext ctx) { this.bot = ctx; }

    public void handleMovement() {

        /*  Get radar's scanned bots  */
        Map<String, Enemy> scannedBots = radar.getScannedBots();

        Vec2D enemiesForce  = Antigravity.getEnemyForce(
                root.getX(),
                root.getY(),
                scannedBots);

        Vec2D finalForce    = enemiesForce;

        /*  Converting vector to angle  */
        double targetAngle  = Math.atan2(finalForce.x, finalForce.y);
        double angleToTurn  = normalizeAngle(targetAngle - root.getHeadingRadians());

        angleToTurn = smoothHeading(angleToTurn, root.getX(), root.getY(), )

        optimalTurnAndGo(angleToTurn, 100);

    }

    public void optimalTurnAndGo( double angleToTurn, double ahead ) {
        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;
            root.setTurnRightRadians(normalizeAngle(angleToTurn));
            root.setAhead(-ahead);
        } else {
            root.setTurnRightRadians(angleToTurn);
            root.setAhead(ahead);
        }
    }

    public double smoothHeading(double desiredAngle, double x, double y, Arena arena, double margin){

        double lookAheadDistance =  30;
        double angleStep =          5;

        double heading = desiredAngle;

        while(!isInsideSafeRect(x, y, arena.getWidth(), arena.getHeight(), margin)){
            /*  TODO: Check for the best orientation (counterclockwise or clockwise)  */
            heading += angleStep;
        }

        return heading;
    }

    public boolean isInsideSafeRect(double x, double y, double w, double h, double margin){

        return (margin <= x <= w-margin) && (margin <= y <= h-margin)
    }
}