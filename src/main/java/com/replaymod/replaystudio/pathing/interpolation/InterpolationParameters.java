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
package com.replaymod.replaystudio.pathing.interpolation;

import lombok.Data;

/**
 * Parameters which can be used to make an interpolation more fitting to the previous one.
 */
@Data
public class InterpolationParameters {
    /**
     * The final value.
     * Should be the same as for the final property.
     * If it differs though, this value should be preferred.
     */
    private final double value;

    /**
     * Velocity at the end of the interpolation.
     * Normally this is the first derivative of the interpolating function at the end of the interpolation.
     */
    private final double velocity;

    /**
     * Acceleration at the end of the interpolation.
     * Normally this is the second derivative of the interpolating function at the end of the interpolation.
     */
    private final double acceleration;
}
