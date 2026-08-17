package duskatron.context;

import duskatron.arena.Arena;
import duskatron.manager.GunManager;
import duskatron.manager.RadarManager;
import duskatron.manager.WheelManager;
import robocode.AdvancedRobot;

public class DuskatronContext {

    private GunManager        gun;        /*  Gun part  */
    private RadarManager      radar;      /*  Radar part  */
    private WheelManager      wheel;      /*  Wheel part  */
    private Arena             arena;      /*  Battlefield arena  */
    private AdvancedRobot     robot;      /*  Duskatron itself  */

    public DuskatronContext() {}

    /*
        Bind parts after instancing them to not fall into circular
        dependency, ex.:

        Bot need Gun in its constructor    Bot bot = new Bot(gun);
        Gun need Bot in its constructor    Gun gun = new Gun(bot);

        So after instancing Bot, we bind it passing to a method
        after initialization:

        Bot bot = new Bot(...);
        gun.bind(bot);
    */
    public void bindRobot(AdvancedRobot robot)  { this.robot = robot; }
    public void bindArena(Arena arena)          { this.arena = arena; }
    public void bindManagers(GunManager gun, WheelManager wheel, RadarManager radar) {
        this.gun = gun;
        this.wheel = wheel;
        this.radar = radar;
    }

    public AdvancedRobot robot()            { return robot; }
    public GunManager gun()                 { return gun; }
    public RadarManager radar()             { return radar; }
    public WheelManager wheel()             { return wheel; }
    public Arena arena()                    { return arena; }
}
