/*
 * Copyright (c) 2023
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

package com.replaymod.replaystudio.protocol.data;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;

public class StringOrNbtText {
    public String str; // 1.20.2-
    public CompoundTag nbt; // 1.20.3+

    public StringOrNbtText(String str) {
        this.str = str;
    }

    public StringOrNbtText(CompoundTag nbt) {
        this.nbt = nbt;
    }
}
