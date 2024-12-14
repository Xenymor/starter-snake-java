package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

public class GameState {
    final BattleSnake me;
    final Board board;
    final Coord head;
    final Coord[] body;
    final Coord[] food;
    final int width;
    final int height;
    final boolean[][] isOccupied;

    public GameState(final JsonNode moveRequest) {
        me = new BattleSnake(moveRequest.get("you"));
        board = new Board(moveRequest.get("board"));
        head = me.head;
        body = me.body;
        food = board.food;
        width = board.width;
        height = board.height;
        isOccupied = new boolean[width][height];
    }
}
