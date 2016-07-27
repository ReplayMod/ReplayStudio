package com.replaymod.replaystudio.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Reflection utilities.
 */
public class Reflection {

    /**
     * Sets a private final field to the new value
     * @param cls The class which declares the field
     * @param fieldName The name of the field
     * @param obj The object or {@code null} if the field is static
     * @param value The new value to set the field to
     */
    public static void setField(Class<?> cls, String fieldName, Object obj, Object value) {
        try {
            Field field = cls.getDeclaredField(fieldName);
            field.setAccessible(true);

            if ((field.getModifiers() & Modifier.FINAL) != 0) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            }

            field.set(obj, value);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

}
