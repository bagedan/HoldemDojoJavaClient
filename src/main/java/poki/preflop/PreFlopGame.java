package poki.preflop;

import com.google.common.collect.ImmutableMap;
import poki.handranking.Card;
import poki.handranking.util.HandRanker;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tkachi on 10/5/2017.
 */
public class PreFlopGame {

    private float probabilityPlay = 0.6f;
    private int tightnessIndex = 1;


    public enum STRATEGY {
        MAKE0, MAKE1, MAKE2, MAKE4;
    }

    enum NUMBER_OF_PLAYER {
        TWO(2), THREE_OR_FOUR(3), MORE_THAN_FIVE(5);

        int number;
        NUMBER_OF_PLAYER(int number){
            this.number = number;
        }
    }

    public STRATEGY getPreflopStrategy(Card[] myhand, int numberPlayerGuaranteed, int numberPlayerStillInGame, int numberOfPlayerYetToAct) {
        float expectedNumberOfPlayers; /* expected number of players */
        NUMBER_OF_PLAYER group; /* TWO, THREEORFOUR, or FIVEPLUS */
        float IR; /* income rate */
        Map<STRATEGY, Float> thresh; /* indexed by strategies */
        /* determine the group */
        expectedNumberOfPlayers = numberPlayerGuaranteed +
                probabilityPlay * (numberPlayerStillInGame - numberPlayerGuaranteed);
        if (numberPlayerStillInGame < 2.5) {
            group = NUMBER_OF_PLAYER.TWO;
        } else if (expectedNumberOfPlayers >= 4.5) {
            group = NUMBER_OF_PLAYER.MORE_THAN_FIVE;
        } else {
            group = NUMBER_OF_PLAYER.THREE_OR_FOUR;
        }
/* calculate IR (see Appendix A) and thresholds. */
        IR = HandRanker.startingHandRank(myhand[0],
                myhand[1], group.number);
/* only the small blind has different thresholds for CALL1 and CALL2 */
        thresh = getThreshold(group, tightnessIndex, numberOfPlayerYetToAct);
/* now use IR to select the appropriate strategy */
        if (IR >= thresh.get(STRATEGY.MAKE4))
            return STRATEGY.MAKE4;
        else if (IR >= thresh.get(STRATEGY.MAKE2))
            return STRATEGY.MAKE2;
        else if (IR >= thresh.get(STRATEGY.MAKE1))
            return STRATEGY.MAKE1;
        else
            return STRATEGY.MAKE0;
    }

    public Map<STRATEGY, Float> getThreshold(NUMBER_OF_PLAYER group, int tightnessIndex, int myPosition) {
        Map<STRATEGY, BaseAndIncrement[]> baseAndIncrementValues = null;
        switch (group){
            case TWO:
                baseAndIncrementValues = valueForBaseAndIncrementTwoPlayer;
                break;
            case THREE_OR_FOUR:
                baseAndIncrementValues = valueForBaseAndIncrementThreeFourPlayer;
                break;
            case MORE_THAN_FIVE:
                baseAndIncrementValues = valueForBaseAndIncrementFivePlayer;
        }
        Map<STRATEGY, Float> thresholds = new HashMap<>();

        for(Map.Entry<STRATEGY, BaseAndIncrement[]> entry : baseAndIncrementValues.entrySet()){
            float threshold = entry.getValue()[tightnessIndex].base +
                    entry.getValue()[tightnessIndex].increment * myPosition;

            thresholds.put(entry.getKey(), threshold);
        }

        return thresholds;
    }

    Map<STRATEGY, BaseAndIncrement[]> valueForBaseAndIncrementTwoPlayer =
            ImmutableMap.of(
                    STRATEGY.MAKE1, new BaseAndIncrement[] {new BaseAndIncrement(-50, 50), new BaseAndIncrement(-50, 50), new BaseAndIncrement(-50, 50)},
                    STRATEGY.MAKE2, new BaseAndIncrement[] {new BaseAndIncrement(150, 50), new BaseAndIncrement(50, 50), new BaseAndIncrement(0, 0)},
                    STRATEGY.MAKE4, new BaseAndIncrement[] {new BaseAndIncrement(300, 50), new BaseAndIncrement(300, 50), new BaseAndIncrement(300, 50)}
            );

    Map<STRATEGY, BaseAndIncrement[]> valueForBaseAndIncrementThreeFourPlayer =
            ImmutableMap.of(
                    STRATEGY.MAKE1, new BaseAndIncrement[] {new BaseAndIncrement(50, 50), new BaseAndIncrement(50, 25), new BaseAndIncrement(50, 10)},
                    STRATEGY.MAKE2, new BaseAndIncrement[] {new BaseAndIncrement(200, 50), new BaseAndIncrement(200, 25), new BaseAndIncrement(200, 10)},
                    STRATEGY.MAKE4, new BaseAndIncrement[] {new BaseAndIncrement(580, 0), new BaseAndIncrement(580, 0), new BaseAndIncrement(580, 0)}
            );

    Map<STRATEGY, BaseAndIncrement[]> valueForBaseAndIncrementFivePlayer =
            ImmutableMap.of(
                    STRATEGY.MAKE1, new BaseAndIncrement[] {new BaseAndIncrement(0, 70), new BaseAndIncrement(0, 70), new BaseAndIncrement(0, 30)},
                    STRATEGY.MAKE2, new BaseAndIncrement[] {new BaseAndIncrement(450, 50), new BaseAndIncrement(450, 25), new BaseAndIncrement(450, 10)},
                    STRATEGY.MAKE4, new BaseAndIncrement[] {new BaseAndIncrement(900, 0), new BaseAndIncrement(900, 0), new BaseAndIncrement(900, 0)}
            );


    public class BaseAndIncrement{
        public int base;
        public int increment;

        public BaseAndIncrement(int base, int increment) {
            this.base = base;
            this.increment = increment;
        }
    }
}
