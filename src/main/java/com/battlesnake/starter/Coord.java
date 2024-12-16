package com.battlesnake.starter;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

public class Coord {
    final int x;
    final int y;

    @Override
    public String toString() {
        return "Coord{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public Coord(final JsonNode jsonNode) {
        x = jsonNode.get("x").asInt();
        y = jsonNode.get("y").asInt();
    }

    public Coord(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    public Coord[] getNeighbors() {
        return new Coord[]{new Coord(this.x + 1, this.y), new Coord(this.x - 1, this.y), new Coord(this.x, this.y + 1), new Coord(this.x, this.y - 1)};
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Coord coord = (Coord) o;
        return x == coord.x && y == coord.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    int dist(Coord other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    boolean isNeighbour(final Coord other) {
        return dist(other) == 1;
    }
}
