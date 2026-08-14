package duskatron.gun;

import duskatron.context.DuskatronContext;

import java.awt.*;

public abstract class Gun implements Shooter {

    DuskatronContext bot;

    public Gun(DuskatronContext ctx) { this.bot = ctx; }
    public void onPaint(Graphics2D g) {};
}
