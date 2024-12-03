package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class Board {

    final int height;
    final int width;
    final Coord[] food;
    final Coord[] hazards;
    final BattleSnake[] snakes;

    public Board(final JsonNode board) {
        height = board.get("height").asInt();
        width = board.get("width").asInt();
        food = readCoordArray(board, "food", width * height);
        hazards = readCoordArray(board, "hazards", width * height);
        snakes = readSnakeArray(board);
    }

    private BattleSnake[] readSnakeArray(final JsonNode board) {
        JsonNode snakeNode = board.get("snakes");
        List<BattleSnake> snakeList = new ArrayList<>();
        for (int i = 0; i < width * height; i++) {
            try {
                snakeList.add(new BattleSnake(snakeNode.get(i)));
            } catch (Exception e) {
                break;
            }
        }
        return snakeList.toArray(new BattleSnake[0]);
    }

    public static Coord[] readCoordArray(final JsonNode board, final String name, final int maxLength) {
        JsonNode foodNode = board.get(name);
        List<Coord> foodList = new ArrayList<>();
        for (int i = 0; i < maxLength; i++) {
            try {
                foodList.add(new Coord(foodNode.get(i)));
            } catch (Exception e) {
                break;
            }
        }
        return foodList.toArray(new Coord[0]);
    }
}
