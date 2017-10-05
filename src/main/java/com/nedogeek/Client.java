package com.nedogeek;


import com.google.common.collect.ImmutableMap;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import poki.preflop.PreFlopGame;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class Client {
    private static final String userName = "bogdan";
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

                    if (userName.equals(mover)) {
                        try {
                            doAnswer();
                        } catch (IOException e) {
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

    private PreFlopGame.STRATEGY preFlopStrategy = null;
    private int preFlopMovedTimes = 0;

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

    private void doAnswer() throws IOException {
        Player me = players.stream().filter(player -> player.name.equals(userName)).findFirst().get();

        if (gameRound.equals("BLIND")) {

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


        } else {
            preFlopStrategy = null;
            preFlopMovedTimes = 0;
            System.out.println("Post flop stage - raising all the time");
            connection.sendMessage(Commands.Rise.toString()+",10");
        }

    }
}
