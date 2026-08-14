package duskatron;

public interface Constants {

    /*  Wall smoothing  */
    public static final double MARGIN =                 30.0;
    public static final double LOOK_AHEAD_DIST =        140.0;

    double HOF_ARRIVAL_THRESHOLD = 15;
    double HOF_WALL_MARGIN = 30;
    int HOF_SEARCH_ATTEMPTS = 200;
    double HOF_SEARCH_MIN_DIST = 100;
    double HOF_SEARCH_RANGE = 200;

    /* Minimum Risk Movement */

    public static final int MRM_POINT_COUNT = 32;

    /*
     * How far candidate destinations are sampled from the robot.
     *
     * 110 px gives the robot enough room to meaningfully change
     * direction without making every decision excessively far ahead.
     */
    public static final double MRM_DISTANCE = 100.0;

    /*
     * Desired distance from arena walls.
     */
    public static final double WALL_MARGIN = 40.0;

    /*
     * Risk priorities.
     *
     * These add up to 1.0, so they can be read as relative importance:
     *
     *   50% enemy danger
     *   20% being the closest target
     *   15% walls
     *   15% avoiding our own recent path
     */
    public static final double ENEMY_RISK_WEIGHT = 0.40;
    public static final double CLOSEST_BOT_RISK_WEIGHT = 0.30;
    public static final double WALL_RISK_WEIGHT = 0.15;
    public static final double TRAIL_RISK_WEIGHT = 0.15;

    /*
     * Distance at which an enemy's distance danger falls substantially.
     *
     * This is a physical distance, not a mysterious multiplier.
     */
    public static final double ENEMY_DISTANCE_SCALE = 140.0;

    /*
     * Positions farther than this from our recent path have
     * almost no trail penalty.
     */
    public static final double TRAIL_DISTANCE_SCALE = 80.0;

    /*
     * Number of historical positions considered.
     */
    public static final int TRAIL_LENGTH = 12;

    /*
     * Wall smoothing.
     */
    public static final double WALL_SMOOTH_ANGLE_STEP = 2.0;
    public static final int WALL_SMOOTH_MAX_STEPS = 90;

    /*  Antigravity and bullet gravity  */
    public static final double ENEMY_STRENGTH =         50.0;
    public static final double ENERGY_WEIGHT_MIN =      0.25;
    public static final double BULLET_STRENGTH =        1500.0;
    public static final double BULLET_RANGE =           250.0;
    public static final double BULLET_RADIAL_WEIGHT =   0.4;
    public static final double BULLET_PERP_WEIGHT =     1.0;

    /*  Number of last position records  */
    public static final int     NUMBER_OF_RECORDS =     32;
}
