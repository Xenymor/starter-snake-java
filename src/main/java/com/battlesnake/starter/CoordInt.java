package com.battlesnake.starter;

import java.util.Objects;

public class CoordInt {
    Coord coord;
    int count;

    public CoordInt(final Coord coord, final int count) {
        this.coord = coord;
        this.count = count;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CoordInt coordInt = (CoordInt) o;
        return count == coordInt.count && Objects.equals(coord, coordInt.coord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coord, count);
    }
}
