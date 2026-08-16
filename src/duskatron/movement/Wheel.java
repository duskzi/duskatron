package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.math.Vec2D;
import robocode.HitByBulletEvent;
import robocode.util.Utils;
import java.awt.*;
import static duskatron.math.AngleUtil.normalizeAngle;
import static java.lang.Math.*;

/*
    Wheel have some utils methods to help subclasses
    implementing it
*/
public abstract class Wheel implements Constants {

    DuskatronContext bot;

    public Wheel(DuskatronContext ctx) { this.bot = ctx; }

    public abstract void move();

    protected void goTo(Vec2D location) {
        double dx = location.x - bot.robot().getX();
        double dy = location.y - bot.robot().getY();

        double angleToTarget = Math.atan2(dx, dy);
        double turnAngle = Utils.normalRelativeAngle(angleToTarget - bot.robot().getHeadingRadians());

        double turnRadians = Math.atan(Math.tan(turnAngle));
        bot.robot().setTurnRightRadians(turnRadians);

        double distance = Math.hypot(dx, dy);
        bot.robot().setAhead(Math.cos(turnAngle) * distance);
    }

    public void optimalTurnAndGo(double angleToTurn, double ahead) {


        double tune = Math.sin(bot.robot().getTime() / 2.0) / 2.0;

        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;

            bot.robot().setTurnRightRadians(
                    normalizeAngle(angleToTurn)
            );
            bot.robot().setAhead(-ahead + tune);
        } else {
            bot.robot().setTurnRightRadians(angleToTurn + tune);
            bot.robot().setAhead(ahead);
        }
    }

    public double smoothHeading(
            double desiredAngle,
            double x,
            double y,
            Arena arena,
            double margin) {

        final double angleStep = toRadians(WALL_SMOOTH_ANGLE_STEP);
        double heading = bot.robot().getHeadingRadians();

        if (isSafeAtOffset(
                desiredAngle,
                x,
                y,
                heading,
                arena,
                margin
        )) { return desiredAngle; }

        for (int i = 1; i <= WALL_SMOOTH_MAX_STEPS; i++) {

            double offset = angleStep * i;
            double left = desiredAngle - offset;

            if (isSafeAtOffset(
                    left,
                    x,
                    y,
                    heading,
                    arena,
                    margin
            )) { return left; }

            double right = desiredAngle + offset;

            if (isSafeAtOffset(
                    right,
                    x,
                    y,
                    heading,
                    arena,
                    margin)) {

                return right;
            }
        }

        return desiredAngle;
    }

    private boolean isSafeAtOffset(
            double offset,
            double x,
            double y,
            double currentHeading,
            Arena arena,
            double margin) {

        double absoluteHeading = currentHeading + offset;
        double projectedX = x + LOOK_AHEAD_DIST * sin(absoluteHeading);
        double projectedY = y + LOOK_AHEAD_DIST * cos(absoluteHeading);

        return isInsideSafeRect(
                projectedX,
                projectedY,
                arena.getWidth(),
                arena.getHeight(),
                margin
        );
    }

    public boolean isInsideSafeRect(
            double x,
            double y,
            double width,
            double height,
            double margin) {

        return margin <= x
                && x <= width - margin
                && margin <= y
                && y <= height - margin;}

    public void onPaint(Graphics2D g) {}
    public void onHitByBullet(HitByBulletEvent e) {}
}