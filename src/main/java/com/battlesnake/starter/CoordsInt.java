package com.battlesnake.starter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CoordsInt {
    final List<Coord> coords;
    final int number;

    public CoordsInt(final int number, final Coord... coords) {
        this.coords = new ArrayList<>();
        this.coords.addAll(List.of(coords));
        this.number = number;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CoordsInt coordsInt = (CoordsInt) o;
        return number == coordsInt.number && Objects.equals(coords, coordsInt.coords);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coords, number);
    }
}
