package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Antigravity;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.util.Utils;

import java.util.ArrayList;
import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;
import static java.lang.Math.*;

public class MrmWheel extends Wheel implements Constants {

    public MrmWheel(DuskatronContext ctx) {
        super(ctx);
    }

    private static final double WALL_MARGIN = 30;      // keep this much clearance from any wall
    private static final double WALL_RISK_WEIGHT = 4e5; // scales wall danger relative to enemy danger
    private static final double ENEMY_RISK_WEIGHT = 4.0;

    public void handleMovement() {

        ArrayList<RiskPoint> points = new ArrayList<>();
        for (int p = 0; p < MRM_POINT_NUMBER; p++) {

            double x = bot.robot().getX() + sin((2.0 * PI) * ((double) p / MRM_POINT_NUMBER)) * MRM_DIST;
            double y = bot.robot().getY() + cos((2.0 * PI) * ((double) p / MRM_POINT_NUMBER)) * MRM_DIST;

            double risk = calcRisk(x, y, bot);

            points.add(new RiskPoint(new Vec2D(x, y), risk));
        }

        // Pick the lowest-risk candidate
        RiskPoint best = points.getFirst();
        for (RiskPoint rp : points) {
            if (rp.risk < best.risk) {
                best = rp;
            }
        }


        goTo(best.location);
    }

    private void goTo(Vec2D location) {
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

    private double calcRisk(double x, double y, DuskatronContext bot) {

        double risk = 0;

        for (Enemy enemy : bot.radar().getScannedBots().values()) {

            double dist = Math.hypot(x - enemy.getX(), y - enemy.getY());
            dist = Math.max(dist, 1.0); // avoid div-by-zero if a point lands on an enemy

            double enemyRisk = (enemy.getEnergy() + 1) / (dist * dist);
            risk += ENEMY_RISK_WEIGHT * enemyRisk;
        }

        double fieldW = bot.arena().getWidth();
        double fieldH = bot.arena().getHeight();

        double distToWall = Math.min(
                Math.min(x, fieldW - x),
                Math.min(y, fieldH - y)
        );

        if (distToWall < 0) {
            return Double.MAX_VALUE; // point is off the field entirely — never pick it
        }

        if (distToWall < WALL_MARGIN) {
            double closeness = (WALL_MARGIN - distToWall) / WALL_MARGIN; // 0..1
            risk += WALL_RISK_WEIGHT * closeness * closeness;
        }

        return risk;
    }

    public void optimalTurnAndGo(double angleToTurn, double ahead) {
        if (Math.abs(angleToTurn) > Math.PI / 2) {
            angleToTurn -= Math.signum(angleToTurn) * Math.PI;

            bot.robot().setTurnRightRadians(
                    normalizeAngle(angleToTurn)
            );
            bot.robot().setAhead(-ahead);
        } else {
            bot.robot().setTurnRightRadians(angleToTurn);
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
                x + lookAhead * sin(absoluteHeading);

        double projectedY =
                y + lookAhead * cos(absoluteHeading);

        return isInsideSafeRect(
                projectedX,
                projectedY,
                arena.getWidth(),
                arena.getHeight(),
                margin
        );
    }

    public record RiskPoint(Vec2D location, double risk) { }
}
