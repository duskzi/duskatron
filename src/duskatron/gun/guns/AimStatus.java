package duskatron.gun.guns;

public class AimStatus {
    double angle;
    boolean outside;

    public void setAngle(double angle) { this.angle = angle; }
    public void setOutside(boolean willBeOutside) { this.outside = willBeOutside; }

    public double getAngle() { return angle; }
    public boolean isOutside() { return outside; }
}
