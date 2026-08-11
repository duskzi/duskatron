package duskatron;/*
Equipe duskatron.Volbeat

Lucas de Souza Siqueira - lucas.s.siqueira.2009@gmail.com

2º DS (AMS) - ETEC Bento Quirino

*/

import robocode.*;
import robocode.util.Utils;

public class Volbeat extends AdvancedRobot {
    // Declara variáveis
	 int erros = 0;
    double potenciaTiro = 2;
    double direction = 1;
    double movimentosZigZag = 0;
    int modoJogo = 1;
    boolean inimigoDetectado = false;
    long tempoUltimaDeteccao = 0; // Usa long porque getTime() retorna um long, representando o tempo da simulação em ticks

    // Função que encontra quando falta para chegar a um tal angulo, usando o Utils, da biblioteca do Robocode
    public double encontrarRestante(double angulo, double heading) {
        return Utils.normalRelativeAngleDegrees(angulo - heading);
    }
    
    // Função que faz o robô evitar as paredes
    public void antiParede() {
		double heading = getHeading();
        
        // PRIMEIRO: Cobre os 4 cantos
        if (getX() > 740 && getY() > 540) { // Superior direito
            setTurnRight(encontrarRestante(225, heading));
            setAhead(150);
            return;
        } else if (getX() > 740 && getY() < 60) { // Inferior direito
            setTurnRight(encontrarRestante(315, heading));
            setAhead(150);
            return;
        } else if (getX() < 60 && getY() > 540) { // Superior esquerdo
            setTurnRight(encontrarRestante(135, heading));
            setAhead(150);
            return;
        } else if (getX() < 60 && getY() < 60) { // Inferior esquerdo
            setTurnRight(encontrarRestante(45, heading));
            setAhead(150);
            return;
        }
        // DEPOIS: Cobre as paredes individuais (CIMA, BAIXO, ESQUERDA, DIREITA)
        else if (getY() > 540) {
            setTurnRight(encontrarRestante(180, heading));
            setAhead(150);
            return;
        } else if (getY() < 60) {
            setTurnRight(encontrarRestante(0, heading));
            setAhead(150);
            return;
        } else if (getX() > 740) {
            setTurnRight(encontrarRestante(270, heading));
            setAhead(150);
            return;
        } else if (getX() < 60) {
            setTurnRight(encontrarRestante(90, heading));
            setAhead(150);
            return;
        }
    }
    

    // Função que roda assim que o batalha começa
	public void run() {
	    setAdjustRadarForGunTurn(true);    // Radar independente da arma
	    setAdjustRadarForRobotTurn(true);  // Radar independente do corpo
	
	    // Loop principal
	    while (true) {
			antiParede();
	        // Se não detectou inimigo há um tempo, gira o radar completo
	        if (!inimigoDetectado || (getTime() - tempoUltimaDeteccao) > 10) {
	            setTurnRadarRight(360);
	        }
	
	        // Movimento contínuo leve para evitar paralisações:
	        // só manda novo comando de avanço/virada quando o anterior ja terminou.
	        if (getDistanceRemaining() == 0 && getTurnRemaining() == 0) {
	            setAhead(100);
	            setTurnRight(10);
	        }
	        execute();
	    }
	}

