package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;

import java.awt.*;

public abstract class Gun {

    DuskatronContext bot;
    public AimStatus aimstatus;

    public Gun(DuskatronContext ctx) { this.bot = ctx; this.aimstatus = new AimStatus(); }

    public abstract void updateAimStatus(Enemy e, double bulletPower);
    public abstract String getName();

    public void onPaint(Graphics2D g) {};
}
