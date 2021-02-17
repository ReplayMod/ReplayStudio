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
package com.replaymod.replaystudio.pathing.property;

public abstract class AbstractPropertyPart<T> implements PropertyPart<T> {
    private final Property<T> property;
    private final boolean interpolatable;
    private final double upperBound;

    public AbstractPropertyPart(Property<T> property, boolean interpolatable) {
        this(property, interpolatable, Double.NaN);
    }

    public AbstractPropertyPart(Property<T> property, boolean interpolatable, double upperBound) {
        this.property = property;
        this.interpolatable = interpolatable;
        this.upperBound = upperBound;
    }

    @Override
    public Property<T> getProperty() {
        return property;
    }

    @Override
    public boolean isInterpolatable() {
        return interpolatable;
    }

    @Override
    public double getUpperBound() {
        return upperBound;
    }
}
