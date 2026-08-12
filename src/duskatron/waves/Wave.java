package duskatron.waves;

import duskatron.math.Vec2D;

public class Wave {

    public Wave(Vec2D origin, long fireTime, double bulletSpeed, double fireAngle){
        this.origin = origin;
        this.fireTime = fireTime;
        this.bulletSpeed = bulletSpeed;
        this.fireAngle = fireAngle;
    }

    Vec2D origin;
    long fireTime;
    double bulletSpeed;
    double fireAngle;
}
