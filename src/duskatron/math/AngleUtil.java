package duskatron.math;

public class AngleUtil {
    public static double normalizeAngle(double angle) {
        double normalized = angle % 360;

        if (normalized > 180) {
            normalized -= 360;
        } else if (normalized <= -180) {
            normalized += 360;
        }

        return normalized;
    }
}
