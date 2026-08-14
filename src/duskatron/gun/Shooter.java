package duskatron.gun;

import duskatron.enemy.Enemy;

public interface Shooter {

    double aimAngleFunction(Enemy e, double bulletPower);

    String getName();
}
