package duskatron.manager;

public interface ManagerConstants {

    /*
        RADAR/SCAN
    */
    double RADAR_OVERSHOOT =        Math.toRadians(15);     /*  How much radar will overshoot when scanning  */
    long   LOST_CONTACT_TIME =      20;                     /*  Maximum lost contact time (in turns?)  */

    /*
        GUN/CANNON
    */
    double ROBOT_RADIUS =          18.0;                    /*  Radius of a bot when testing virtual bullets  */
    double MISS_DISTANCE_MARGIN =  60.0;                    /*  Extra distance to the enemy when checking for collisions  */

    /*
        WHEEL/MOVEMENT
    */
    int NUMBER_OF_RECORDS =         16;                      /*  Number of last position records  */
    int TICKS_BETWEEN_RECORD =      2;
}
