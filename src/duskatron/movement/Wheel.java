package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Antigravity;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.HitByBulletEvent;
import robocode.util.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class Wheel implements Constants {

    private List<Vec2D> pastPositions = new ArrayList<>();
    DuskatronContext bot;

    public Wheel(DuskatronContext ctx) {
        this.bot = ctx;
    }

    public void recordPositions() {

        pastPositions.add(new Vec2D(bot.robot().getX(), bot.robot().getY()));

        if (pastPositions.size() > NUMBER_OF_RECORDS) { pastPositions.removeFirst(); }
    }

    public List<Vec2D> getRecordedPositions() {
        return pastPositions;
    }

    public void handleMovement() {
        Map<String, Enemy> scannedBots = bot.radar().getScannedBots();

        Vec2D enemyForce = Antigravity.getMovementForce(
                bot.robot().getX(),
                bot.robot().getY(),
                scannedBots,
                bot.robot().getTime()
        );

        double targetAngle = Math.atan2(enemyForce.x, enemyForce.y);

        double angleToTurn = normalizeAngle(
                targetAngle - bot.robot().getHeadingRadians()
        );

        angleToTurn = smoothHeading(
                angleToTurn,
                bot.robot().getX(),
                bot.robot().getY(),
                bot.arena(),
                MARGIN
        );

        optimalTurnAndGo(angleToTurn, 100);
    }

    protected void goTo(Vec2D location) {
        double dx = location.x - bot.robot().getX();
        double dy = location.y - bot.robot().getY();

        double angleToTarget = Math.atan2(dx, dy);
        double turnAngle = Utils.normalRelativeAngle(angleToTarget - bot.robot().getHeadingRadians());

        // Folding into [-90°, 90°] via atan(tan(...)) means "facing away" is handled
        // by driving backward instead of doing a near-180° turn.
        double turnRadians = Math.atan(Math.tan(turnAngle));
        bot.robot().setTurnRightRadians(turnRadians);

        double distance = Math.hypot(dx, dy);
        // Scaling by cos(turnAngle) kills forward motion while still turning hard,
        // and ramps it back up as heading converges — no lurch, no overshoot spikes.
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
            double margin
    ) {

        final double angleStep = Math.toRadians(2.0);

        double heading = bot.robot().getHeadingRadians();

        if (isSafeAtOffset(
                desiredAngle,
                x,
                y,
                heading,
                arena,
                margin,
                LOOK_AHEAD_DIST
        )) {
            return desiredAngle;
        }

        for (int i = 1; i <= 90; i++) {
            double offset = angleStep * i;

            double left = desiredAngle - offset;

            if (isSafeAtOffset(
                    left,
                    x,
                    y,
                    heading,
                    arena,
                    margin,
                    LOOK_AHEAD_DIST
            )) {
                return left;
            }

            double right = desiredAngle + offset;

            if (isSafeAtOffset(
                    right,
                    x,
                    y,
                    heading,
                    arena,
                    margin,
                    LOOK_AHEAD_DIST
            )) {
                return right;
            }
        }

        return desiredAngle;
    }

    public boolean isInsideSafeRect(
            double x,
            double y,
            double width,
            double height,
            double margin
    ) {
        return margin <= x
                && x <= width - margin
                && margin <= y
                && y <= height - margin;
    }

    private boolean isSafeAtOffset(
            double offset,
            double x,
            double y,
            double currentHeading,
            Arena arena,
            double margin,
            double lookAhead
    ) {
        double absoluteHeading = currentHeading + offset;

        double projectedX =
                x + lookAhead * Math.sin(absoluteHeading);

        double projectedY =
                y + lookAhead * Math.cos(absoluteHeading);

        return isInsideSafeRect(
                projectedX,
                projectedY,
                arena.getWidth(),
                arena.getHeight(),
                margin
        );
    }

    public void onPaint(Graphics2D g) {}
    public void onHitByBullet(HitByBulletEvent e) {}
}