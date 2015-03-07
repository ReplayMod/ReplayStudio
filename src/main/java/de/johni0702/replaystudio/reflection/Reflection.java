package de.johni0702.replaystudio.reflection;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class Reflection {

    @SneakyThrows
    public static void setField(Class<?> cls, String fieldName, Object obj, Object content) {
        Field field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);

        if ((field.getModifiers() & Modifier.FINAL) != 0) {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }

        field.set(obj, content);
    }

}
