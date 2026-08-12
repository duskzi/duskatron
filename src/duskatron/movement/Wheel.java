package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Antigravity;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;

import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class Wheel implements Constants {

    protected final DuskatronContext bot;

    public Wheel(DuskatronContext ctx) {
        this.bot = ctx;
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
}