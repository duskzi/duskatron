package duskatron.arena;

import duskatron.context.DuskatronContext;
import duskatron.math.Vec2D;

public class Arena {

    private final double width;
    private final double height;
    private DuskatronContext bot;
    // private double margin;

    public Arena(DuskatronContext ctx, double w, double h){
        this.width = w;
        this.height = h;
        this.bot = ctx;
        // this.margin = margin;
    }

    /*  TODO: see if I really need margin here  */
    //public void setMargin(double margin) { this.margin = margin; }
    //public double getMargin() { return margin; }

    public double getWidth()    { return width; }
    public double getHeight()   { return height; }
    public boolean is1v1()      { return (bot.robot().getOthers() == 1); }
    public boolean isInsideArena(Vec2D pos) {
        return (0.0 < pos.x && pos.x < width) && (0.0 < pos.y && pos.y < height);
    }
}
