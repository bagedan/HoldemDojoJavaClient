/*
 * Copyright (C) 2016 Jellen Vermeir
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package poki;

import poki.handranking.Card;
import poki.handranking.util.HandFactory;
import poki.handranking.util.HandRanker;
import poki.handranking.util.HandRankingException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

//import org.rosuda.JRI.Rengine;

/**
 * @author Jellen Vermeir
 */
public class PokerGame {
    
    public PokerGame() {
        // Todo: do something useful..
    }
    
    public static void main(String[] args) throws HandRankingException{

        calculateFlopEHSRefactor();

        calculateTurnEHSRefactor();

        calculateRiverEHSRefactor();

    }

    private static void calculateTurnEHS() throws HandRankingException {
        Deck deck = new Deck();

        // starting hand
        Card ace_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_ACE);
        Card queen_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_QUEEN);

        // flop
        Card three_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_3);
        Card four_spades = new Card(Card.SUIT_SPADES, Card.RANK_4);
        Card jack_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_JACK);

        //turn
        Card three_spades = new Card(Card.SUIT_SPADES, Card.RANK_3);

        HashSet<Card> flop = new HashSet<>();
        flop.add(three_hearts);
        flop.add(four_spades);
        flop.add(jack_hearts);


        deck.removeCard(ace_hearts); deck.removeCard(queen_hearts);
        deck.removeCard(three_hearts); deck.removeCard(four_spades);
        deck.removeCard(jack_hearts); deck.removeCard(three_spades);

        List<Card> remainingCards = deck.getCards();
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();

        GameState gameState = new GameState();
        gameState.setFlop(flop);
        gameState.setTurn(three_spades);
        Player player = new Player(ace_hearts, queen_hearts, gameState);

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());
//        player.calculateHandStrength(weightArray, 2, remainingCards);
        System.out.println(Instant.now());

        player.calculateHandPotential(weightArray, 2, false, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time: " + (end-start));


        System.out.println("HandStrength: " + player.getHandStrength());
        System.out.println("Positive Potential: " + player.getPositiveHandPotential());
        System.out.println("Negative Potential: " + player.getNegativeHandPotential());

        System.out.println("Handranker new Starting Hands: " + HandFactory.rankableHandCounter);

        /***************************************************************************
         * *************************************************************************
         * *************************************************************************
         */
    }

    private static void calculatePreFlopEHS() throws HandRankingException {
        Deck deck = new Deck();

        // starting hand
        Card ace_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_ACE);
        Card queen_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_QUEEN);

        // flop
        Card three_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_3);
        Card four_spades = new Card(Card.SUIT_SPADES, Card.RANK_4);
        Card jack_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_JACK);

        HashSet<Card> flop = new HashSet<>();
        flop.add(three_hearts);
        flop.add(four_spades);
        flop.add(jack_hearts);


        deck.removeCard(ace_hearts); deck.removeCard(queen_hearts);
        deck.removeCard(three_hearts); deck.removeCard(four_spades);
        deck.removeCard(jack_hearts);

        List<Card> remainingCards = deck.getCards();
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();

        GameState gameState = new GameState();
        gameState.setFlop(flop);
        Player player = new Player(ace_hearts, queen_hearts, gameState);

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());
//        player.calculateHandStrength(weightArray, 2, remainingCards);
        System.out.println(Instant.now());

        player.calculateHandPotential(weightArray, 2, true, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time: " + (end-start));


        System.out.println("HandStrength: " + player.getHandStrength());
        System.out.println("Positive Potential: " + player.getPositiveHandPotential());
        System.out.println("Negative Potential: " + player.getNegativeHandPotential());

        System.out.println("Handranker new Starting Hands: " + HandFactory.rankableHandCounter);

        /***************************************************************************
         * *************************************************************************
         * *************************************************************************
         */
    }

    private static void calculateTurnEHSRefactor() throws HandRankingException {
        Deck deck = new Deck();

        // starting hand
        Card ace_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_ACE);
        Card queen_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_QUEEN);

        // flop
        Card three_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_3);
        Card four_spades = new Card(Card.SUIT_SPADES, Card.RANK_4);
        Card jack_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_JACK);

        //turn
        Card three_spades = new Card(Card.SUIT_SPADES, Card.RANK_3);

        HashSet<Card> flop = new HashSet<>();
        flop.add(three_hearts);
        flop.add(four_spades);
        flop.add(jack_hearts);


        deck.removeCard(ace_hearts); deck.removeCard(queen_hearts);
        deck.removeCard(three_hearts); deck.removeCard(four_spades);
        deck.removeCard(jack_hearts); deck.removeCard(three_spades);

        List<Card> remainingCards = deck.getCards();
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();

        GameState gameState = new GameState();
        gameState.setFlop(flop);
        gameState.setTurn(three_spades);
        PlayerLoki player = new PlayerLoki(ace_hearts, queen_hearts, gameState);

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());

        player.calculateHandStatistics(weightArray, 2, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time for turn ehs calculation: " + (end-start));


        System.out.println("HandStrength: " + player.getHandStrength());
        System.out.println("Positive Potential: " + player.getPositiveHandPotential());
        System.out.println("Negative Potential: " + player.getNegativeHandPotential());

        System.out.println("Handranker new Starting Hands: " + HandFactory.rankableHandCounter);

        /***************************************************************************
         * *************************************************************************
         * *************************************************************************
         */
    }

    private static void calculateFlopEHSRefactor() throws HandRankingException {
        Deck deck = new Deck();

        // starting hand
        Card ace_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_ACE);
        Card queen_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_QUEEN);

        // flop
        Card three_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_3);
        Card four_spades = new Card(Card.SUIT_SPADES, Card.RANK_4);
        Card jack_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_JACK);

        HashSet<Card> flop = new HashSet<>();
        flop.add(three_hearts);
        flop.add(four_spades);
        flop.add(jack_hearts);


        deck.removeCard(ace_hearts); deck.removeCard(queen_hearts);
        deck.removeCard(three_hearts); deck.removeCard(four_spades);
        deck.removeCard(jack_hearts);

        List<Card> remainingCards = deck.getCards();
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();

        GameState gameState = new GameState();
        gameState.setFlop(flop);
        PlayerLoki player = new PlayerLoki(ace_hearts, queen_hearts, gameState);

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());

        player.calculateHandStatistics(weightArray, 2, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time for flop ehs calculation: " + (end-start));


        System.out.println("HandStrength: " + player.getHandStrength());
        System.out.println("Positive Potential: " + player.getPositiveHandPotential());
        System.out.println("Negative Potential: " + player.getNegativeHandPotential());

        System.out.println("Handranker new Starting Hands: " + HandFactory.rankableHandCounter);

        /***************************************************************************
         * *************************************************************************
         * *************************************************************************
         */
    }

    private static void calculateRiverEHSRefactor() throws HandRankingException {
        Deck deck = new Deck();

        // starting hand
        Card ace_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_ACE);
        Card ace_diamonds = new Card(Card.SUIT_DIAMONDS, Card.RANK_ACE);

        // flop
        Card three_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_3);
        Card four_spades = new Card(Card.SUIT_SPADES, Card.RANK_4);
        Card jack_hearts = new Card(Card.SUIT_HEARTS, Card.RANK_JACK);

        //turn
        Card three_spades = new Card(Card.SUIT_SPADES, Card.RANK_3);

        //river
        Card ace_spades = new Card(Card.SUIT_SPADES, Card.RANK_ACE);

        HashSet<Card> flop = new HashSet<>();
        flop.add(three_hearts);
        flop.add(four_spades);
        flop.add(jack_hearts);


        deck.removeCard(ace_hearts); deck.removeCard(ace_diamonds);
        deck.removeCard(three_hearts); deck.removeCard(four_spades);
        deck.removeCard(jack_hearts); deck.removeCard(three_spades);
        deck.removeCard(ace_spades);

        List<Card> remainingCards = deck.getCards();
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();

        GameState gameState = new GameState();
        gameState.setFlop(flop);
        gameState.setTurn(three_spades);
        gameState.setRiver(ace_spades);

        PlayerLoki player = new PlayerLoki(ace_hearts, ace_diamonds, gameState);

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());

        player.calculateHandStatistics(weightArray, 2, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());


        System.out.println("Time for river ehs calculation: " + (end-start));


        System.out.println("HandStrength: " + player.getHandStrength());
        System.out.println("Positive Potential: " + player.getPositiveHandPotential());
        System.out.println("Negative Potential: " + player.getNegativeHandPotential());

        System.out.println("Handranker new Starting Hands: " + HandFactory.rankableHandCounter);

        /***************************************************************************
         * *************************************************************************
         * *************************************************************************
         */
    }


}