    public void onScannedRobot(ScannedRobotEvent e) {
        inimigoDetectado = true; // Diz que já detectou um inimigo (para de girar o radar)
        tempoUltimaDeteccao = getTime(); // Atualiza o tempo da última da detecção
        
        double energia = getEnergy(); // Pega energia atual
        double energiaInimigo = e.getEnergy(); // Pega energia do inimigo
        double distance = e.getDistance(); // Pega distância do inimigo detectado
		
		        // Determinar potência baseada na situação tática
        if (distance < 50) {
            potenciaTiro = 3; // Sempre máximo quando muito perto
        } else if (energia < 20) {
            potenciaTiro = 1; // Conservar energia quando baixa
        } else if (energia > 50 && energiaInimigo < 30) {
            potenciaTiro = 3; // Aproveitar vantagem de energia
        } else if (distance < 150) {
            potenciaTiro = 2; // Potência média para distância média
        } else {
            potenciaTiro = 1; // Potência baixa para longas distâncias
        }
    
        // Mira preditiva

        /*
            Calcula onde o inimigo estará quando a bala chegar e, ao invés de atirar onde ele está agora, atira onde ele estará
            Usa:
                - velocidade do tiro;
                - X do inimigo;
                - Y do inimigo.
            para calcular o futuro X e Y

            Com isto, vira a arma para o futuro X e Y, usando:
                - futuro X;
                - futuro Y;
                - distancia até o futuro X e Y.

            Esse sistema utiliza MUITA geometria
        */ 

        double velocidadeTiro = 20 - 3 * potenciaTiro;
        double tempoAteInimigo = e.getDistance() / velocidadeTiro;
        double inimigoX = getX() + Math.sin(Math.toRadians(e.getBearing() + getHeading())) * distance;
        double inimigoY = getY() + Math.cos(Math.toRadians(e.getBearing() + getHeading())) * distance;
        double futuroX = inimigoX + Math.sin(Math.toRadians(e.getHeading())) * e.getVelocity() * tempoAteInimigo;
        double futuroY = inimigoY + Math.cos(Math.toRadians(e.getHeading())) * e.getVelocity() * tempoAteInimigo;
        double distanciaAteFuturoX = futuroX - getX();
        double distanciaAteFuturoY = futuroY - getY();
        double anguloFuturo = Math.toDegrees(Math.atan2(distanciaAteFuturoX, distanciaAteFuturoY));
        double anguloArma = normalizarAngulo(anguloFuturo - getGunHeading());
        setTurnGunRight(anguloArma);
        
        if(Math.abs(anguloArma) < 5){ // Só atira se já tiver virado a arma o suficiente
            setFireBullet(potenciaTiro);
        }
		
		 execute();
        
        // Sistemas de modos que são alterados dependendo da energia do robô e do inimigo

        /*
            MODO 1: Avança com zigue-zague normal
            MODO 2: Avança com zigue-zague mais direto
            MODO 3: Recuo defensivo com potência 2
            MODO 4: Recuo com zigue-zague e potência 1
        */

        if (energia > 50 && energiaInimigo > 50) {
            modoJogo = 1; // Avança com zigue-zague normal
        } else if (energia > 50 && energiaInimigo < 40) {
            modoJogo = 2; // Avança com zigue-zague mais direto
        } else if (energia < 50 && energia > 30 && energiaInimigo > 50) {
            modoJogo = 3; // Recua atirando potência 2
        } else if (energia < 20 && energiaInimigo > 40) {
            modoJogo = 4; // Recua com zigue-zague atirando potência 1
        }

        // Prender mira no adversário
        double radarTurn = getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians(); // Calcula quanto falta para chegar no inimigo
        radarTurn = Utils.normalRelativeAngle(radarTurn); // Vira esse restante
        setTurnRadarRightRadians(radarTurn * 2); // Aumenta o tamanho do radar, para não perder o adversário
        
		/*
			Como funciona o zigue-zague aleatório:
			  1º - pega um ângulo aleatório;
			  2º - avança/recua uma distância aleatória;
			  3º - escolhe uma quantidade de turnos aleatória que vai andar antes de virar uma vez;
			  4º - muda de direção depois de andar essa quantidade de turnos.
		*/

        if ((getX() > 740 || getX() < 60 || getY() > 540 || getY() < 60)) { // Se estiver muito perto das paredes...
            antiParede(); //...executa o anti-parede
        } else { // Senão, vê se precisa mudar de modo de jogo
            if (e.getDistance() > 80 && modoJogo == 1) {
                int angulosAleatorios = (int) (Math.random() * (70 - 45 + 1) + 45);
                double viradaFinal = e.getBearing() + (angulosAleatorios * direction);
                setTurnRight(viradaFinal);
                
				 if (getDistanceRemaining() == 0) {
                	int avancoAleatorio = (int) (Math.random() * (120 - 90 + 1) + 90);
                	setAhead(avancoAleatorio);
				 }
                
                movimentosZigZag++;
                int viradasAleatorias = (int) (Math.random() * (18 - 8 + 1) + 8);
                if (movimentosZigZag >= viradasAleatorias) {
                    direction *= -1;
                    movimentosZigZag = 0;
                }
				
            } else if (e.getDistance() > 80 && modoJogo == 2) {
                double viradaFinal = e.getBearing() + (45 * direction);
                setTurnRight(viradaFinal);
                
                if (getDistanceRemaining() == 0) {
				     setAhead(100);
				  }
                
                movimentosZigZag++;
                if (movimentosZigZag >= 10) {
                    direction *= -1;
                    movimentosZigZag = 0;
                }
            } else if (e.getDistance() > 80 && modoJogo == 3) {
                double viradaFinal = e.getBearing() + 180 + (30 * direction);
                setTurnRight(viradaFinal);
                
				 if (getDistanceRemaining() == 0) {
					setBack(75);
				 }
                
                movimentosZigZag++;
                if (movimentosZigZag >= 8) {
                    direction *= -1;
                    movimentosZigZag = 0;
                }
            } else if (e.getDistance() > 80 && modoJogo == 4) {
                double viradaFinal = e.getBearing() + (45 * direction);
                setTurnRight(viradaFinal);
                
                if (getDistanceRemaining() == 0) {
				 	setBack(100);
				 }
                
                movimentosZigZag++;
                if (movimentosZigZag >= 10) {
                    direction *= -1;
                    movimentosZigZag = 0;
                }
            } else { // Senão tiver que mudar de modo (porque está longe) só se vira para o inimigo e avança até ele
				 double ajuste = e.getDistance() - 30;
				 setTurnRight(e.getBearing());
				 if (ajuste > 0 && getDistanceRemaining() == 0) {
					setAhead(ajuste);
				 } else if (getDistanceRemaining() == 0) {
					setBack(-ajuste);
				 }
            }
        }
    }

    public void onHitWall(HitWallEvent e) { // Quando bate na parede, anda pra trás ao mesmo tempo vira para o inimigo
        setBack(100);
        setTurnRight(e.getBearing());
    }

    public void onHitRobot(HitRobotEvent e) { // Quando bate num inimigo, atira com potência 3
        potenciaTiro = 3;
        setFireBullet(potenciaTiro);
    }
    
    public void onWin(WinEvent e) { // Quando ganha, dá uma giradinha (comemoração)
        while(true) {
            turnRight(360);
        }
    }
	
	public void onBulletMissed(BulletMissedEvent e) {
		erros++;
	}
    
    // Função normalizadora de ângulo
    public double normalizarAngulo(double angulo) {  // Garante que o ângulo fique entre -180º e 180º, evitando viradas desnecessárias, por exemplo, se for mais rápido virar pela esquerda, garante que isso aconteça
        while (angulo > 180) angulo -= 360;
        while (angulo < -180) angulo += 360;
        return angulo;
    }
}