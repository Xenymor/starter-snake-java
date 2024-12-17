package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class BattleSnake {
    public final String id;
    public final Coord[] body;
    public Coord head;
    public final int squad;
    public final String name;
    public final int health;
    public final String latency;
    public final int length;
    public final String shout;
    public CoordsInt[][] distances;

    public BattleSnake(final JsonNode jsonNode) {
        id = jsonNode.get("id").asText();
        length = jsonNode.get("length").asInt();
        body = Board.readCoordArray(jsonNode, "body", length);
        squad = jsonNode.get("squad").asInt();
        name = jsonNode.get("name").asText();
        health = jsonNode.get("health").asInt();
        latency = jsonNode.get("latency").asText();
        shout = jsonNode.get("shout").asText();
        head = new Coord(jsonNode.get("head"));
    }

    public boolean canEat(Coord[] food) {
        Coord[] nextFields = body[0].getNeighbors();
        for (Coord field : nextFields) {
            for (final Coord coord : food) {
                if (field.x == coord.x && field.y == coord.y) {
                    return true;
                }
            }
        }
        return false;
    }


    void generateDistArray(GameState gameState) {
        distances = new CoordsInt[gameState.width][gameState.height];

        Set<Coord> visited = new HashSet<>();
        Queue<CoordInt> queue = new ArrayDeque<>();

        visited.add(head);
        queue.add(new CoordInt(head, 0));
        distances[head.x][head.y] = new CoordsInt(0);

        while (queue.size() > 0) {
            CoordInt curr = queue.poll();

            int newDist = curr.count + 1;

            Coord[] neighbors = gameState.getInBoardNeighbors(curr.coord, newDist);

            for (Coord neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    distances[neighbor.x][neighbor.y] = new CoordsInt(newDist, curr.coord);
                    queue.add(new CoordInt(neighbor, newDist));
                    visited.add(neighbor);
                } else {
                    final CoordsInt coordsInt = distances[neighbor.x][neighbor.y];
                    if (coordsInt.number == newDist) {
                        coordsInt.coords.add(curr.coord);
                    } else if (newDist < coordsInt.number) {
                        coordsInt.coords.clear();
                        coordsInt.coords.add(curr.coord);
                    }
                }
            }
        }
    }
}
