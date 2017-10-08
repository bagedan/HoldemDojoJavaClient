package com.nedogeek;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import poki.Deck;
import poki.GameState;
import poki.handranking.util.HandRanker;
import poki.handranking.util.HandRankingException;
import poki.preflop.PreFlopGame;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Client {
    private static final String userName = "bogdan4";
    private static final String password = "somePassword";

    private static final String SERVER = "ws://localhost:8080/ws";
    private org.eclipse.jetty.websocket.WebSocket.Connection connection;
    private double[] currentRoundEhs;

    enum Commands {
        Check, Call, Rise, Fold, AllIn
    }

    public class Card {
        final String suit;
        final String value;

        private Map<String, Integer> suitMap = ImmutableMap.of(
                "♣", poki.handranking.Card.SUIT_CLUBS,
                "♦", poki.handranking.Card.SUIT_DIAMONDS,
                "♥", poki.handranking.Card.SUIT_HEARTS,
                "♠", poki.handranking.Card.SUIT_SPADES
        );

        private Map<String, Integer> valuesMap = ImmutableMap.<String, Integer>builder()
                .put("2", poki.handranking.Card.RANK_2)
                .put("3", poki.handranking.Card.RANK_3)
                .put("4", poki.handranking.Card.RANK_4)
                .put("5", poki.handranking.Card.RANK_5)
                .put("6", poki.handranking.Card.RANK_6)
                .put("7", poki.handranking.Card.RANK_7)
                .put("8", poki.handranking.Card.RANK_8)
                .put("9", poki.handranking.Card.RANK_9)
                .put("10", poki.handranking.Card.RANK_10)
                .put("J", poki.handranking.Card.RANK_JACK)
                .put("Q", poki.handranking.Card.RANK_QUEEN)
                .put("K", poki.handranking.Card.RANK_KING)
                .put("A", poki.handranking.Card.RANK_ACE)
                .build();


        Card(String suit, String value) {
            this.suit = suit;
            this.value = value;
        }

        poki.handranking.Card toPokiCard() {
            poki.handranking.Card pokiCard = new poki.handranking.Card(suitMap.get(suit), valuesMap.get(value));
            System.out.printf("Mapped %s:%s to %s", suit, value, pokiCard);
            return pokiCard;
        }

        @Override
        public String toString() {
            return "Card{" +
                    "value='" + value + '\'' +
                    ", suit='" + suit + '\'' +
                    '}';
        }
    }


    private void con() {
        WebSocketClientFactory factory = new WebSocketClientFactory();
        try {
            factory.start();

            WebSocketClient client = factory.newWebSocketClient();

            connection = client.open(new URI(SERVER + "?user=" + userName + "&password=" + password), new org.eclipse.jetty.websocket.WebSocket.OnTextMessage() {
                public void onOpen(Connection connection) {
                    System.out.println("Opened");
                }

                public void onClose(int closeCode, String message) {
                    System.out.println("Closed");
                }

                public void onMessage(String data) {
                    parseMessage(data);
                    System.out.println(data);

                    if (userName.equals(mover) && event.get(0).contains(userName)) {
                        try {
                            doAnswer();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (HandRankingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class Player {

        final String name;
        final int balance;
        final int pot;
        final String status;
        final List<Card> cards;

        Player(String name, int balance, int pot, String status, List<Card> cards) {
            this.name = name;
            this.balance = balance;
            this.pot = pot;
            this.status = status;
            this.cards = cards;
        }

        @Override
        public String toString() {
            return "Player{" +
                    "name='" + name + '\'' +
                    ", balance=" + balance +
                    ", pot=" + pot +
                    ", status='" + status + '\'' +
                    ", cards=" + cards +
                    '}';
        }
    }

    List<Card> deskCards;

    int deskPot;
    String gameRound;

    String dealer;
    String mover;
    List<String> event;
    List<Player> players;

    String cardCombination;

    private PreFlopGame preFlopGame = new PreFlopGame();

    private PreFlopGame.STRATEGY preFlopStrategy = PreFlopGame.STRATEGY.MAKE1;
    private int preFlopMovedTimes = 0;

    int numberOfRaisesInThisRoundByAll = 0;
    int numberOfRaisesToCall = 0;
    int numberOfMyBetsPutInInThisRound = 0;
    String prevRound = "";

    int EHS_OPTIMISTIC_INDEX = 0;
    int EHS_FULL_INDEX = 1;
    int PPOT_INDEX = 2;


    int SMALL_BLIND = 1;

    boolean semiBlufFlag = false;

    public Client() {
        con();
    }

    public static void main(String[] args) {
        new Client();
    }

    private void parseMessage(String message) {
        JSONObject json = new JSONObject(message);

        if (json.has("deskPot")) {
            deskPot = json.getInt("deskPot");
        }
        if (json.has("mover")) {
            mover = json.getString("mover");
        }
        if (json.has("dealer")) {
            dealer = json.getString("dealer");
        }
        if (json.has("gameRound")) {
            gameRound = json.getString("gameRound");
        }
        if (json.has("event")) {
            event = parseEvent(json.getJSONArray("event"));
        }
        if (json.has("players")) {
            players = parsePlayers(json.getJSONArray("players"));
        }

        if (json.has("deskCards")) {
            deskCards = parseCards(((JSONArray) json.get("deskCards")));
        }

        if (json.has("combination")) {
            cardCombination = json.getString("combination");
        }

        if (prevRound.equals(gameRound)) {
            if (event.get(0).contains("makes bet")) {
                numberOfRaisesInThisRoundByAll++;
                if (!event.contains(userName)) {
                    numberOfRaisesToCall++;
                }
            }
        } else {
            prevRound = gameRound;
            numberOfRaisesInThisRoundByAll = 0;
            numberOfRaisesToCall = 0;
            numberOfMyBetsPutInInThisRound = 0;
            currentRoundEhs = null;
        }
    }

    private List<String> parseEvent(JSONArray eventJSON) {
        List<String> events = new ArrayList<>();

        for (int i = 0; i < eventJSON.length(); i++) {
            events.add(eventJSON.getString(i));
        }

        return events;
    }

    private List<Player> parsePlayers(JSONArray playersJSON) {
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playersJSON.length(); i++) {
            JSONObject playerJSON = (JSONObject) playersJSON.get(i);
            int balance = 0;
            int pot = 0;
            String status = "";
            String name = "";
            List<Card> cards = new ArrayList<>();

            if (playerJSON.has("balance")) {
                balance = playerJSON.getInt("balance");
            }
            if (playerJSON.has("pot")) {
                pot = playerJSON.getInt("pot");
            }
            if (playerJSON.has("status")) {
                status = playerJSON.getString("status");
            }
            if (playerJSON.has("name")) {
                name = playerJSON.getString("name");
            }
            if (playerJSON.has("cards")) {
                cards = parseCards((JSONArray) playerJSON.get("cards"));
            }

            players.add(new Player(name, balance, pot, status, cards));
        }

        return players;
    }

    private List<Card> parseCards(JSONArray cardsJSON) {
        List<Card> cards = new ArrayList<>();

        for (int i = 0; i < cardsJSON.length(); i++) {
            String cardSuit = ((JSONObject) cardsJSON.get(i)).getString("cardSuit");
            String cardValue = ((JSONObject) cardsJSON.get(i)).getString("cardValue");

            cards.add(new Card(cardSuit, cardValue));
        }

        return cards;
    }

    private void doAnswer() throws IOException, HandRankingException {
        try {
            Player me = players.stream().filter(player -> player.name.equals(userName)).findFirst().get();
            System.out.println("My cards: " + me.cards.get(0).suit+":"+me.cards.get(0).value + " and " + me.cards.get(1).suit+":"+me.cards.get(1).value );
            System.out.println("There has been " + numberOfRaisesInThisRoundByAll + " raises in this round already");
            System.out.println("There are " + numberOfRaisesToCall + " raises to call now");
            if (gameRound.equals("BLIND")) {
                semiBlufFlag = false;
                doPreFlopMove(me);
            } else {
                preFlopStrategy = null;
                preFlopMovedTimes = 0;

                doPostFlopMove(me);
            }
        } catch (Throwable th) {
            System.out.println(th);
        }


        numberOfRaisesToCall = 0;

    }

    private void doPostFlopMove(Player me) throws HandRankingException, IOException {
        if(currentRoundEhs == null) {
            System.out.println("Calculating esh...");
            currentRoundEhs = calculateHandStrengthAndPotentials(me);
        }

        double ehsOptimisticCalculated = currentRoundEhs[EHS_OPTIMISTIC_INDEX];
        if (ehsOptimisticCalculated >= 0.85) {
            System.out.println("Cards are great! Going to do at two raises in this round");
            doRaisingMove(2);
            return;
        } else if (ehsOptimisticCalculated >= 0.5) {
            System.out.println("Cards are fine. Going to do max one raises in this round");
            doRaisingMove(1);
            return;
        }

        System.out.println("Cards are not good enough to raise - checking other options");
        if (numberOfRaisesToCall == 0) {
            System.out.println("Nobody raised yet - may be worth to do some bluffing");
            doSemiBluffMove(me, currentRoundEhs[PPOT_INDEX]);
            return;
        } else {
            System.out.println("Someone raised already - checking pot and showdown odds");
            doPotAndShowDownOddsMove(me, currentRoundEhs[EHS_FULL_INDEX], currentRoundEhs[PPOT_INDEX]);
        }
    }

    private void doPotAndShowDownOddsMove(Player me, double ehs_full, double PPOT) throws IOException {
        int maxPotSoFar = players.stream().map(player -> player.pot).max(Integer::compareTo).get();
        int betsToCall = maxPotSoFar - me.pot;

        double potOdds = betsToCall / (deskPot + betsToCall);

        if ((gameRound.equals("FIVE_CARDS") && ehs_full >= potOdds) ||
                (!gameRound.equals("FIVE_CARDS") && PPOT >= potOdds)) {
            System.out.println("Calling based on pots odds");
            connection.sendMessage(Commands.Call.toString());
            return;
        }

        if (gameRound.equals("FIVE_CARDS")) {
            connection.sendMessage(Commands.Fold.toString());
            return;
        }

        if (gameRound.equals("THREE_CARDS")) {
            connection.sendMessage(Commands.Fold.toString());
            return;
        }

        int showDownCost = 0;

        if (gameRound.equals("THREE_CARDS")) {
            showDownCost = betSize() * 4;
        } else {
            showDownCost = betSize();
        }

        double showDownOdds = (betsToCall + showDownCost)/(deskPot + betsToCall + 2*showDownCost);

        if(ehs_full >= showDownOdds){
            System.out.println("Calling based on showdown odds");
            connection.sendMessage(Commands.Call.toString());
            return;
        }

        connection.sendMessage(Commands.Fold.toString());
    }

    private void doSemiBluffMove(Player me, double PPOT) throws IOException {
        int betSize = betSize();

        float potOdds = 2 * betSize / (deskPot + 6 * betSize);
        if (semiBlufFlag || (gameRound.equals("THREE_CARDS") && PPOT >= potOdds)) {
            semiBlufFlag = true;
            System.out.println("Decided to do semi bluffing as nobody raise and cards are not so bad");
            connection.sendMessage(Commands.Rise.toString());
        } else {
            System.out.println("Cards are not good enough to go for bluffing - just checking");
            connection.sendMessage(Commands.Check.toString());
        }

    }

    private void doRaisingMove(int betsToMakeInThisRound) throws IOException {
        System.out.println("betsToMakeInThisRound is " + betsToMakeInThisRound);
        System.out.println("numberOfRaisesInThisRoundByAll is " + numberOfRaisesInThisRoundByAll);
        System.out.println("numberOfRaisesToCall is " + numberOfRaisesToCall);
        System.out.println("numberOfMyBetsPutInInThisRound is " + numberOfMyBetsPutInInThisRound);
        if (numberOfRaisesInThisRoundByAll < betsToMakeInThisRound) {
            System.out.println("My cards look good - raising");
            connection.sendMessage(Commands.Rise.toString());
            numberOfMyBetsPutInInThisRound++;
        } else if (numberOfMyBetsPutInInThisRound > 0 ||
                betsToMakeInThisRound >= 2 ||
                numberOfRaisesToCall <= betsToMakeInThisRound) {
            System.out.println("Not confident enough to raise - from now on only calling");
            connection.sendMessage(Commands.Call.toString());
        } else {
            System.out.println("Too many raises - fold");
            connection.sendMessage(Commands.Fold.toString());
        }
    }

    private double[] calculateHandStrengthAndPotentials(Player me) throws HandRankingException {
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();
        GameState gameState = new GameState();
        Deck deck = new Deck();

        List<poki.handranking.Card> deskPokiCards = deskCards.stream()
                .map(card -> card.toPokiCard()).collect(Collectors.toList());
        gameState.setFlop(ImmutableSet.of(deskPokiCards.get(0), deskPokiCards.get(1), deskPokiCards.get(2)));

        if (deskPokiCards.size() > 3) {
            gameState.setTurn(deskPokiCards.get(3));
        }

        if (deskPokiCards.size() > 4) {
            gameState.setRiver(deskPokiCards.get(4));
        }

        deck.removeCards(deskPokiCards);


        deck.removeCard(me.cards.get(0).toPokiCard());
        deck.removeCard(me.cards.get(1).toPokiCard());

        poki.PlayerLoki pokiPlayer = new poki.PlayerLoki(me.cards.get(0).toPokiCard(),
                me.cards.get(1).toPokiCard(), gameState);

        int numberOfActivePlayer = (int) players.stream().filter(
                player -> !player.status.equals("Fold")
        ).count();

        List<poki.handranking.Card> remainingCards = deck.getCards();

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());

        pokiPlayer.calculateHandStatistics(weightArray, numberOfActivePlayer, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time to calculate ehs: " + (end - start));

        //not including negative potentials here
        double EHS_optimistic = pokiPlayer.getHandStrength() +
                (1 - pokiPlayer.getHandStrength()) * pokiPlayer.getPositiveHandPotential();

        double EHS_full = pokiPlayer.getHandStrength() +
                (1 - pokiPlayer.getHandStrength()) * pokiPlayer.getPositiveHandPotential() -
                -pokiPlayer.getHandStrength() * pokiPlayer.getNegativeHandPotential();

        System.out.println("EHS_opt is " + EHS_optimistic);
        System.out.println("EHS_full is " + EHS_full);
        double[] result = new double[3];
        result[EHS_OPTIMISTIC_INDEX] = EHS_optimistic;
        result[EHS_FULL_INDEX] = EHS_full;
        result[PPOT_INDEX] = pokiPlayer.getPositiveHandPotential();

        return result;
    }

    private void doPreFlopMove(Player me) throws IOException {
//        preFlopStrategy = PreFlopGame.STRATEGY.MAKE2;
        if (preFlopStrategy == null) {

            int numberOfActivePlayer = (int) players.stream().filter(
                    player -> !player.status.equals("Fold")
            ).count();

            int numberPlayerYetToAct = (int) players.stream().filter(
                    player -> player.status.equals("NotMoved")
            ).count();

            preFlopStrategy = preFlopGame.getPreflopStrategy(
                    new poki.handranking.Card[]{me.cards.get(0).toPokiCard(),
                            me.cards.get(1).toPokiCard()},
                    numberOfActivePlayer - numberPlayerYetToAct + 1,
                    numberOfActivePlayer,
                    numberPlayerYetToAct
            );
        }

        System.out.println("For preflop choose strategy: " + preFlopStrategy);

        switch (preFlopStrategy) {
            case MAKE0:
                System.out.println("Got bad cards - folding");
                connection.sendMessage(Commands.Fold.toString());
                preFlopStrategy = null;
                preFlopMovedTimes = 0;
                break;
            case MAKE1:
                if (preFlopMovedTimes == 0) {
                    System.out.println("Got fine cards - call for the first time");
                    connection.sendMessage(Commands.Call.toString());
                    preFlopMovedTimes++;
                } else {
                    System.out.println("Got fine cards, but not good enough to call twice - folding");
                    connection.sendMessage(Commands.Fold.toString());
                    preFlopStrategy = null;
                    preFlopMovedTimes = 0;
                }
                break;
            case MAKE2:
                if (preFlopMovedTimes < 2) {
                    System.out.println("Got better cards - raise for the first two times");
                    connection.sendMessage(Commands.Rise.toString());
                    preFlopMovedTimes++;
                } else {
                    System.out.println("Got better cards, but not good enough to continue raising - calling");
                    connection.sendMessage(Commands.Call.toString());
                }
                break;
            case MAKE4:
                System.out.println("Got the best card - raising all the time");
                connection.sendMessage(Commands.Rise.toString());
                preFlopMovedTimes++;
                break;
        }

    }

    private int betSize(){
        int maxPotSoFar = players.stream().map(player -> player.pot).max(Integer::compareTo).get();
        int betSize = 2 * SMALL_BLIND + maxPotSoFar;
        return betSize;
    }
}
