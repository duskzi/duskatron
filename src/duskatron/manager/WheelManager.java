package duskatron.manager;

import duskatron.context.DuskatronContext;
import duskatron.math.Vec2D;
import duskatron.movement.MrmWheel;
import duskatron.movement.SurferWheel;
import duskatron.movement.Wheel;
import robocode.HitByBulletEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WheelManager implements ManagerConstants {

    private final List<Vec2D> pastPositions = new ArrayList<>();
    DuskatronContext bot;

    private final Wheel surfer;
    private final Wheel MRM;

    private Wheel wheel;

    public WheelManager(DuskatronContext ctx) {

        this.bot =      ctx;
        this.MRM =      new MrmWheel(ctx);
        this.surfer =   new SurferWheel(ctx);

        /*  Using MRM at first  */
        this.wheel = this.MRM;
    }

    public void handleMovement() {

        recordPositions();

        /*
            If there's only one bot, use wave surfing
            otherwise use minimum risk movement
        */
        if(bot.arena().is1v1())    { wheel = surfer; }

        /*  Actually use the movement strategy  */
        wheel.move();
    }

    /*  Records previous places that we passed  */
    public void recordPositions() {
        if(bot.robot().getTime() % TICKS_BETWEEN_RECORD == 0) {

            pastPositions.add(new Vec2D(bot.robot().getX(), bot.robot().getY()));
            if (pastPositions.size() > NUMBER_OF_RECORDS) { pastPositions.removeFirst(); }
        }
    }

    public List<Vec2D> getRecordedPositions() { return pastPositions; }

    public void onPaint(Graphics2D g)               { wheel.onPaint(g); }
    public void onHitByBullet(HitByBulletEvent e)   { wheel.onHitByBullet(e); }
}