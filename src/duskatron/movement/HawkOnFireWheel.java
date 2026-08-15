package duskatron.movement;

import duskatron.Constants;
import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.math.Vec2D;
import robocode.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

/*
    HawkOnFireWheel

    Based on HawkOnFire rozu's movement adapted to Duskatron wheel
    architecture (see: https://robowiki.net/wiki/HawkOnFire/)

    Movement samples random points within a search radius, scores each
    with a cheap antigravity evaluation.
 */
public class HawkOnFireWheel extends Wheel implements Constants {

    private Vec2D nextDestination;
    private Vec2D lastDestination;

    public HawkOnFireWheel(DuskatronContext ctx) {
        super(ctx);
    }

    public void move() {

        Vec2D myPos = new Vec2D(bot.robot().getX(), bot.robot().getY());

        if (nextDestination == null) {
            nextDestination = myPos;
            lastDestination = myPos;
        }


        List<Enemy> enemies = new ArrayList<>(bot.radar().getScannedBots().values());
        Enemy closest = bot.radar().getClosestEnemy();

        /*  Nothing scanned, skip until find someone  */
        if (closest == null) {
            return;
        }

        double distanceToTarget = sqrt(myPos.distanceSq(closest.getPosition()));
        double distanceToDestination = sqrt(myPos.distanceSq(nextDestination));

        if (distanceToDestination < HOF_ARRIVAL_THRESHOLD) {
            nextDestination = pickDestination(enemies, myPos, distanceToTarget);
            lastDestination = myPos;
            distanceToDestination = sqrt(myPos.distanceSq(nextDestination));
        }

        moveTowards(myPos, distanceToDestination);
    }

    /*
     * Samples random points around the current position and keeps the
     * lowest-scoring one that lands inside the arena margin.
     */
    private Vec2D pickDestination(
            List<Enemy> enemies,
            Vec2D myPos,
            double distanceToTarget
    ) {
        double myEnergy = bot.robot().getEnergy();
        double margin = HOF_WALL_MARGIN;

        double minX = margin;
        double minY = margin;
        double maxX = bot.arena().getWidth() - margin;
        double maxY = bot.arena().getHeight() - margin;

        int liveEnemies = 0;
        for (Enemy enemy : enemies) {
            if (enemy.exists()) {
                liveEnemies++;
            }
        }

        /*  Fancy math that I don't understand  */
        double addLast = 1 - rint(pow(Math.random(), max(liveEnemies, 1)));

        Vec2D best = nextDestination;
        double bestScore = score(best, enemies, myPos, myEnergy, addLast);

        for (int i = 0; i < HOF_SEARCH_ATTEMPTS; i++) {
            double searchDist = min(
                    distanceToTarget * 0.8,
                    HOF_SEARCH_MIN_DIST + HOF_SEARCH_RANGE * Math.random()
            );
            double angle = 2 * PI * Math.random();

            Vec2D candidate = new Vec2D(
                    myPos.x + searchDist * sin(angle),
                    myPos.y + searchDist * cos(angle)
            );

            if (candidate.x < minX || candidate.x > maxX
                    || candidate.y < minY || candidate.y > maxY) {
                continue;
            }

            double candidateScore = score(candidate, enemies, myPos, myEnergy, addLast);

            if (candidateScore < bestScore) {
                best = candidate;
                bestScore = candidateScore;
            }
        }

        return best;
    }

    /*
        Lower is better, antigravity from each enemy, weighted by how
        dangerous it is and how angled the point is relative to that
        enemy, plus a term that rewards distance from the past recorded
        positions
    */
    private double score(
            Vec2D p,
            List<Enemy> enemies,
            Vec2D myPos,
            double myEnergy,
            double addLast
    ) {
        double s = addLast * 0.08 / p.distanceSq(lastDestination);

        for (Enemy enemy : enemies) {
            if (!enemy.exists()) {
                continue;
            }

            double energyRatio = min(enemy.getEnergy() / myEnergy, 2.0);

            double angleToMe = atan2(myPos.x - p.x, myPos.y - p.y);
            double angleToEnemy = atan2(enemy.getPosition().x - p.x, enemy.getPosition().y - p.y);
            double orbitalPenalty = 1 + abs(cos(angleToMe - angleToEnemy));

            s += energyRatio * orbitalPenalty / p.distanceSq(enemy.getPosition());
        }

        return s;
    }

    /*
        Turns and moves toward the destination, flipping into reverse when
        the target is behind us so we never need more than a 90-degree turn.
        This is what keeps HawkOnFire from wasting ticks spinning in place.
     */
    private void moveTowards(Vec2D myPos, double distanceToDestination) {
        double heading = bot.robot().getHeadingRadians();
        double angle = atan2(nextDestination.x - myPos.x, nextDestination.y - myPos.y) - heading;
        double direction = 1;

        if (cos(angle) < 0) {
            angle += PI;
            direction = -1;
        }

        angle = Utils.normalRelativeAngle(angle);

        bot.robot().setAhead(distanceToDestination * direction);
        bot.robot().setTurnRightRadians(angle);
        bot.robot().setMaxVelocity(abs(angle) > 1 ? 0 : 8.0);
    }
}