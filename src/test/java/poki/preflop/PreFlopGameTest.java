package poki.preflop;

import org.junit.Test;
import poki.handranking.Card;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Tkachi on 10/5/2017.
 */
public class PreFlopGameTest {

    private PreFlopGame preFlopGame = new PreFlopGame();

    @Test
    public void testThresholdFormula(){
        Map<PreFlopGame.STRATEGY, Float> thresholds = preFlopGame.getThreshold(
                PreFlopGame.NUMBER_OF_PLAYER.THREE_OR_FOUR,
                0, 1);

        assertTrue(thresholds.get(PreFlopGame.STRATEGY.MAKE1) == 100f);
        assertTrue(thresholds.get(PreFlopGame.STRATEGY.MAKE2) == 250f);
        assertTrue(thresholds.get(PreFlopGame.STRATEGY.MAKE4) == 580f);
    }

    @Test
    public void testStrategyDeciding(){
        Card[] myHand = new Card[] {
              new Card(Card.SUIT_DIAMONDS, Card.RANK_ACE),
              new Card(Card.SUIT_DIAMONDS, Card.RANK_10)
        };

        PreFlopGame.STRATEGY resultStrategy = preFlopGame.getPreflopStrategy(myHand,
                3, 4, 1);

        assertEquals(resultStrategy, PreFlopGame.STRATEGY.MAKE2);
    }

}