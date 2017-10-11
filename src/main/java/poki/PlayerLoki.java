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
import poki.handranking.StartingHand;
import poki.handranking.util.HandFactory;
import poki.handranking.util.HandRanker;
import poki.handranking.util.HandRankingException;
import poki.util.Combinations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * This class represents a poker player.
 *
 * @author Jellen Vermeir
 */
public class PlayerLoki {

    // The starting hand, representing the two player hole Cards.
    private final StartingHand holeCards;
    // The current gamestate.
    private final GameState gameState;

    // This value represents the immediate handstrength
    private double handStrength;
    // This value respresents the implied odds
    private double positiveHandPotential;
    // This value represents the reverse implied odds
    private double negativeHandPotential;


    final int AHEAD = 0;
    final int BEHIND = 1;
    final int TIED = 2;

    /**
     * Create a player, taking the two holecards and current gamestate as input.
     *
     * @param c1    The first holecard
     * @param c2    The second holecard
     * @param state The GameState
     */
    public PlayerLoki(Card c1, Card c2, GameState state) {
        this.gameState = state;
        this.holeCards = new StartingHand(c1, c2);
    }

    /**
     * Return the expected immediate strength of the Players Hand.
     *
     * @return The handStrength
     */
    public double getHandStrength() {
        return this.handStrength;
    }

    /**
     * Return the expected positive Player hand potential.
     *
     * @return the positive hand potential.
     */
    public double getPositiveHandPotential() {
        return this.positiveHandPotential;
    }

    /**
     * Return the expected negative Hand Potential.
     *
     * @return the negative hand potential.
     */
    public double getNegativeHandPotential() {
        return this.negativeHandPotential;
    }

    public void calculateHandStatistics(List<List<Double>> weights, int nrOpponents, List<Card> unknownCards) throws HandRankingException {

        // Fetch community cards
        Set<Card> communityCards = this.gameState.getCommunityCards();
        if (communityCards == null) {
            throw new IllegalStateException("This method should not be used for pre flop calculations");
        } else if (communityCards.size() == 3) {
            calculateFlopHandStatistics(weights, nrOpponents, unknownCards);
        } else if (communityCards.size() == 4) {
            calculateTurnHandStatistics(weights, nrOpponents, unknownCards);
        } else if (communityCards.size() == 5) {
            calculateRiverHandStatistics(weights, nrOpponents, unknownCards);
        } else {
            throw new IllegalStateException("Community cards have wrong size " + communityCards.size());
        }

    }

