package duskatron.context;

import duskatron.arena.Arena;
import duskatron.gun.Gun;
import duskatron.movement.Wheel;
import duskatron.radar.Radar;
import robocode.AdvancedRobot;

public class DuskatronContext {

    private Gun               gun;        /*  Gun part  */
    private Radar             radar;      /*  Radar part  */
    private Wheel             wheel;      /*  Wheel part  */
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
    public void bindParts(Radar radar, Gun gun, Wheel wheel, Arena arena) {
        this.radar = radar;
        this.gun = gun;
        this.wheel = wheel;
        this.arena = arena;
    }

    public AdvancedRobot robot()    { return robot; }
    public Gun gun()                { return gun; }
    public Radar radar()            { return radar; }
    public Wheel wheel()            { return wheel; }
    public Arena arena()            { return arena; }
}
