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

import com.replaymod.replaystudio.util.I18n;

/**
 * Abstract base class for most properties.
 */
public abstract class AbstractProperty<T> implements Property<T> {
    private final String id, localizationKey;
    private final PropertyGroup propertyGroup;
    private final T initialValue;

    public AbstractProperty(String id, String localizationKey, PropertyGroup propertyGroup, T initialValue) {
        this.id = id;
        this.localizationKey = localizationKey;
        this.propertyGroup = propertyGroup;
        this.initialValue = initialValue;

        if (propertyGroup != null) {
            propertyGroup.getProperties().add(this);
        }
    }

    @Override
    public String getLocalizedName() {
        return I18n.format(localizationKey);
    }

    @Override
    public PropertyGroup getGroup() {
        return propertyGroup;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public T getNewValue() {
        return initialValue;
    }
}
