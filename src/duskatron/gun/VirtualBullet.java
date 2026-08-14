package duskatron.gun;

import duskatron.math.Vec2D;

public class VirtualBullet {

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
