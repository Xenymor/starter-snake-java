package com.battlesnake.starter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static spark.Spark.*;

/**
 * This is a simple BattleSnake server written in Java.
 * <p>
 * For instructions see
 * <a href="https://github.com/BattlesnakeOfficial/starter-snake-java/">GitHub of the original</a>
 */
public class Main {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final Handler HANDLER = new Handler();
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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
        Evaluator evaluator;

        /**
         * Generic processor that prints out the request and response from the methods.
         */
        public Map<String, String> process(Request req, Response res) {
            try {
                JsonNode parsedRequest = JSON_MAPPER.readTree(req.body());
                String uri = req.uri();
                LOG.info("{} called with: {}", uri, req.body());
                Map<String, String> snakeResponse = switch (uri) {
                    case "/" -> index();
                    case "/start" -> start(parsedRequest);
                    case "/move" -> move(parsedRequest);
                    case "/end" -> end(parsedRequest);
                    default -> throw new IllegalAccessError("Strange call made to the snake: " + uri);
                };

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
            response.put("color", "#e04a01");
            response.put("head", "viper");
            response.put("tail", "flame");
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
            evaluator = new Evaluator(LOG);
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

            GameState gameState = new GameState(moveRequest);

            Evaluator.MoveScore moveScore = evaluator.evaluate(gameState);

            final String moveString = Objects.requireNonNull(moveScore.bestMove).toString().toLowerCase();

            Map<String, String> answer = new HashMap<>();
            answer.put("move", moveString);

            return answer;
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
            return EMPTY;
        }
    }

}
