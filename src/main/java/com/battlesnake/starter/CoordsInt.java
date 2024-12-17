package com.battlesnake.starter;

import java.util.ArrayList;
import java.util.List;

public class CoordsInt {
    final List<Coord> coords;
    final int number;

    public CoordsInt(final int number, final Coord... coords) {
        this.coords = new ArrayList<>();
        this.coords.addAll(List.of(coords));
        this.number = number;
    }
}
