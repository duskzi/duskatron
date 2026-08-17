import robocode.*;
import robocode.util.Utils;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Random;

public class Glicerioxx extends AdvancedRobot {

    Random rng = new Random();

    // Dados do inimigo
    double enemyEnergy = 100;
    double enemyBearing;
    double enemyDistance;
    double enemyHeading;
    double enemyVelocity;
    String enemyName = "";

    // Controle de movimento
    double moveDirection = 1;
    double preferredDistance = 400;
    double lastEnemyVelocity = 0;

    // Controle de tiro
    double bulletPower = 2;
    double lastEnemyHeading = 0;

    public void run() {
        setBodyColor(Color.YELLOW);
        setGunColor(Color.ORANGE);
        setRadarColor(Color.RED);
        setScanColor(Color.WHITE);

        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        // Radar infinito
        turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        enemyName = e.getName();
        enemyBearing = e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyHeading = e.getHeadingRadians();
        enemyVelocity = e.getVelocity();

        // Posição absoluta do inimigo
        double absBearing = getHeadingRadians() + enemyBearing;
        double enemyX = getX() + enemyDistance * Math.sin(absBearing);
        double enemyY = getY() + enemyDistance * Math.cos(absBearing);

        // ---- Controle de distância ---
        if (enemyDistance < preferredDistance - 50) {
            moveDirection = -1; // afasta
        } else if (enemyDistance > preferredDistance + 50) {
            moveDirection = 1; // aproxima
        }

        // Movimento evasivo adaptativo
        if (Math.abs(enemyVelocity - lastEnemyVelocity) > 2) {
            moveDirection *= -1; // muda direção quando inimigo acelera/freia muito
        }
        lastEnemyVelocity = enemyVelocity;

        // Pequena variação aleatória para quebrar padrões
        double variation = (rng.nextDouble() - 0.5) * 40;
        setTurnRightRadians(Math.sin(absBearing) * moveDirection + variation / 120);
        setAhead(100 * moveDirection);

        // ---- Mira preditiva refinada ---
        bulletPower = Math.min(3.0, Math.max(1.5, 500 / enemyDistance));
        double bulletSpeed = 20 - 3 * bulletPower;
        double deltaTime = 0;
        double predictedX = enemyX;
        double predictedY = enemyY;

        while ((++deltaTime) * bulletSpeed < Point2D.distance(getX(), getY(), predictedX, predictedY)) {
            predictedX += Math.sin(enemyHeading) * enemyVelocity * 1.05;
            predictedY += Math.cos(enemyHeading) * enemyVelocity * 1.05;

            if (predictedX < 18.0 || predictedY < 18.0 ||
                    predictedX > getBattleFieldWidth() - 18.0 ||
                    predictedY > getBattleFieldHeight() - 18.0) {

                predictedX = Math.min(Math.max(18.0, predictedX), getBattleFieldWidth() - 18.0);
                predictedY = Math.min(Math.max(18.0, predictedY), getBattleFieldHeight() - 18.0);
                break;
            }
        }

        double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
        setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));

        // ---- Condição de disparo ---
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < Math.toRadians(4)) {
            // Evita desperdiçar tiros contra inimigos com energia baixa se eles forem morrer logo
            if (enemyEnergy > 0.4 || enemyDistance < 200) {
                setFire(bulletPower);
            }
        }

        // ---- Radar travado no inimigo ---
        double radarTurn = Utils.normalRelativeAngle(absBearing - getRadarHeadingRadians());
        double extraTurn = Math.min(Math.atan(36.0 / enemyDistance), Math.PI / 4);
        setTurnRadarRightRadians(radarTurn + (radarTurn < 0 ? -extraTurn : extraTurn));
    }

    public void onHitByBullet(HitByBulletEvent e) {
        moveDirection *= -1;
        setAhead(100 * moveDirection);
    }
}