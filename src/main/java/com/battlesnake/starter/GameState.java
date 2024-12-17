package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class GameState {
    final BattleSnake me;
    final Board board;
    final Coord head;
    final Coord[] body;
    final Coord[] food;
    final int width;
    final int height;
    final int[][] minOccupationTime;

    public GameState(final JsonNode moveRequest) {
        me = new BattleSnake(moveRequest.get("you"));
        board = new Board(moveRequest.get("board"));
        head = me.head;
        body = me.body;
        food = board.food;
        width = board.width;
        height = board.height;
        minOccupationTime = new int[width][height];
    }

    Coord[] getInBoardNeighbors(final Coord pos, final boolean mustBeFree) {
        int x = pos.x;
        int y = pos.y;

        List<Coord> neighbors = new ArrayList<>(4);

        if (x + 1 < width) {
            if (!mustBeFree || !isOccupied(x + 1, y)) {
                neighbors.add(new Coord(x + 1, y));
            }
        }
        if (x - 1 >= 0) {
            if (!mustBeFree || !isOccupied(x - 1, y)) {
                neighbors.add(new Coord(x - 1, y));
            }
        }
        if (y + 1 < height) {
            if (!mustBeFree || !isOccupied(x, y + 1)) {
                neighbors.add(new Coord(x, y + 1));
            }
        }
        if (y - 1 >= 0) {
            if (!mustBeFree || !isOccupied(x, y - 1)) {
                neighbors.add(new Coord(x, y - 1));
            }
        }

        return neighbors.toArray(Coord[]::new);
    }

    Coord[] getInBoardNeighbors(final Coord pos, final int freeIn) {
        int x = pos.x;
        int y = pos.y;

        List<Coord> neighbors = new ArrayList<>(4);

        if (x + 1 < width) {
            if (!isOccupiedIn(x + 1, y, freeIn)) {
                neighbors.add(new Coord(x + 1, y));
            }
        }
        if (x - 1 >= 0) {
            if (!isOccupiedIn(x - 1, y, freeIn)) {
                neighbors.add(new Coord(x - 1, y));
            }
        }
        if (y + 1 < height) {
            if (!isOccupiedIn(x, y + 1, freeIn)) {
                neighbors.add(new Coord(x, y + 1));
            }
        }
        if (y - 1 >= 0) {
            if (!isOccupiedIn(x, y - 1, freeIn)) {
                neighbors.add(new Coord(x, y - 1));
            }
        }

        return neighbors.toArray(Coord[]::new);
    }

    private boolean isOccupiedIn(final int x, final int y, final int freeIn) {
        return minOccupationTime[x][y] > freeIn;
    }

    boolean isOccupied(final int x, final int y) {
        return minOccupationTime[x][y] > 1;
    }

    //TODO make prettier
    void updateMinOccupationTime(BattleSnake snake, int[] moveScores, final Evaluator evaluator) {
        for (int i = 0; i < snake.length; i++) {
            Coord curr = snake.body[i];
            minOccupationTime[curr.x][curr.y] = snake.length - i;
            if (i == snake.length - 1) {
                //TODO for all fields
                if (snake.canEat(food)) {
                    minOccupationTime[curr.x][curr.y] = 2;
                    evaluator.updateScore(curr, evaluator.DIE_SCORE, head, moveScores);
                }
            } else {
                evaluator.updateScore(curr, evaluator.DIE_SCORE, head, moveScores);
            }
        }
    }

    boolean isEdge(final Coord pos) {
        return pos.x == 0 || pos.x == width - 1 || pos.y == 0 || pos.y == height - 1;
    }

    int getCavitySize(final Coord pos) {
        if (isOccupied(pos.x, pos.y)) {
            return 0;
        }

        Set<Coord> queued = new HashSet<>();
        Stack<CoordInt> stack = new Stack<>();
        stack.push(new CoordInt(pos, 0));
        queued.add(pos);
        int cavitySize = 0;

        while (stack.size() > 0) {
            CoordInt current = stack.pop();

            cavitySize++;

            final int newCount = current.count + 1;
            Coord[] neighbors = getInBoardNeighbors(current.coord, newCount);
            for (Coord neighbor : neighbors) {
                if (!queued.contains(neighbor)) {
                    stack.add(new CoordInt(neighbor, newCount));
                    queued.add(neighbor);
                }
            }
        }

        return cavitySize;
    }
}
