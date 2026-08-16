package duskatron;

import duskatron.arena.Arena;
import duskatron.context.DuskatronContext;
import duskatron.manager.GunManager;
import duskatron.manager.WheelManager;
import duskatron.manager.RadarManager;
import robocode.*;
import java.awt.*;

/*
    |`'. |  | {_´´ |../  /\  "|" |  ) .''. |\ |   [_ `\=-='
    |_.' |..| .__} |  \ /  \  |  |  \ '..' | \|   (.....)
            a robocode bot by Dusk.

    EQUIPE:         DUSKATRON

    INTEGRANTE 1:   Felipe Kühl Pereira
    INTEGRANTE 2:   n/a
    INTEGRANTE 3:   n/a

    ESCLARECIMENTO SOBRE O USO DE IA:

    Durante o desenvolvimento do projeto
    foi utilizada IA (inteligência
    artificial) para as seguintes
    circunstâncias:

        > Limpeza de código, como:
            Remover expressões redundantes.
            Facilitar a procura por erros.

        > Auxílio ao portar algoritmos, como:
            Circular Targeting.
            Ajuda na limpeza do código de
            wave surfing.

    /!\  NENHUM CÓDIGO FOI PLÁGIADO, TODAS AS
    REFERÊNCIAS E ALGORITMOS VIERAM DE:

    https://book.robocode.dev/
        > Visão geral sobre bots e física
        > Radar, virtual aim

    https://robowiki.net/wiki/Main_Page
        > Algoritmos avançados
        > Estratégias avançadas
*/

/*
    All of my code is written in English, but I'll
    let the header in PT-BR cuz I'm not confident
    writing important info in English.

    Good classes to learn from:

        DuskatronContext.java
        Duskatron.java
        Manager constants interface
        All 3 managers
        Enemy.java
*/
public class Duskatron extends AdvancedRobot {

    /*
        Bot's context holds references to all
        duskatron managers listed below
    */
    DuskatronContext ctx = new DuskatronContext();

    /*
        Robo-parts
            Radar:      Manages find and storing enemy data
            Cannon:     Choose the best enemy and how to fire it
            Wheel:      Handles movement
    */
    public RadarManager radar =     new RadarManager(ctx);
    public GunManager gun =         new GunManager(ctx);
    public WheelManager wheel =     new WheelManager(ctx);
    /*
        Arena holds battlefield width and height
    */
    public Arena arena;

    @Override
    public void run() {

        arena = new Arena(
                ctx,
                this.getBattleFieldWidth(),
                this.getBattleFieldHeight());

        /*
            Pass all parts references to the
            context to be used later inside
            each part
        */
        ctx.bindParts(this, radar, gun, wheel, arena);

        /*
            Allows radar and gun to rotate
            independently for advanced
            scanning and shooting
        */
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        /*  Bot colors  */
        setRadarColor(Color.ORANGE);
        setBodyColor(Color.BLACK);
        setGunColor(Color.DARK_GRAY);

        /*  Debug  */
        System.out.println("Summary:");
        System.out.println("    Arena: (" +
                arena.getWidth() + " " +
                arena.getHeight() + ")");

        System.out.println("    Total Bots: " + getOthers());

        /*  Main loop  */
        for (;;) {

            radar.handleScanning();
            wheel.handleMovement();
            gun.handleGun();

            execute();
        }
    }

    /*  Components method calls  */
    public void onScannedRobot(ScannedRobotEvent e)     { radar.trackScannedBots(e); }
    public void onRobotDeath(RobotDeathEvent e)         { radar.removeEnemy(e.getName()); }
    public void onHitByBullet(HitByBulletEvent e)       { wheel.onHitByBullet(e); }

    /*  Larping after win  */
    public void onWin(WinEvent event) {

        for(;;) {

            setTurnRight(Double.POSITIVE_INFINITY);
            setTurnGunLeft(Double.POSITIVE_INFINITY);
            setAhead(6.7);

            float hue = (getTime() * 0.2f) % 1.0f;
            Color rainbowColor = Color.getHSBColor(hue, 1.0f, 4.0f);

            setBodyColor(rainbowColor);

            execute();

        }
    }

    public void onPaint(Graphics2D g) {
        radar.onPaint(g);
        if(getOthers() != 1) gun.onPaint(g);
        wheel.onPaint(g);
    }
}
