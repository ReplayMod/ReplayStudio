/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.replaymod.replaystudio.util;

import java.util.Objects;

/**
 * Position with integer components and dimension id.
 */
public class IGlobalPosition {

    private final String dimension;
    private final IPosition position;

    public IGlobalPosition(String dimension, IPosition position) {
        this.dimension = dimension;
        this.position = position;
    }

    public String getDimension() {
        return dimension;
    }

    public IPosition getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IGlobalPosition that = (IGlobalPosition) o;
        return dimension.equals(that.dimension) && position.equals(that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, position);
    }

    @Override
    public String toString() {
        return "IGlobalPosition{" +
                "dimension='" + dimension + '\'' +
                ", position=" + position +
                '}';
    }
}
