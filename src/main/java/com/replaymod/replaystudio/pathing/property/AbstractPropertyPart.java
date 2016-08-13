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