    private void calculateRiverHandStatistics(List<List<Double>> weights, int nrOpponents, List<Card> unknownCards) throws HandRankingException {
        final int AHEAD = 0;
        final int BEHIND = 1;
        final int TIED = 2;
        // ahead, behind, tied
        double[] outcomes = new double[]{0, 0, 0}; // for handstrength
        // Transitions from ahead/behind/tied to ahead/behind/tied
        double[][] transitionMatrix = new double[3][3]; // for potential
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transitionMatrix[i][j] = 0;
            }
        }

        // Opponent hands combinations
        List<List<Integer>> opponentHandCombinations = Combinations.combine(unknownCards.size(), 2);

        // Fetch communitycards
        Set<Card> communityCards = this.gameState.getCommunityCards();
        // Fetch player hand
        Set<Card> playerHand = this.holeCards.getCards();

        List<Card> playerCards = new ArrayList<>(communityCards);
        playerCards.addAll(playerHand);
        List<Card> enemyCards = new ArrayList<>(7);

        int initialPlayerHandValue =
                HandFactory.getHand(playerCards).getHandValue();

        // for each possible set of opponent hole cards
        for (List<Integer> comb : opponentHandCombinations) {

            Card u1 = unknownCards.get(comb.get(0) - 1);
            Card u2 = unknownCards.get(comb.get(1) - 1);

            enemyCards.clear();
            enemyCards.addAll(communityCards);
            enemyCards.add(u1);
            enemyCards.add(u2);

            int initialEnemyHandValue =
                    HandFactory.getHand(enemyCards).getHandValue();

            int initialStatus;
            double enemyCardWeights = HandRanker.Map_169(u1, u2, weights);

            if (initialPlayerHandValue > initialEnemyHandValue) {
                initialStatus = AHEAD;
                outcomes[AHEAD] += enemyCardWeights;
            } else if (initialPlayerHandValue < initialEnemyHandValue) {
                initialStatus = BEHIND;
                outcomes[BEHIND] += enemyCardWeights;
            } else {
                initialStatus = TIED;
                outcomes[TIED] += enemyCardWeights;
            }

        }

        double hs = (outcomes[AHEAD] + outcomes[TIED] / 2) / (outcomes[AHEAD] + outcomes[BEHIND] + outcomes[TIED]);
        this.handStrength = Math.pow(hs, nrOpponents);

    }

    private void calculateTurnHandStatistics(List<List<Double>> weights, int nrOpponents, List<Card> unknownCards) throws HandRankingException {
        final int AHEAD = 0;
        final int BEHIND = 1;
        final int TIED = 2;
        // ahead, behind, tied
        double[] outcomes = new double[]{0, 0, 0}; // for handstrength
        // Transitions from ahead/behind/tied to ahead/behind/tied
        DoubleAdder[][] transitionMatrix = new DoubleAdder[3][3]; // for potential
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transitionMatrix[i][j] = new DoubleAdder();
            }
        }

        // Opponent hands combinations
        List<List<Integer>> opponentHandcombinations = Combinations.combine(unknownCards.size(), 2);
        // BoardCard Combinations
        List<List<Integer>> futureBoardCombinations
                = Combinations.combine(unknownCards.size() - 2, 1);

        // Fetch communitycards
        Set<Card> communityCards = this.gameState.getCommunityCards();
        // Fetch player hand
        Set<Card> playerHand = this.holeCards.getCards();

        List<Card> playerCards = new ArrayList<>(communityCards);
        playerCards.addAll(playerHand);
        List<Card> enemyCards = new ArrayList<>(7);

        int initialPlayerHandValue =
                HandFactory.getHand(playerCards).getHandValue();

        // for each possible set of opponent hole cards
        for (List<Integer> comb : opponentHandcombinations) {

            Card u1 = unknownCards.get(comb.get(0) - 1);
            Card u2 = unknownCards.get(comb.get(1) - 1);

            enemyCards.clear();
            enemyCards.addAll(communityCards);
            enemyCards.add(u1);
            enemyCards.add(u2);

            int initialEnemyHandValue =
                    HandFactory.getHand(enemyCards).getHandValue();

            int initialStatus;
            double enemyCardWeights = HandRanker.Map_169(u1, u2, weights);

            if (initialPlayerHandValue > initialEnemyHandValue) {
                initialStatus = AHEAD;
                outcomes[AHEAD] += enemyCardWeights;
            } else if (initialPlayerHandValue < initialEnemyHandValue) {
                initialStatus = BEHIND;
                outcomes[BEHIND] += enemyCardWeights;
            } else {
                initialStatus = TIED;
                outcomes[TIED] += enemyCardWeights;
            }

            //calculate potentials
            List<Card> remainingCards = new ArrayList<>(unknownCards);
            remainingCards.remove(u1);
            remainingCards.remove(u2);

            ForkJoinPool forkJoinPool = new ForkJoinPool(2);
            forkJoinPool.submit(() -> {
                futureBoardCombinations.parallelStream().forEach(boardCombi -> updateTurnTransitionMatrix(playerCards, enemyCards, remainingCards,
                        transitionMatrix, boardCombi, enemyCardWeights, initialStatus));
            });
        }


        double hs = (outcomes[AHEAD] + outcomes[TIED] / 2) / (outcomes[AHEAD] + outcomes[BEHIND] + outcomes[TIED]);
        this.handStrength = Math.pow(hs, nrOpponents);

        // printStats(outcomes, transitionMatrix);
        double sumBehind = transitionMatrix[BEHIND][BEHIND].sum() + transitionMatrix[BEHIND][AHEAD].sum() + transitionMatrix[BEHIND][TIED].sum();
        double sumAhead = transitionMatrix[AHEAD][BEHIND].sum() + transitionMatrix[AHEAD][AHEAD].sum() + transitionMatrix[AHEAD][TIED].sum();
        double sumTied = transitionMatrix[TIED][BEHIND].sum() + transitionMatrix[TIED][AHEAD].sum() + transitionMatrix[TIED][TIED].sum();

        this.positiveHandPotential = (transitionMatrix[BEHIND][AHEAD].sum() + transitionMatrix[BEHIND][TIED].sum() / 2 + transitionMatrix[TIED][AHEAD].sum() / 2) /
                (sumBehind + sumTied / 2);
        this.negativeHandPotential = (transitionMatrix[AHEAD][BEHIND].sum() + transitionMatrix[AHEAD][TIED].sum() / 2 + transitionMatrix[TIED][BEHIND].sum() / 2) /
                (sumAhead + sumTied / 2);

        //printStats(outcomes, transitionMatrix);
    }

    private void calculateFlopHandStatistics(List<List<Double>> weights, int nrOpponents, List<Card> unknownCards) throws HandRankingException {
        // ahead, behind, tied
        double[] outcomes = new double[]{0, 0, 0}; // for handstrength
        // Transitions from ahead/behind/tied to ahead/behind/tied
        DoubleAdder[][] transitionMatrix = new DoubleAdder[3][3]; // for potential
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transitionMatrix[i][j] = new DoubleAdder();
            }
        }

        // Opponent hands combinations
        List<List<Integer>> opponentHandcombinations = Combinations.combine(unknownCards.size(), 2);
        // BoardCard Combinations
        List<List<Integer>> futureBoardCombinations
                = Combinations.combine(unknownCards.size() - 2, 2);

        // Fetch communitycards
        Set<Card> communityCards = this.gameState.getCommunityCards();
        // Fetch player hand
        Set<Card> playerHand = this.holeCards.getCards();

        List<Card> playerCards = new ArrayList<>(communityCards);
        playerCards.addAll(playerHand);
        List<Card> enemyCards = new ArrayList<>(7);

        int initialPlayerHandValue =
                HandFactory.getHand(playerCards).getHandValue();

        // for each possible set of opponent hole cards
        for (List<Integer> comb : opponentHandcombinations) {

            Card u1 = unknownCards.get(comb.get(0) - 1);
            Card u2 = unknownCards.get(comb.get(1) - 1);

            enemyCards.clear();
            enemyCards.addAll(communityCards);
            enemyCards.add(u1);
            enemyCards.add(u2);

            int initialEnemyHandValue =
                    HandFactory.getHand(enemyCards).getHandValue();

            int initialStatus;
            double enemyCardWeights = HandRanker.Map_169(u1, u2, weights);

            if (initialPlayerHandValue > initialEnemyHandValue) {
                initialStatus = AHEAD;
                outcomes[AHEAD] += enemyCardWeights;
            } else if (initialPlayerHandValue < initialEnemyHandValue) {
                initialStatus = BEHIND;
                outcomes[BEHIND] += enemyCardWeights;
            } else {
                initialStatus = TIED;
                outcomes[TIED] += enemyCardWeights;
            }

            //calculate potentials
            List<Card> remainingCards = new ArrayList<>(unknownCards);
            remainingCards.remove(u1);
            remainingCards.remove(u2);

            ForkJoinPool forkJoinPool = new ForkJoinPool(2);
            forkJoinPool.submit(() -> {
                futureBoardCombinations.parallelStream().forEach(boardCombi -> updateFlopTransitionMatrix(playerCards, enemyCards, remainingCards,
                        transitionMatrix, boardCombi, enemyCardWeights, initialStatus));
            });


        }


        double hs = (outcomes[AHEAD] + outcomes[TIED] / 2) / (outcomes[AHEAD] + outcomes[BEHIND] + outcomes[TIED]);
        this.handStrength = Math.pow(hs, nrOpponents);

        // printStats(outcomes, transitionMatrix);
        double sumBehind = transitionMatrix[BEHIND][BEHIND].sum() + transitionMatrix[BEHIND][AHEAD].sum() + transitionMatrix[BEHIND][TIED].sum();
        double sumAhead = transitionMatrix[AHEAD][BEHIND].sum() + transitionMatrix[AHEAD][AHEAD].sum() + transitionMatrix[AHEAD][TIED].sum();
        double sumTied = transitionMatrix[TIED][BEHIND].sum() + transitionMatrix[TIED][AHEAD].sum() + transitionMatrix[TIED][TIED].sum();

        this.positiveHandPotential = (transitionMatrix[BEHIND][AHEAD].sum() + transitionMatrix[BEHIND][TIED].sum() / 2 + transitionMatrix[TIED][AHEAD].sum() / 2) /
                (sumBehind + sumTied / 2);
        this.negativeHandPotential = (transitionMatrix[AHEAD][BEHIND].sum() + transitionMatrix[AHEAD][TIED].sum() / 2 + transitionMatrix[TIED][BEHIND].sum() / 2) /
                (sumAhead + sumTied / 2);

        //printStats(outcomes, transitionMatrix);

    }

    private void updateFlopTransitionMatrix(List<Card> playerCards, List<Card> enemyCards, List<Card> remainingCards,
                                            DoubleAdder[][] transitionMatrix, List<Integer> boardCombi,
                                            double enemyCardWeights, int initialStatus) {
        try {
            List<Card> impliedPlayerCards = new ArrayList<>(7);
            List<Card> impliedEnemyCards = new ArrayList<>(7);
            impliedPlayerCards.addAll(playerCards);
            impliedEnemyCards.addAll(enemyCards);

            Card u3 = remainingCards.get(boardCombi.get(0) - 1);
            impliedPlayerCards.add(u3);
            impliedEnemyCards.add(u3);
            Card u4 = remainingCards.get(boardCombi.get(1) - 1);
            impliedPlayerCards.add(u4);
            impliedEnemyCards.add(u4);

            int impliedPlayerHandValue =
                    HandFactory.getHand(impliedPlayerCards).getHandValue();
            int impliedEnemyHandValue =
                    HandFactory.getHand(impliedEnemyCards).getHandValue();

            if (impliedPlayerHandValue > impliedEnemyHandValue)
                transitionMatrix[initialStatus][AHEAD].add(enemyCardWeights);
            else if (impliedPlayerHandValue < impliedEnemyHandValue)
                transitionMatrix[initialStatus][BEHIND].add(enemyCardWeights);
            else
                transitionMatrix[initialStatus][TIED].add(enemyCardWeights);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private void updateTurnTransitionMatrix(List<Card> playerCards, List<Card> enemyCards, List<Card> remainingCards,
                                            DoubleAdder[][] transitionMatrix, List<Integer> boardCombi,
                                            double enemyCardWeights, int initialStatus) {
        try {
            List<Card> impliedPlayerCards = new ArrayList<>(7);
            List<Card> impliedEnemyCards = new ArrayList<>(7);
            impliedPlayerCards.addAll(playerCards);
            impliedEnemyCards.addAll(enemyCards);

            Card possibleRiverCard = remainingCards.get(boardCombi.get(0) - 1);
            impliedPlayerCards.add(possibleRiverCard);
            impliedEnemyCards.add(possibleRiverCard);

            int impliedPlayerHandValue =
                    HandFactory.getHand(impliedPlayerCards).getHandValue();
            int impliedEnemyHandValue =
                    HandFactory.getHand(impliedEnemyCards).getHandValue();

            if (impliedPlayerHandValue > impliedEnemyHandValue)
                transitionMatrix[initialStatus][AHEAD].add(enemyCardWeights);
            else if (impliedPlayerHandValue < impliedEnemyHandValue)
                transitionMatrix[initialStatus][BEHIND].add(enemyCardWeights);
            else
                transitionMatrix[initialStatus][TIED].add(enemyCardWeights);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }


    /**
     * Debug DEMO
     *
     * @param outcomes         The outcomes, for evaluation of immediate handStrength.
     * @param transitionMatrix The transitionMatrix, for evaluation of (reverse) implied odds.
     */
    private void printStats(double[] outcomes, double[][] transitionMatrix) {

        final int AHEAD = 0;
        final int BEHIND = 1;
        final int TIED = 2;
        double sumBehind = transitionMatrix[BEHIND][BEHIND] + transitionMatrix[BEHIND][AHEAD] + transitionMatrix[BEHIND][TIED];
        double sumAhead = transitionMatrix[AHEAD][BEHIND] + transitionMatrix[AHEAD][AHEAD] + transitionMatrix[AHEAD][TIED];
        double sumTied = transitionMatrix[TIED][BEHIND] + transitionMatrix[TIED][AHEAD] + transitionMatrix[TIED][TIED];

        System.out.println("Handstrength (current board):");
        System.out.println("Ahead weighted sum: " + outcomes[AHEAD]);
        System.out.println("Behind weighted sum: " + outcomes[BEHIND]);
        System.out.println("Tied weighted sum: " + outcomes[TIED]);
        double hs = (outcomes[AHEAD] + outcomes[TIED] / 2) / (outcomes[AHEAD] + outcomes[BEHIND] + outcomes[TIED]);
        System.out.println("Handstrength one opponent: " + hs);

        System.out.println("TRANSITIONS (currently ahead)");
        System.out.println("Total simulations sarting ahead: " + sumAhead);
        System.out.println("Ahead-Ahead: " + transitionMatrix[AHEAD][AHEAD]);
        System.out.println("Ahead-Tied: " + transitionMatrix[AHEAD][TIED]);
        System.out.println("Ahead-Behind: " + transitionMatrix[AHEAD][BEHIND]);

        System.out.println("TRANSITIONS (currently behind)");
        System.out.println("Total simulations sarting behind: " + sumBehind);
        System.out.println("Behind-Ahead: " + transitionMatrix[BEHIND][AHEAD]);
        System.out.println("Behind-Tied: " + transitionMatrix[BEHIND][TIED]);
        System.out.println("Behind-Behind" + transitionMatrix[BEHIND][BEHIND]);

        System.out.println("TRANSITIONS (currently tied)");
        System.out.println("Total simulations sarting tied: " + sumTied);
        System.out.println("Tied-Ahead: " + transitionMatrix[TIED][AHEAD]);
        System.out.println("Tied-Tied: " + transitionMatrix[TIED][TIED]);
        System.out.println("Tied-Behind: " + transitionMatrix[TIED][BEHIND]);
    }
}
