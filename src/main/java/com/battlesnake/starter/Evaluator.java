package com.battlesnake.starter;

import org.slf4j.Logger;

import java.util.*;

public class Evaluator {
    final Logger log;

    private final int LEFT = 0;
    private final int RIGHT = 1;
    private final int UP = 2;
    private final int DOWN = 3;

    final int DIE_SCORE = -1_000_000;
    final int FOOD_SCORE = 10;
    final int CAPTURING_SCORE = 50;
    final int LOSING_DUEL_SCORE = -70;
    final int WINNING_DUEL_SCORE = 45;
    final int LARGE_CAVITY_SCORE = 200;
    final int EDGE_SCORE = -1;
    final int HP_THRESHOLD = 25;
    final int FOOD_SCORE_MULTIPLIER_WHEN_LOW = 3;

    public Evaluator(final Logger log) {
        this.log = log;
    }

    public MoveScore evaluate(final GameState gameState) {
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

    void logInfo(final String msg) {
        log.info(msg);
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
        gameState.me.generateDistArray(gameState);

        if (gameState.food.length == 0) {
            return;
        }

        CoordsInt[][] dists = gameState.me.distances;

        printDists(dists);

        Coord[] neighbors = gameState.getInBoardNeighbors(gameState.head, true);
        int currFoodScore = getCurrFoodScore(gameState);

        int lowestDist = Integer.MAX_VALUE;
        Coord nearest = null;
        for (Coord food : gameState.food) {
            final CoordsInt curr = dists[food.x][food.y];
            if (curr == null) {
                continue;
            }
            if (curr.number <= lowestDist) {
                lowestDist = curr.number;
                nearest = food;
            }
        }

        //TODO penalize moving away
        if (nearest != null) {
            if (contains(neighbors, nearest)) {
                updateScore(nearest, currFoodScore, gameState.head, moveScores);
            } else {
                Set<Coord> added = new HashSet<>();
                Queue<Coord> toCheck = new ArrayDeque<>();
                toCheck.add(nearest);
                added.add(nearest);

                while (!toCheck.isEmpty()) {
                    Coord curr = toCheck.poll();
                    for (Coord coord : dists[curr.x][curr.y].coords) {
                        if (!added.contains(coord)) {
                            added.add(coord);
                            if (contains(neighbors, coord)) {
                                updateScore(coord, currFoodScore, gameState.head, moveScores);
                            } else {
                                toCheck.add(coord);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean contains(final Coord[] arr, final Coord coord) {
        for (Coord curr : arr) {
            if (curr.equals(coord)) {
                return true;
            }
        }
        return false;
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

    private void printDists(final CoordsInt[][] foodDists) {
        StringBuilder builder = new StringBuilder("\n");
        for (int y = foodDists[0].length - 1; y >= 0; y--) {
            builder.append("|");
            for (final CoordsInt[] foodDist : foodDists) {
                final int dist = foodDist[y] == null ? Integer.MAX_VALUE : foodDist[y].number;
                if (dist == Integer.MAX_VALUE) {
                    builder.append("--");
                } else if (dist > 15) {
                    builder.append("ll");
                } else {
                    builder.append(String.format("%02d", dist));
                }
                builder.append("|");
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
        List<Coord> biggestCavities = new ArrayList<>();
        int biggestCavitySize = -1;
        for (int i = 0; i < neighbors.length; i++) {
            final Coord neighbor = neighbors[i];
            int size = gameState.getCavitySize(neighbor);
            if (size >= 2 * gameState.me.body.length) {
                updateScore(neighbor, LARGE_CAVITY_SCORE, gameState.head, moveScores);
                string.append(i).append(",");
                largeCavityExists = true;
            }
            if (size == biggestCavitySize) {
                biggestCavities.add(neighbor);
            } else if (size > biggestCavitySize) {
                biggestCavitySize = size;
                biggestCavities.clear();
                biggestCavities.add(neighbor);
            }
        }
        if (!largeCavityExists) {
            updateScores(biggestCavities.toArray(Coord[]::new), LARGE_CAVITY_SCORE, gameState.head, moveScores);
            string.delete(0, string.length());
            string.append("Largest cavity: ").append(biggestCavities);
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
        gameState.minOccupationTime[tail.x][tail.y] = 1;
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

    public static class MoveScore {
        public final Move bestMove;
        public final int bestScore;
        public final int[] moveScores;

        public MoveScore(final Move bestMove, final int bestScore, final int[] moveScores) {
            this.bestMove = bestMove;
            this.bestScore = bestScore;
            this.moveScores = moveScores;
        }
    }
}
