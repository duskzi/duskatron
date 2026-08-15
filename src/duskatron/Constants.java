package duskatron;

/*
    +-----------------------------------------------+
    |    GLOBAL CONSTANTS                           |
    |    Change them, tune them, erase them         |
    |    They're all explained here, good luck!     |
    |                                               |
    +-----------------------------------------------+
*/
public interface Constants {

    /*  Wall smoothing  */
    double MARGIN =                     30.0;
    double LOOK_AHEAD_DIST =            140.0;

    /*
        Hawk On Fire movement
    */
    double HOF_ARRIVAL_THRESHOLD =      15;
    double HOF_WALL_MARGIN =            30;
    int HOF_SEARCH_ATTEMPTS =           200;
    double HOF_SEARCH_MIN_DIST =        100;
    double HOF_SEARCH_RANGE =           200;

    /*
        Minimum Risk Movement (MRM)
    */
    int MRM_POINT_COUNT =               32;         /*  How many points to use when sampling  */
    double MRM_DISTANCE =               100.0;      /*  How far risk points are sampled from the bot  */
    double WALL_MARGIN =                40.0;       /*  Desired distance from arena walls  */
    /*
        MRM wall smoothing
        Only applied if you use the 'SmoothHeading' method over 'goTo'
    */
    double WALL_SMOOTH_ANGLE_STEP =     2.0;
    int WALL_SMOOTH_MAX_STEPS =         90;

    /*
        Risk priorities

            60% enemy danger
            10% being the closest target
            15% walls
            15% avoiding our own recent path

        Yeah, I know it sucks being in floating
        decimals but make sense for me
    */
    double ENEMY_RISK_WEIGHT =          0.60;
    double CLOSEST_BOT_RISK_WEIGHT =    0.10;
    double WALL_RISK_WEIGHT =           0.15;
    double TRAIL_RISK_WEIGHT =          0.15;

    /*
        Distance at which an enemy's distance danger start to grow,
        this is a physical distance, not a mysterious multiplier
    */
    double ENEMY_DISTANCE_SCALE =       140.0;
    double TRAIL_DISTANCE_SCALE =       80.0;       /*  Positions farther than this have almost no trail penalty  */
    int TRAIL_LENGTH =                  12;         /* Number of historical positions to be considered  */


    /*
        Antigravity Wheel
    */
    double ENEMY_STRENGTH =         50.0;
    double ENERGY_WEIGHT_MIN =      0.25;
}
