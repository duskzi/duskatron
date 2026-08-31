package duskatron.gun.guns;

import duskatron.context.DuskatronContext;
import duskatron.enemy.Enemy;
import duskatron.gun.VirtualBullet;

import java.awt.*;

public abstract class Gun {

    DuskatronContext bot;
    public AimStatus aimstatus;

    public Gun(DuskatronContext ctx) { this.bot = ctx; this.aimstatus = new AimStatus(); }

    public abstract void updateAimStatus(Enemy e, double bulletPower);
    public abstract String getName();

    public void onVirtualBulletResult(VirtualBullet bullet, Enemy enemy, boolean hit) {}

    public void onPaint(Graphics2D g) {};
}
