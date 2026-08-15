package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Antigravity;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import java.util.Map;

import static duskatron.math.AngleUtil.normalizeAngle;

public class AntigravityWheel extends Wheel implements Constants {

    public AntigravityWheel(DuskatronContext ctx) {
        super(ctx);
    }

    public void move() {
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
}