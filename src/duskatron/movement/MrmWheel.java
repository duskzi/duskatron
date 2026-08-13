package duskatron.movement;

import duskatron.Constants;
import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.util.Utils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class MrmWheel extends Wheel implements Constants {

    private final ArrayList<RiskPoint> currentRiskPoints = new ArrayList<>();

    public MrmWheel(DuskatronContext ctx) {
        super(ctx);
    }

    private double trembleDirection = 1.0;
    private long nextTrembleTick = 0;

    private static final double TREMBLE_ANGLE = Math.toRadians(12.0);

    private static final long TREMBLE_DURATION = 6;

    public void goTo(Vec2D destination) {
        double x = bot.robot().getX();
        double y = bot.robot().getY();

        double dx = destination.x - x;
        double dy = destination.y - y;

        if (dx == 0.0 && dy == 0.0) {
            return;
        }

        /*
         * Your coordinate system:
         *   0 = north
         *   PI/2 = east
         */
        double desiredAngle =
                Math.atan2(dx, dy);

        /*
         * Change wobble direction periodically.
         */
        long time = bot.robot().getTime();

        if (time >= nextTrembleTick) {
            trembleDirection = -trembleDirection;
            nextTrembleTick = time + TREMBLE_DURATION;
        }

        /*
         * Slightly rotate the target direction left/right.
         */
        double trembledAngle =
                desiredAngle
                        + trembleDirection * TREMBLE_ANGLE;

        /*
         * Use your normal shortest-path GoTo logic.
         */
        double turn =
                Utils.normalRelativeAngle(
                        trembledAngle
                                - bot.robot().getHeadingRadians()
                );

        double distance =
                Math.hypot(dx, dy);

        if (Math.abs(turn) > Math.PI / 2) {
            turn = Utils.normalRelativeAngle(turn + Math.PI);
            distance = -distance;
        }

        bot.robot().setTurnRightRadians(turn);
        bot.robot().setAhead(distance);
    }

    public void handleMovement() {
        ArrayList<Enemy> enemies = new ArrayList<>(
                bot.radar().getScannedBots().values()
        );

        ArrayList<RiskPoint> points = new ArrayList<>(MRM_POINT_COUNT);

        double robotX = bot.robot().getX();
        double robotY = bot.robot().getY();

        for (int i = 0; i < MRM_POINT_COUNT; i++) {
            /*
             * Robocode uses:
             *   0 radians = north
             *   PI / 2   = east
             *
             * Therefore:
             *   x += sin(angle)
             *   y += cos(angle)
             */
            double angle = (PI * 2.0) * i / MRM_POINT_COUNT;

            Vec2D destination = new Vec2D(
                    robotX + sin(angle) * MRM_DISTANCE,
                    robotY + cos(angle) * MRM_DISTANCE
            );

            points.add(
                    evaluateRisk(
                            enemies,
                            destination,
                            angle
                    )
            );
        }

        currentRiskPoints.clear();
        currentRiskPoints.addAll(points);

        RiskPoint best = points.get(0);

        for (RiskPoint point : points) {
            if (point.totalRisk() < best.totalRisk()) {
                best = point;
            }
        }

        goTo(best.location());
    }

    /*
     * ------------------------------------------------------------------------
     * RISK EVALUATION
     * ------------------------------------------------------------------------
     *
     * Every component is normalized to approximately [0, 1].
     *
     * 0.0 = desirable
     * 1.0 = dangerous
     *
     * The final weights therefore have an intuitive meaning.
     */

    private RiskPoint evaluateRisk(
            List<Enemy> enemies,
            Vec2D destination,
            double movementAngle
    ) {
        double enemyRisk = getEnemyRisk(
                enemies,
                destination,
                movementAngle
        );

        double closestBotRisk = getClosestBotRisk(
                enemies,
                destination
        );

        double wallRisk = getWallRisk(
                destination
        );

        double trailRisk = getTrailRisk(
                destination
        );

        double totalRisk =
                ENEMY_RISK_WEIGHT * enemyRisk
                        + CLOSEST_BOT_RISK_WEIGHT * closestBotRisk
                        + WALL_RISK_WEIGHT * wallRisk
                        + TRAIL_RISK_WEIGHT * trailRisk;

        return new RiskPoint(
                destination,
                enemyRisk,
                closestBotRisk,
                wallRisk,
                trailRisk,
                totalRisk
        );
    }

    /**
     * Enemy risk combines:
     *
     * - distance
     * - perpendicularity
     * - enemy energy
     *
     * Individual enemy risks are combined into a bounded [0, 1] value.
     */
    private double getEnemyRisk(
            List<Enemy> enemies,
            Vec2D destination,
            double movementAngle
    ) {
        if (enemies.isEmpty()) {
            return 0.0;
        }

        double safeProbability = 1.0;

        double myEnergy = max(bot.robot().getEnergy(), 1.0);

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            double distance =
                    sqrt(destination.distanceSq(enemy.getPosition()));

            /*
             * 0 when very far away.
             * Approaches 1 when very close.
             */
            double distanceRisk =
                    exp(-distance / ENEMY_DISTANCE_SCALE);

            /*
             * Convert enemy bearing into an absolute battlefield angle.
             */
            double enemyAbsoluteBearing =
                    bot.robot().getHeadingRadians()
                            + enemy.getBearingRadians();

            /*
             * Difference between our movement direction
             * and the direction toward the enemy.
             */
            double angleDifference =
                    Utils.normalRelativeAngle(
                            movementAngle - enemyAbsoluteBearing
                    );

            /*
             * 1.0 = moving directly toward/away from enemy
             * 0.0 = perfectly perpendicular
             *
             * Diamond's melee movement explicitly favors
             * perpendicular movement.
             */
            double perpendicularRisk =
                    abs(cos(angleDifference));

            /*
             * Higher-energy enemies are somewhat more important.
             *
             * Relative energy is used instead of an arbitrary 4x multiplier.
             */
            double energyRatio =
                    enemy.getEnergy() / myEnergy;

            double energyRisk =
                    Math.clamp(
                            0.5 + 0.5 * energyRatio,
                            0.5,
                            1.5
                    );

            /*
             * Combine the three factors.
             */
            double individualRisk =
                    distanceRisk
                            * (0.5 + 0.5 * perpendicularRisk)
                            * energyRisk;

            individualRisk =
                    Math.clamp(
                            individualRisk,
                            0.0,
                            1.0
                    );

            /*
             * Treat each enemy as another source of danger.
             *
             * This combines multiple enemies without allowing
             * the result to exceed 1.0.
             */
            safeProbability *= (1.0 - individualRisk);
        }

        return 1.0 - safeProbability;
    }

    /**
     * Penalizes destinations where we would be the closest bot
     * to another enemy.
     *
     * In a true 1v1 this becomes constant, which is correct:
     * there is no alternative bot that can be closer.
     */
    private double getClosestBotRisk(
            List<Enemy> enemies,
            Vec2D destination
    ) {
        if (enemies.size() < 2) {
            return 0.0;
        }

        int validEnemies = 0;
        int threatenedEnemies = 0;

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            validEnemies++;

            double ourDistanceSq =
                    destination.distanceSq(enemy.getPosition());

            boolean weAreClosest = true;

            for (Enemy other : enemies) {
                if (!other.exists() || other == enemy) {
                    continue;
                }

                double otherDistanceSq =
                        other.getPosition()
                                .distanceSq(enemy.getPosition());

                if (otherDistanceSq < ourDistanceSq) {
                    weAreClosest = false;
                    break;
                }
            }

            if (weAreClosest) {
                threatenedEnemies++;
            }
        }

        if (validEnemies == 0) {
            return 0.0;
        }

        return (double) threatenedEnemies / validEnemies;
    }

    /**
     * Wall risk:
     *
     * 0 = comfortably inside the arena
     * 1 = touching/crossing the desired wall margin
     */
    private double getWallRisk(Vec2D position) {
        double fieldWidth = bot.arena().getWidth();
        double fieldHeight = bot.arena().getHeight();

        double distanceToWall = min(
                min(
                        position.x,
                        fieldWidth - position.x
                ),
                min(
                        position.y,
                        fieldHeight - position.y
                )
        );

        /*
         * Candidate is outside the arena.
         * Such a point should never be selected.
         */
        if (distanceToWall <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }

        /*
         * No wall danger once we are safely beyond the margin.
         */
        if (distanceToWall >= WALL_MARGIN) {
            return 0.0;
        }

        /*
         * Quadratic increase as we approach the wall.
         */
        double closeness =
                (WALL_MARGIN - distanceToWall)
                        / WALL_MARGIN;

        return closeness * closeness;
    }

    /**
     * Avoid recently visited positions.
     *
     * More recent positions receive more weight.
     */
    private double getTrailRisk(Vec2D destination) {
        List<Vec2D> history =
                bot.wheel().getRecordedPositions();

        if (history.isEmpty()) {
            return 0.0;
        }

        int count =
                min(history.size(), TRAIL_LENGTH);

        double weightedRisk = 0.0;
        double totalWeight = 0.0;

        for (int i = 0; i < count; i++) {
            Vec2D oldPosition =
                    history.get(history.size() - 1 - i);

            double distance =
                    sqrt(
                            destination.distanceSq(oldPosition)
                    );

            /*
             * Recent positions matter more.
             */
            double recency =
                    (double) (count - i) / count;

            /*
             * 1 near the trail,
             * approaches 0 as distance increases.
             */
            double distanceRisk =
                    exp(-distance / TRAIL_DISTANCE_SCALE);

            weightedRisk +=
                    recency * distanceRisk;

            totalWeight += recency;
        }

        if (totalWeight == 0.0) {
            return 0.0;
        }

        return weightedRisk / totalWeight;
    }

    /*
     * ------------------------------------------------------------------------
     * DEBUG VISUALIZATION
     * ------------------------------------------------------------------------
     */

    public void onPaint(Graphics2D g) {
        if (currentRiskPoints.isEmpty()) {
            return;
        }

        int radius = 6;

        for (RiskPoint point : currentRiskPoints) {
            double risk = point.totalRisk();

            /*
             * Infinity means outside the arena.
             */
            double normalized;

            if (!Double.isFinite(risk)) {
                normalized = 1.0;
            } else {
                normalized = Math.clamp(
                        risk,
                        0.0,
                        1.0
                );
            }

            int red =
                    (int) (255 * normalized);

            int blue =
                    (int) (255 * (1.0 - normalized));

            g.setColor(
                    new Color(
                            red,
                            0,
                            blue,
                            190
                    )
            );

            int x =
                    (int) point.location().x - radius;

            int y =
                    (int) point.location().y - radius;

            g.fillOval(
                    x,
                    y,
                    radius * 2,
                    radius * 2
            );
        }

        /*
         * Draw the MRM search circle.
         */
        g.setColor(
                new Color(
                        255,
                        255,
                        255,
                        100
                )
        );

        int radiusAroundRobot =
                (int) MRM_DISTANCE;

        int robotX =
                (int) bot.robot().getX();

        int robotY =
                (int) bot.robot().getY();

        g.drawOval(
                robotX - radiusAroundRobot,
                robotY - radiusAroundRobot,
                radiusAroundRobot * 2,
                radiusAroundRobot * 2
        );

        /*
         * Highlight the best point.
         */
        RiskPoint best =
                currentRiskPoints.get(0);

        for (RiskPoint point : currentRiskPoints) {
            if (point.totalRisk() < best.totalRisk()) {
                best = point;
            }
        }

        g.setColor(Color.GREEN);

        g.fillOval(
                (int) best.location().x - 8,
                (int) best.location().y - 8,
                16,
                16
        );

        g.drawLine(
                robotX,
                robotY,
                (int) best.location().x,
                (int) best.location().y
        );
    }

    /*
     * ------------------------------------------------------------------------
     * WALL SMOOTHING
     * ------------------------------------------------------------------------
     */

    public double smoothHeading(
            double desiredAngle,
            double x,
            double y,
            Arena arena,
            double margin
    ) {
        final double angleStep =
                toRadians(WALL_SMOOTH_ANGLE_STEP);

        double heading =
                bot.robot().getHeadingRadians();

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

        for (int i = 1; i <= WALL_SMOOTH_MAX_STEPS; i++) {
            double offset =
                    angleStep * i;

            double left =
                    desiredAngle - offset;

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

            double right =
                    desiredAngle + offset;

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

    private boolean isSafeAtOffset(
            double offset,
            double x,
            double y,
            double currentHeading,
            Arena arena,
            double margin,
            double lookAhead
    ) {
        double absoluteHeading =
                currentHeading + offset;

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

    public record RiskPoint(
            Vec2D location,
            double enemyRisk,
            double closestBotRisk,
            double wallRisk,
            double trailRisk,
            double totalRisk
    ) {
    }
}