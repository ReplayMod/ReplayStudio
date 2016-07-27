package com.replaymod.replaystudio.pathing.impl;

import com.replaymod.replaystudio.pathing.path.Keyframe;
import com.replaymod.replaystudio.pathing.property.Property;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

@RequiredArgsConstructor
public class KeyframeImpl implements Keyframe {
    private final long time;
    private final Map<Property, Object> properties = new HashMap<>();

    @Override
    public long getTime() {
        return time;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getValue(Property<T> property) {
        return properties.containsKey(property) ? Optional.of((T) properties.get(property)) : Optional.empty();
    }

    @Override
    public <T> void setValue(Property<T> property, T value) {
        properties.put(property, value);
    }

    @Override
    public void removeProperty(Property property) {
        properties.remove(property);
    }

    @Override
    public Set<Property> getProperties() {
        return Collections.unmodifiableSet(properties.keySet());
    }
}
