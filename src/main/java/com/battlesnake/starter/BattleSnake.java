package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

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
}
