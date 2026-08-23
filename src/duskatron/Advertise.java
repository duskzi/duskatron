package duskatron;

/*
    |`'. |  | {_´´ |../  /\  "|" |  ) .''. |\ |   [_ `\=-='
    |_.' |..| '..} |  \ /  \  |  |  \ '..' | \|   (.....)
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

        > Script para merge:
            O projeto foi desenvolvido em
            torno de 21 classes.
            O torneio aceita apenas uma
            classe java nomeada
            <NomeDaEquipe>.java, então
            precisei unir as classes em
            apenas um arquivo, utilizando
            um script em python, merge.py.

        > Limpeza de código, como:
            Remover expressões redundantes.
            Facilitar a procura por erros.

        > Auxílio ao portar algoritmos, como:
            Circular Targeting.
            Ajuda na limpeza do código de
            wave surfing.
            Substituir Vec2D por Point2D para
            melhor compatibilidade.

    Todas referências e algoritmos vieram de:

    https://book.robocode.dev/
        > Visão geral sobre bots e física
        > Radar, virtual aim

    https://robowiki.net/wiki/Main_Page
        > Algoritmos avançados
        > Estratégias avançadas
        > Bots para testar contra
*/

/*
    All of my code is written in English, but I'll
    let the header in PT-BR cuz I'm not confident
    writing important stuff in English.

    Good classes to learn from:

        | Class                         | Description

        DuskatronContext.java           Holds all bot's parts
        Duskatron.java                  Main class
        Manager constants interface     All configs/consts
        All 3 managers                  Entire bot logic
        Enemy.java                      All info about enemy
*/

/*  Just empty to get header message at the top when merging  */
public interface Advertise { }