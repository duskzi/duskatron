package duskatron.gun;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.manager.ManagerConstants;

/*
    Firepower selection

    AI ASSISTED

    Grounded in the community-standard ideas documented on RoboWiki's
    "Selecting Fire Power" page, see: robowiki.net/wiki/Selecting_Fire_Power

      * In 1v1, many strong bots settle around ~1.9-2.0 power as a
        general sweet spot, rather than always maxing out

      * In melee, many bots default toward max power (3.0), since more
        targets and more crossfire raise the odds of a hit landing

      * Power should taper up at very close range (near-guaranteed
        hits) and down at long range (low hit probability makes full
        power wasteful)

      * Power should be reduced as the bot's own energy drops, not
        only hard-capped as a last resort

      * Power should never exceed what's actually needed to finish the
        enemy off - computed from Robocode's real piecewise damage
        formula (4p + 2*max(0, p-1)), not a flat /4 approximation

    Note: the exact numeric thresholds below (distance, energy) are
    NOT reverse-engineered from any specific bot's source that isn't
    publicly available, they're reasonable values built around the
    rules above.
*/
public class GunUtils implements ManagerConstants {

    public static double getBestPower(DuskatronContext ctx, Enemy e) {
        double myEnergy = ctx.robot().getEnergy();

        if (myEnergy < 0.2) { return 0.0; }

        double distance = e.getDistance();
        double power = !ctx.arena().is1v1()
                ? getMeleePower(ctx, distance)
                : getDuelPower(distance);

        /*
            Self-preservation: taper power down as our own energy gets
            low, instead of only clamping it at the very end
        */
        power *= getEnergyConservationFactor(myEnergy);

        /*  Never spend more power than needed to finish the enemy  */
        power = Math.min(power, getKillPower(e.getEnergy()));

        double maxSafePower = Math.clamp(myEnergy - 0.1, 0.1, 3.0);
        return Math.clamp(power, 0.1, maxSafePower);
    }

    /*  Melee: default near max power - common melee baseline per
        RoboWiki - scaled slightly by range and by how many enemies
        are currently in view, since a crowded field raises the odds
        that *something* eats the bullet even off-target  */
    private static double getMeleePower(DuskatronContext ctx, double distance) {

        double power = distance < 200 ? 3.0 : (distance < 400 ? 2.5 : 1.5);

        int enemyCount = ctx.radar().getScannedBots().size();

        if (enemyCount >= 4) {
            power += 1.2;
        } else if (enemyCount >= 3) {
            power += 0.5;
        }

        return Math.min(power, 3.0);
    }

    /*  1v1: settle around the documented ~1.9-2.0 sweet spot instead
        of always maxing out, scaling up at close range (near-guaranteed hit)
        and down at long range (low hit chance makes
        full power wasteful)  */
    private static double getDuelPower(double distance) {

        if (distance < 150) return 3.0;   // point-blank, near-guaranteed hit
        if (distance < 300) return 2.0;
        if (distance < 500) return 1.9;   // general duel sweet spot
        return 1.1;
    }

    /*  Reduce power gradually as our own energy drops, rather than
        only clamping it at the very last moment  */
    private static double getEnergyConservationFactor(double myEnergy) {

        if (myEnergy > 30) return 1.0;
        if (myEnergy > 15) return 0.8;
        if (myEnergy > 5)  return 0.6;

        return 0.4;
    }

    /*  Minimum power that would finish the enemy off, using Robocode's
        real piecewise damage formula: damage = 4p + 2*max(0, p-1)
        Inverted:
          E <= 4  ->  p = E / 4
          E > 4   ->  p = (E + 2) / 6                                  */
    private static double getKillPower(double enemyEnergy) {

        if (enemyEnergy <= 0.0) return 0.1;

        double power = enemyEnergy <= 4.0
                ? enemyEnergy / 4.0
                : (enemyEnergy + 2.0) / 6.0;

        return Math.clamp(power, 0.1, 3.0);
    }

    public static double getBulletSpeed(double power) { return 20.0 - (3.0 * power); }
}