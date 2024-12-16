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

    int[][] generateDistArray(final Coord[] coords, final Evaluator evaluator) {
        int[][] result = new int[width][height];
        for (final int[] ints : result) {
            Arrays.fill(ints, Integer.MAX_VALUE);
        }
        Set<Coord> visited = new HashSet<>();
        Queue<Coord> queue = new ArrayDeque<>();
        for (Coord food : coords) {
            queue.add(food);
            result[food.x][food.y] = 0;
            visited.add(food);
        }
        while (queue.size() > 0) {
            Coord curr = queue.poll();
            int currDist = result[curr.x][curr.y] + 1;
            Coord[] neighbors = getInBoardNeighbors(curr, true);
            for (Coord neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
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

    boolean isOccupied(final int x, final int y) {
        return minOccupationTime[x][y] > 0;
    }

    //TODO make prettier
    void updateMinOccupationTime(BattleSnake snake, int[] moveScores, final Evaluator evaluator) {
        for (int i = 0; i < snake.length; i++) {
            Coord curr = snake.body[i];
            minOccupationTime[curr.x][curr.y] = snake.length - i - 1;
            if (i == snake.length - 1) {
                if (snake.canEat(food)) {
                    minOccupationTime[curr.x][curr.y] = 1;
                } else {
                    evaluator.updateScore(curr, evaluator.DIE_SCORE, head, moveScores);
                }
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
        Stack<Coord> stack = new Stack<>();
        stack.push(pos);
        int cavitySize = 0;

        while (stack.size() > 0) {
            Coord current = stack.pop();

            cavitySize++;

            Coord[] neighbors = getInBoardNeighbors(current, false);
            for (Coord neighbor : neighbors) {
                if (!isOccupied(neighbor.x, neighbor.y)) {
                    if (!queued.contains(neighbor)) {
                        stack.add(neighbor);
                        queued.add(neighbor);
                    }
                }
            }
        }

        return cavitySize;
    }
}
