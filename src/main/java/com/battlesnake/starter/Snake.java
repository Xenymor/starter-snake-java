package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static spark.Spark.*;

/**
 * This is a simple BattleSnake server written in Java.
 * <p>
 * For instructions see
 * https://github.com/BattlesnakeOfficial/starter-snake-java/README.md
 */
public class Snake {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Snake.class);

    /**
     * Main entry point.
     *
     * @param args are ignored.
     */
    public static void main(String[] args) {
        String port = System.getProperty("PORT");
        if (port == null) {
            LOG.info("Using default port: {}", (Object) null);
            port = "8080";
        } else {
            LOG.info("Found system provided port: {}", port);
        }
        port(Integer.parseInt(port));
        get("/", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/start", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/move", HANDLER::process, JSON_MAPPER::writeValueAsString);
        post("/end", HANDLER::process, JSON_MAPPER::writeValueAsString);
    }

    /**
     * Handler class for dealing with the routes set UP in the main method.
     */
    public static class Handler {

        /**
         * For the start/end request
         */
        private static final Map<String, String> EMPTY = new HashMap<>();
        private static final int LEFT = 0;
        private static final int RIGHT = 1;
        private static final int UP = 2;
        private static final int DOWN = 3;

        final int DIE_SCORE = -1_000_000;
        final int FOOD_SCORE = 10;
        final int CAPTURING_SCORE = 50;
        final int LOSING_DUEL_SCORE = -70;
        final int WINNING_DUEL_SCORE = 45;
        final int LARGE_CAVITY_SCORE = 200;
        final int EDGE_SCORE = -1;
        final int HP_THRESHOLD = 25;
        final int FOOD_SCORE_MULTIPLIER_WHEN_LOW = 3;

        /**
         * Generic processor that prints out the request and response from the methods.
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse;
                switch (uri) {
                    case "/":
                        snakeResponse = index();
                        break;
                    case "/start":
                        snakeResponse = start(parsedRequest);
                        break;
                    case "/move":
                        snakeResponse = move(parsedRequest);
                        break;
                    case "/end":
                        snakeResponse = end(parsedRequest);
                        break;
                    default:
                        throw new IllegalAccessError("Strange call made to the snake: " + uri);
                }

                LOG.info("Responding with: {}", JSON_MAPPER.writeValueAsString(snakeResponse));

                return snakeResponse;
            } catch (JsonProcessingException e) {
                LOG.warn("Something went wrong!", e);
                return null;
            }
        }

        /**
         * This method is called everytime your BattleSnake is entered into a game.
         * <p>
         * Use this method to decide how your BattleSnake is going to look on the board.
         *
         * @return a response back to the engine containing the BattleSnake setUP
         * values.
         */
        public Map<String, String> index() {
            Map<String, String> response = new HashMap<>();
            response.put("apiversion", "1");
            response.put("author", "Xenymor");
            response.put("color", "#e04a00");
            response.put("head", "viper");
            response.put("tail", "freckled");
            return response;
        }

        /**
         * This method is called everytime your BattleSnake is entered into a game.
         * <p>
         * Use this method to decide how your BattleSnake is going to look on the board.
         *
         * @param startRequest a JSON data map containing the information about the game
         *                     that is about to be played.
         *
         * @return responses back to the engine are ignored.
         */

        public Map<String, String> start(JsonNode startRequest) {
            logInfo("GAME START");
            return EMPTY;
        }


        static class MoveScore {
            public final Move bestMove;
            public final int bestScore;
            public final int[] moveScores;

            public MoveScore(final Move bestMove, final int bestScore, final int[] moveScores) {
                this.bestMove = bestMove;
                this.bestScore = bestScore;
                this.moveScores = moveScores;
            }
        }

        /**
         * This method is called on every turn of a game. It's how your snake decides
         * where to move.
         * <p>
         * Use the information in 'moveRequest' to decide your next move. The
         * 'moveRequest' variable can be interacted with as
         * com.fasterxml.jackson.databind.JsonNode, and contains all of the information
         * about the BattleSnake board for each move of the game.
         * <p>
         * For a full example of 'json', see
         * https://docs.battlesnake.com/references/api/sample-move-request
         *
         * @param moveRequest JsonNode of all Game Board data as received from the
         *                    BattleSnake Engine.
         *
         * @return a Map<String,String> response back to the engine the single move to
         * make. One of "UP", "DOWN", "LEFT" or "RIGHT".
         */


        public Map<String, String> move(JsonNode moveRequest) {

            GameState gameState = new GameState(moveRequest);

            MoveScore moveScore = getScore(gameState);

            final String moveString = Objects.requireNonNull(moveScore.bestMove).toString().toLowerCase();
            logInfo("MOVE " + moveRequest.get("turn").asInt() + ":" + moveString + " ;scores:" + Arrays.toString(moveScore.moveScores));

            Map<String, String> answer = new HashMap<>();
            answer.put("move", moveString);

            logInfo("MOVE " + moveString);

            return answer;
        }

        private MoveScore getScore(final GameState gameState) {
            int[] moveScores = new int[]{0, 0, 0, 0};

            considerBounds(gameState, moveScores);

            //Prevent your Battlesnake from colliding
            //Consider duel fields

            stopCollisions(gameState, moveScores);

            //Handle Cavities
            Coord[] neighbors = gameState.getInBoardNeighbors(gameState.head, true);
            StringBuilder string = new StringBuilder("LargeCavities: ");

            handleCavities(gameState, moveScores, neighbors, string);

            //Reduce Edge-Score
            penalizeEdges(gameState, moveScores, neighbors);

            //Move towards food
            incentivizeFood(gameState, moveScores);

            Move nextMove = chooseMove(moveScores, string);

            return new MoveScore(nextMove, moveScores[nextMove.ordinal()], moveScores);
        }

        private Move chooseMove(final int[] moveScores, final StringBuilder string) {
            Move nextMove = null;
            int maxScore = Integer.MIN_VALUE;
            for (Move move : Move.values()) {
                final int ordinal = move.ordinal();
                if (moveScores[ordinal] >= maxScore) {
                    maxScore = moveScores[ordinal];
                    nextMove = move;
                }
            }
            logInfo(string.toString());
            return nextMove;
        }

        private void incentivizeFood(final GameState gameState, final int[] moveScores) {
            gameState.minOccupationTime[gameState.head.x][gameState.head.y] = 0;
            int[][] foodDists = gameState.generateDistArray(gameState.food, this);
            gameState.minOccupationTime[gameState.head.x][gameState.head.y] = gameState.me.length - 1;

            printDists(foodDists);

            int headDist = foodDists[gameState.head.x][gameState.head.y];
            Coord[] neighbors = gameState.getInBoardNeighbors(gameState.head, true);
            int currFoodScore = getCurrFoodScore(gameState);

            for (Coord neighbor : neighbors) {
                handleNeighbor(neighbor, moveScores, foodDists[neighbor.x][neighbor.y], headDist, currFoodScore, gameState);
            }
        }

        private void handleNeighbor(final Coord neighbor, final int[] moveScores, final int dist, final int headDist, final int currFoodScore, final GameState gameState) {
            if (dist < headDist) {
                if (!updateScore(neighbor, currFoodScore, gameState.head, moveScores)) {
                    System.out.println("???");
                }
            } else if (dist > headDist) {
                if (!updateScore(neighbor, -currFoodScore, gameState.head, moveScores)) {
                    System.out.println("???");
                }
            }
        }

        private int getCurrFoodScore(final GameState gameState) {
            int currFoodScore;
            if (gameState.me.health <= HP_THRESHOLD) {
                currFoodScore = FOOD_SCORE * FOOD_SCORE_MULTIPLIER_WHEN_LOW;
                logInfo("Low on hp; Searching food");
            } else {
                currFoodScore = FOOD_SCORE;
            }
            return currFoodScore;
        }

        private void printDists(final int[][] foodDists) {
            StringBuilder builder = new StringBuilder("\n");
            for (int y = foodDists[0].length - 1; y >= 0; y--) {
                for (final int[] foodDist : foodDists) {
                    final int dist = foodDist[y];
                    if (dist == Integer.MAX_VALUE) {
                        builder.append("-");
                    } else {
                        builder.append(dist);
                    }
                }
                builder.append("\n");
            }
            logInfo(builder.toString());
        }

        private void penalizeEdges(final GameState gameState, final int[] moveScores, final Coord[] neighbors) {
            for (Coord neighbor : neighbors) {
                if (gameState.isEdge(neighbor)) {
                    updateScore(neighbor, EDGE_SCORE, gameState.head, moveScores);
                }
            }
        }

        private void handleCavities(final GameState gameState, final int[] moveScores, final Coord[] neighbors, StringBuilder string) {
            boolean largeCavityExists = false;
            Coord biggestCavity = gameState.head;
            int biggestCavitySize = -1;
            for (int i = 0; i < neighbors.length; i++) {
                int size = gameState.getCavitySize(neighbors[i]);
                if (size >= 2 * gameState.me.body.length) {
                    updateScore(neighbors[i], LARGE_CAVITY_SCORE, gameState.head, moveScores);
                    string.append(i).append(",");
                    largeCavityExists = true;
                }
                if (size > biggestCavitySize) {
                    biggestCavitySize = size;
                    biggestCavity = neighbors[i];
                }
            }
            if (!largeCavityExists) {
                updateScore(biggestCavity, LARGE_CAVITY_SCORE, gameState.head, moveScores);
                string.delete(0, string.length());
                string.append("Largest cavity: ").append(biggestCavity);
            }
        }

        private void stopCollisions(final GameState gameState, final int[] moveScores) {
            BattleSnake[] opponents = gameState.board.snakes;
            boolean isHeadEdge = gameState.isEdge(gameState.head);
            for (final BattleSnake battleSnake : opponents) {
                Coord[] opponent = battleSnake.body;
                gameState.updateMinOccupationTime(battleSnake, moveScores, this);
                if (!Objects.equals(gameState.me.id, battleSnake.id)) {
                    final Coord opponentHead = opponent[0];
                    handleDuelField(opponentHead, opponent.length, moveScores, gameState);

                    //Handle catching snakes on edge
                    incentivizeCatchingOnEdge(gameState, moveScores, isHeadEdge, opponentHead);
                }
            }
            Coord tail = gameState.body[gameState.body.length - 1];
            gameState.minOccupationTime[tail.x][tail.y] = 0;
        }

        private void incentivizeCatchingOnEdge(final GameState gameState, final int[] moveScores, final boolean isHeadEdge, final Coord opponentHead) {
            if (!isHeadEdge) {
                if (gameState.isEdge(opponentHead)) {
                    if (gameState.head.isNeighbour(opponentHead)) {
                        Coord[] moves = gameState.getInBoardNeighbors(opponentHead, true);
                        if (moves.length == 1) {
                            updateScores(gameState.getInBoardNeighbors(moves[0], true),
                                    CAPTURING_SCORE, gameState.head, moveScores);
                        } else {
                            System.out.println("??? when checking for possible moves of caught snake");
                        }
                    }
                }
            }
        }

        private void considerBounds(final GameState gameState, final int[] moveScores) {
            //Prevent your Battlesnake from moving out of bounds

            if (gameState.head.x + 1 >= gameState.width) {
                moveScores[RIGHT] += DIE_SCORE;
            } else if (gameState.head.x <= 0) {
                moveScores[LEFT] += DIE_SCORE;
            }
            if (gameState.head.y + 1 >= gameState.height) {
                moveScores[UP] += DIE_SCORE;
            } else if (gameState.head.y <= 0) {
                moveScores[DOWN] += DIE_SCORE;
            }
        }

        private void updateScores(final Coord[] fields, final int score, Coord head, int[] moveScores) {
            for (Coord field : fields) {
                updateScore(field, score, head, moveScores);
            }
        }

        public void handleDuelField(Coord otherHead, int opponentLength, final int[] moveScores, final GameState gameState) {
            Coord[] candidateFields = otherHead.getNeighbors();
            for (Coord field : candidateFields) {
                if (gameState.head.dist(field) == 1) {
                    if (opponentLength >= gameState.me.length) {
                        updateScore(field, LOSING_DUEL_SCORE, gameState.head, moveScores);
                    } else {
                        updateScore(field, WINNING_DUEL_SCORE, gameState.head, moveScores);
                    }
                }
            }
        }

        public boolean updateScore(Coord field, int score, Coord head, int[] moveScores) {
            int xDiff = field.x - head.x;
            int yDiff = field.y - head.y;
            if (Math.abs(xDiff) == 1 && yDiff == 0) {
                if (xDiff < 0) {
                    moveScores[LEFT] += score;
                } else {
                    moveScores[RIGHT] += score;
                }
                return true;
            } else if (Math.abs(yDiff) == 1 && xDiff == 0) {
                if (yDiff < 0) {
                    moveScores[DOWN] += score;
                } else {
                    moveScores[UP] += score;
                }
                return true;
            } else {
                return false;
            }
        }

        /**
         * This method is called when a game your BattleSnake was in ends.
         * <p>
         * It is purely for informational purposes, you don't have to make any decisions
         * here.
         *
         * @param endRequest a map containing the JSON sent to this snake. Use this data
         *                   to know which game has ended
         *
         * @return responses back to the engine are ignored.
         */
        public Map<String, String> end(JsonNode endRequest) {
            logInfo("END");
            return EMPTY;
        }
    }

    static BufferedWriter outputStream;

    static {
        try {
            outputStream = new BufferedWriter(new FileWriter("logfile.log"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void logInfo(final String msg) {
        LOG.info(msg);
        try {
            outputStream.append(msg);
        } catch (IOException e) {
            try {
                outputStream.append(e.toString());
            } catch (IOException ex) {
                e.printStackTrace();
            }
        }
    }

}
