package duskatron.enemy;

import robocode.ScannedRobotEvent;
import robocode.Robot;

public class Enemy {

    private String name;
    private double x, y;
    private double bearing, distance, energy, heading, velocity;

    public void update(ScannedRobotEvent e, Robot me) {
        this.name = e.getName();
        this.bearing = e.getBearing();
        this.distance = e.getDistance();
        this.energy = e.getEnergy();
        this.heading = e.getHeading();
        this.velocity = e.getVelocity();

        double absoluteBearing = Math.toRadians(me.getHeading() + e.getBearing());
        this.x = me.getX() + Math.sin(absoluteBearing) * e.getDistance();
        this.y = me.getY() + Math.cos(absoluteBearing) * e.getDistance();
    }

    public void reset() { this.name = ""; }
    public boolean exists() { return !this.name.isEmpty(); }

    /*  Getters  */
    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getHeading() { return heading; }
    public double getVelocity() { return velocity; }
    public double getDistance() { return distance; }
    public double getEnergy() { return energy; }

}
