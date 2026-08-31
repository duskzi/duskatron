package duskatron.manager;

public interface ManagerConstants {

    /*
        Duskatron
    */
    double MAX_LIFE =               100;                    /*  Bot's maximum life  */

    /*
        Radar/Scan
    */
    double RADAR_OVERSHOOT =        Math.toRadians(15);     /*  How much radar will overshoot when scanning  */
    long   LOST_CONTACT_TIME =      20;                     /*  Maximum lost contact time (in turns btw?)  */

    /*
        Gun/Cannon
    */
    double ROBOT_RADIUS =           18.0;                    /*  Radius of a bot when testing virtual bullets  */
    double MISS_DISTANCE_MARGIN =   60.0;                    /*  Extra distance to the enemy when checking for collisions  */
    double VIRTUAL_AIM_DELAY =      8.0;                     /*  Delay between virtual firing  */
    double GUN_TURN_PRECISION =     Math.toRadians(4.0);     /*  Value of gun's precision offset before shooting   */

    /*
        Wheel/Movement
    */
    int NUMBER_OF_RECORDS =         16;                      /*  Number of last position records  */
    int TICKS_BETWEEN_RECORD =      2;
}
