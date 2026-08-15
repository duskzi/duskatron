package duskatron.enemy;

import duskatron.math.Vec2D;
import robocode.ScannedRobotEvent;
import robocode.Robot;

import java.util.ArrayList;
import java.util.List;

public class Enemy {

    private String name;
    private double x, y;
    private double bearing, distance, energy, heading, velocity;
    private long lastScanTime;

    public void update(ScannedRobotEvent e, Robot me) {
        this.name =         e.getName();
        this.bearing =      e.getBearing();
        this.distance =     e.getDistance();
        this.heading =      e.getHeading();
        this.velocity =     e.getVelocity();

        this.energy =       e.getEnergy();

        double absoluteBearing =    Math.toRadians(me.getHeading() + e.getBearing());
        this.x =                    me.getX() + Math.sin(absoluteBearing) * e.getDistance();
        this.y =                    me.getY() + Math.cos(absoluteBearing) * e.getDistance();

        this.lastScanTime = me.getTime();
    }

    public void reset()                     { this.name = ""; }
    public boolean exists()                 { return !this.name.isEmpty(); }

    /*  Getters  */
    public Vec2D getPosition()              { return new Vec2D(x, y); }
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
    public long getLastScanTime()           { return lastScanTime; }
}
