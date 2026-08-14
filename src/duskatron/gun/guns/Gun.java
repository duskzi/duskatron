package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;

import java.awt.*;

public abstract class Gun {

    DuskatronContext bot;

    public Gun(DuskatronContext ctx) { this.bot = ctx; }

    public abstract double aimAngleFunction(Enemy e, double bulletPower);
    public abstract String getName();

    public void onPaint(Graphics2D g) {};
}
