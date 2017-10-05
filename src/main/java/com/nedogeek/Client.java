package com.nedogeek;


import com.google.common.collect.ImmutableMap;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class Client {
    private static final String userName = "bogdan2";
    private static final String password = "somePassword";

    private static final String SERVER = "ws://localhost:8080/ws";
    private org.eclipse.jetty.websocket.WebSocket.Connection connection;

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
        final int bet;
        final String status;
        final List<Card> cards;

        Player(String name, int balance, int bet, String status, List<Card> cards) {
            this.name = name;
            this.balance = balance;
            this.bet = bet;
            this.status = status;
            this.cards = cards;
        }

        @Override
        public String toString() {
            return "Player{" +
                    "name='" + name + '\'' +
                    ", balance=" + balance +
                    ", bet=" + bet +
                    ", status='" + status + '\'' +
                    ", cards=" + cards +
                    '}';
        }
    }

    List<Card> deskCards;

    int pot;
    String gameRound;

    String dealer;
    String mover;
    List<String> event;
    List<Player> players;

    String cardCombination;

    private PreFlopGame preFlopGame = new PreFlopGame();

    private PreFlopGame.STRATEGY preFlopStrategy = PreFlopGame.STRATEGY.MAKE1;
    private int preFlopMovedTimes = 0;

    double flopEHSCalculated = 0;

    int numberOfRaisesInThisRoundByAll = 0;
    int numberOfRaisesToCall = 0;
    int numberOfMyBetsPutInInThisRound = 0;
    String prevRound = "";

    public Client() {
        con();
    }

    public static void main(String[] args) {
        new Client();
    }

    private void parseMessage(String message) {
        JSONObject json = new JSONObject(message);

        if (json.has("deskPot")) {
            pot = json.getInt("deskPot");
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

        if(prevRound.equals(gameRound)) {
            if (event.get(0).contains("makes bet")) {
                numberOfRaisesInThisRoundByAll++;
                if(!event.contains(userName)){
                    numberOfRaisesToCall++;
                }
            }
        } else {
            prevRound = gameRound;
            numberOfRaisesInThisRoundByAll = 0;
            numberOfRaisesToCall = 0;
            numberOfMyBetsPutInInThisRound = 0;
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
            int bet = 0;
            String status = "";
            String name = "";
            List<Card> cards = new ArrayList<>();

            if (playerJSON.has("balance")) {
                balance = playerJSON.getInt("balance");
            }
            if (playerJSON.has("pot")) {
                bet = playerJSON.getInt("pot");
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

            players.add(new Player(name, balance, bet, status, cards));
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
        Player me = players.stream().filter(player -> player.name.equals(userName)).findFirst().get();
        System.out.println("There has been " + numberOfRaisesInThisRoundByAll + " in this round already");
        System.out.println("There are " + numberOfRaisesToCall + " raises to call now");
        if (gameRound.equals("BLIND")) {
            flopEHSCalculated = 0;
            doPreFlopMove(me);
        }else if(gameRound.equals("THREE_CARDS")){
            preFlopStrategy = null;
            preFlopMovedTimes = 0;

            doFlopMove(me);

        } else {
            doFlopMove(me);
        }


        numberOfRaisesToCall = 0;

    }

    private void doFlopMove(Player me) throws HandRankingException, IOException {

        if(flopEHSCalculated == 0){
            System.out.println("Calculating esh...");
            flopEHSCalculated = calculateFlopESH(me);
        }
        int betsToMakeInThisRound = 0;
        if(flopEHSCalculated >= 0.85){
            betsToMakeInThisRound = 2;
        } else if(flopEHSCalculated >=0.5){
            betsToMakeInThisRound = 1;
        }

        System.out.println("betsToMakeInThisRound is " + betsToMakeInThisRound);
        System.out.println("numberOfRaisesInThisRoundByAll is " + numberOfRaisesInThisRoundByAll);
        System.out.println("numberOfRaisesToCall is " + numberOfRaisesToCall);
        System.out.println("numberOfMyBetsPutInInThisRound is " + numberOfMyBetsPutInInThisRound);
        if(numberOfRaisesInThisRoundByAll < betsToMakeInThisRound){
            System.out.println("My cards look good - raising");
            connection.sendMessage(Commands.Rise.toString());
            numberOfMyBetsPutInInThisRound++;
        } else if(numberOfMyBetsPutInInThisRound > 0 ||
                betsToMakeInThisRound >=2 ||
                numberOfRaisesToCall <= betsToMakeInThisRound){
            System.out.println("Have raised already - from now on only calling");
            connection.sendMessage(Commands.Call.toString());
        } else {
            System.out.println("Too many raises - fold");
            connection.sendMessage(Commands.Fold.toString());
        }
    }

    private double calculateFlopESH(Player me) throws HandRankingException {
        List<List<Double>> weightArray = HandRanker.getUniformWeightArray();
        GameState postFlopGameState = new GameState();
        Deck deck = new Deck();

        Set<poki.handranking.Card> flop = deskCards.stream()
                .map(card -> card.toPokiCard()).collect(Collectors.toSet());
        postFlopGameState.setFlop(flop);

        deck.removeCards(flop);


        deck.removeCard(me.cards.get(0).toPokiCard());
        deck.removeCard(me.cards.get(1).toPokiCard());

        poki.Player pokiPlayer = new poki.Player(me.cards.get(0).toPokiCard(),
                me.cards.get(1).toPokiCard(), postFlopGameState);

        int numberOfActivePlayer = (int) players.stream().filter(
                player -> !player.status.equals("Fold")
        ).count();

        List<poki.handranking.Card> remainingCards = deck.getCards();

        long start = System.currentTimeMillis();
        System.out.println(Instant.now());
        pokiPlayer.calculateHandStrength(weightArray, numberOfActivePlayer, remainingCards);
        System.out.println(Instant.now());

        pokiPlayer.calculateHandPotential(weightArray, numberOfActivePlayer, true, remainingCards);
        long end = System.currentTimeMillis();
        System.out.println(Instant.now());

        System.out.println("Time: " + (end - start));

        //not including negative potentials here
        double EHS_optimistic = pokiPlayer.getHandStrength() +
                (1 - pokiPlayer.getHandStrength()) * pokiPlayer.getPositiveHandPotential();

        System.out.println("EHS is " + EHS_optimistic);
        return EHS_optimistic;
    }

    private void doPreFlopMove(Player me) throws IOException {
        preFlopStrategy = PreFlopGame.STRATEGY.MAKE2;
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
}
