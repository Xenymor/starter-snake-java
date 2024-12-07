package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

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

        Coord head;
        int[] moveScores;
        BattleSnake me;
        Coord[] body;
        Coord[] food;
        Board board;

        boolean[][] isOccupied;

        final int DIE_SCORE = -1_000_000;
        final int FOOD_SCORE = 20;
        final int LOSING_DUEL_SCORE = -50;
        final int WINNING_DUEL_SCORE = 10;
        final int LARGE_CAVITY_SCORE = 70;

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
            LOG.info("GAME START");
            Board board = new Board(startRequest.get("board"));
            isOccupied = new boolean[board.width][board.height];
            return EMPTY;
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

            /*try {
                LOG.info("Data: {}", JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(moveRequest));
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing payload", e);
            }*/

            /*
             * Example how to retrieve data from the request payload:
             *
             * String gameId = moveRequest.get("game").get("id").asText();
             *
             * int height = moveRequest.get("board").get("height").asInt();
             *
             */

            me = new BattleSnake(moveRequest.get("you"));
            board = new Board(moveRequest.get("board"));
            head = me.head;
            body = me.body;
            food = board.food;

            if (moveRequest.get("turn").asInt() == 2) {
                System.out.println();
            }

            for (int i = 0; i < board.width; i++) {
                Arrays.fill(isOccupied[i], false);
            }

            moveScores = new int[]{0, 0, 0, 0};

            //Prevent your Battlesnake from moving out of bounds

            if (head.x + 1 >= board.width) {
                moveScores[RIGHT] += DIE_SCORE;
            } else if (head.x <= 0) {
                moveScores[LEFT] += DIE_SCORE;
            }
            if (head.y + 1 >= board.height) {
                moveScores[UP] += DIE_SCORE;
            } else if (head.y <= 0) {
                moveScores[DOWN] += DIE_SCORE;
            }

            //Prevent your Battlesnake from colliding
            //Consider duel fields

            BattleSnake[] opponents = board.snakes;
            for (final BattleSnake battleSnake : opponents) {
                Coord[] opponent = battleSnake.body;
                markUnsafe(opponent);
                if (!Objects.equals(me.id, battleSnake.id)) {
                    handleDuelField(opponent[0], opponent.length);
                }
            }
            Coord tail = body[body.length - 1];
            isOccupied[tail.x][tail.y] = false;

            //Handle Cavities
            Coord[] neighbors = getInBoardNeighbors(head);
            StringBuilder string = new StringBuilder("LargeCavities: ");

            for (int i = 0; i < neighbors.length; i++) {
                int size = getCavitySize(neighbors[i]);
                if (size >= 2 * me.body.length) {
                    updateScore(neighbors[i], LARGE_CAVITY_SCORE);
                    string.append(i).append(",");
                }
            }

            //Move towards food
            boolean resetValue = isOccupied[head.x][head.y];
            isOccupied[head.x][head.y] = false;
            int[][] foodDists = generateDistArray(food, board.width, board.height);
            isOccupied[head.x][head.y] = resetValue;

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
            LOG.info(builder.toString());

            int headDist = foodDists[head.x][head.y];
            neighbors = getInBoardNeighbors(head);

            for (Coord neighbor : neighbors) {
                final int dist = foodDists[neighbor.x][neighbor.y];
                if (dist < headDist) {
                    if (!updateScore(neighbor, FOOD_SCORE)) {
                        System.out.println("???");
                    }
                } else if (dist > headDist) {
                    if (!updateScore(neighbor, -FOOD_SCORE)) {
                        System.out.println("???");
                    }
                }
            }

            Move nextMove = null;
            int maxScore = Integer.MIN_VALUE;
            for (Move move : Move.values()) {
                final int ordinal = move.ordinal();
                if (moveScores[ordinal] >= maxScore) {
                    maxScore = moveScores[ordinal];
                    nextMove = move;
                }
            }

            final String moveString = Objects.requireNonNull(nextMove).toString().toLowerCase();
            LOG.info("MOVE " + moveRequest.get("turn").asInt() + ":" + moveString + " ;scores:" + Arrays.toString(moveScores));
            LOG.info("LargeCavity for " + string);

            Map<String, String> answer = new HashMap<>();
            answer.put("move", moveString);

            LOG.info("MOVE {}", moveString);

            return answer;
        }

        private int[][] generateDistArray(final Coord[] foodCoords, final int width, final int height) {
            int[][] result = new int[width][height];
            for (final int[] ints : result) {
                Arrays.fill(ints, Integer.MAX_VALUE);
            }
            Set<Coord> visited = new HashSet<>();
            Queue<Coord> queue = new ArrayDeque<>();
            for (Coord food : foodCoords) {
                queue.add(food);
                result[food.x][food.y] = 0;
                visited.add(food);
            }
            while (queue.size() > 0) {
                Coord curr = queue.poll();
                int currDist = result[curr.x][curr.y] + 1;
                Coord[] neighbors = getInBoardNeighbors(curr);
                for (Coord neighbor : neighbors) {
                    if (!visited.contains(neighbor) && !isOccupied[neighbor.x][neighbor.y]) {
                        if (currDist < result[neighbor.x][neighbor.y]) {
                            result[neighbor.x][neighbor.y] = currDist;
                            queue.add(neighbor);
                            visited.add(neighbor);
                        }
                    }
                }
            }
            return result;
        }

        public void markUnsafe(Coord[] fields) {
            int max = fields.length;
            if (!canEat(fields[0])) {
                max--;
            }
            for (int i = 0; i < max; i++) {
                Coord curr = fields[i];
                isOccupied[curr.x][curr.y] = true;
                updateScore(curr, DIE_SCORE);
            }
        }

        public void handleDuelField(Coord otherHead, int length) {
            Coord[] candidateFields = getNeighbors(otherHead);
            for (Coord field : candidateFields) {
                if (dist(head, field) == 1) {
                    if (length >= body.length) {
                        updateScore(field, LOSING_DUEL_SCORE);
                    } else {
                        updateScore(field, WINNING_DUEL_SCORE);
                    }
                }
            }
        }

        public int dist(Coord field1, Coord field2) {
            return Math.abs(field1.x - field2.x) + Math.abs(field1.y - field2.y);
        }

        public Coord[] getNeighbors(Coord pos) {
            return new Coord[]{new Coord(pos.x + 1, pos.y), new Coord(pos.x - 1, pos.y), new Coord(pos.x, pos.y + 1), new Coord(pos.x, pos.y - 1)};
        }

        public boolean updateScore(Coord field, int change) {
            int xDiff = field.x - head.x;
            int yDiff = field.y - head.y;
            if (Math.abs(xDiff) == 1 && yDiff == 0) {
                if (xDiff < 0) {
                    moveScores[LEFT] += change;
                } else {
                    moveScores[RIGHT] += change;
                }
                return true;
            } else if (Math.abs(yDiff) == 1 && xDiff == 0) {
                if (yDiff < 0) {
                    moveScores[DOWN] += change;
                } else {
                    moveScores[UP] += change;
                }
                return true;
            } else {
                return false;
            }
        }

        public boolean canEat(Coord head) {
            Coord[] nextFields = getNeighbors(head);
            for (Coord field : nextFields) {
                for (final Coord coord : food) {
                    if (field.x == coord.x && field.y == coord.y) {
                        return true;
                    }
                }
            }
            return false;
        }

        public Coord[] getInBoardNeighbors(Coord pos) {
            int boardWidth = board.width;
            int boardHeight = board.height;
            int x = pos.x;
            int y = pos.y;

            List<Coord> neighbors = new ArrayList<>(4);

            if (x + 1 < boardWidth) {
                neighbors.add(new Coord(x + 1, y));
            }
            if (x - 1 >= 0) {
                neighbors.add(new Coord(x - 1, y));
            }
            if (y + 1 < boardHeight) {
                neighbors.add(new Coord(x, y + 1));
            }
            if (y - 1 >= 0) {
                neighbors.add(new Coord(x, y - 1));
            }
            return neighbors.toArray(Coord[]::new);
        }

        public int getCavitySize(Coord pos) {
            if (isOccupied[pos.x][pos.y]) {
                return 0;
            }

            Set<Coord> queued = new HashSet<>();
            Stack<Coord> stack = new Stack<>();
            stack.push(pos);
            int cavitySize = 0;

            while (stack.size() > 0) {
                Coord current = stack.pop();

                cavitySize++;

                Coord[] neighbors = getInBoardNeighbors(current);
                for (Coord neighbor : neighbors) {
                    if (!isOccupied[neighbor.x][neighbor.y]) {
                        if (!queued.contains(neighbor)) {
                            stack.add(neighbor);
                            queued.add(neighbor);
                        }
                    }
                }
            }

            return cavitySize;
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
            LOG.info("END");
            return EMPTY;
        }
    }

}
