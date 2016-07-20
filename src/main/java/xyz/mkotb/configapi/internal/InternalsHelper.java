/*
 * Copyright (c) 2016, Mazen Kotb, mazenkotb@gmail.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package xyz.mkotb.configapi.internal;

import sun.reflect.ReflectionFactory;
import xyz.mkotb.configapi.internal.naming.NamingStrategies;
import xyz.mkotb.configapi.internal.naming.NamingStrategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class InternalsHelper {
    private InternalsHelper() {
    }

    public static Class typeOf(Field field, int index) {
        Type genericType = field.getGenericType();

        if (genericType instanceof ParameterizedType) {
            return (Class) ((ParameterizedType) genericType).getActualTypeArguments()[index];
        }

        return Object.class;
    }

    /*
     * Creates an instance of the class without calling it's constructor.
     * clazz must implement java.io.Serializable
     */
    public static <T> T newInstanceWithoutInit(Class<T> clazz) {
        try {
            ReflectionFactory rf =
                    ReflectionFactory.getReflectionFactory();
            Constructor objDef = Object.class.getDeclaredConstructor();
            Constructor intConstr = rf.newConstructorForSerialization(clazz, objDef);

            return clazz.cast(intConstr.newInstance());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create object", e);
        }
    }

    public static <T> T newInstance(Class<T> classOf) {
        try {
            return classOf.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            return null;
        }
    }

    public static <T> Map<String, Object> mapFieldsOf(T obj) {
        return mapFieldsOf(obj, NamingStrategies.from("dummy"));
    }

    public static <T> Map<String, Object> mapFieldsOf(T obj, NamingStrategy strategy) {
        Map<String, Object> fieldMap = new HashMap<>();
        Field[] fields = obj.getClass().getFields();

        for (Field field : fields) {
            fieldMap.put(strategy.rename(field.getName()), getField(field, obj));
        }

        return fieldMap;
    }

    public static Field staticFieldFor(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException ex) {
            return null;
        }
    }

    public static Field fieldFor(Object object, String name) {
        try {
            return object.getClass().getDeclaredField(name);
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }

    public static <T> T getField(Field field, Object instance) {
        try {
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (IllegalAccessException ex) {
            return null;
        }
    }

    public static boolean setField(String fieldName, Object instance, Object value) {
        Field field = fieldFor(instance, fieldName);
        return field != null && setField(field, instance, value);
    }

    public static boolean setField(Field field, Object instance, Object value) {
        try {
            field.setAccessible(true);
            field.set(instance, value);
            return true;
        } catch (IllegalAccessException ex) {
            return false;
        }
    }
}
