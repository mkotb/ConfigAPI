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
package xyz.mkotb.configapi.internal.adapt;

import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import xyz.mkotb.configapi.RequiredField;
import xyz.mkotb.configapi.ex.ClassStructureException;
import xyz.mkotb.configapi.ex.InvalidConfigurationException;
import xyz.mkotb.configapi.internal.InternalsHelper;
import xyz.mkotb.configapi.internal.SerializableMemorySection;
import xyz.mkotb.configapi.internal.adapt.impl.*;
import xyz.mkotb.configapi.internal.adapt.impl.atomic.*;
import xyz.mkotb.configapi.internal.adapt.impl.bukkit.*;
import xyz.mkotb.configapi.internal.naming.NamingStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

public final class AdapterHandler {
    private static final Map<Class<?>, ObjectAdapter<?, ?>> ADAPTERS = new ConcurrentHashMap<>();

    static {
        ADAPTERS.put(Date.class, new DateAdapter());
        ADAPTERS.put(java.sql.Date.class, new SQLDateAdapter());

        ADAPTERS.put(Color.class, new ColorAdapter());
        ADAPTERS.put(ConfigurationSection.class, new ConfigurationSectionAdapter());
        ADAPTERS.put(ItemStack.class, new ItemStackAdapter());
        ADAPTERS.put(OfflinePlayer.class, new OfflinePlayerAdapter());
        ADAPTERS.put(Vector.class, new VectorAdapter());

        ADAPTERS.put(AtomicBoolean.class, new AtomicBooleanAdapter());
        ADAPTERS.put(AtomicInteger.class, new AtomicIntegerAdapter());
        ADAPTERS.put(AtomicIntegerArray.class, new AtomicIntegerArrayAdapter());
        ADAPTERS.put(AtomicLong.class, new AtomicLongAdapter());
        ADAPTERS.put(AtomicLongArray.class, new AtomicLongArrayAdapter());
    }

    private final NamingStrategy namingStrategy;

    private AdapterHandler(NamingStrategy strategy) {
        this.namingStrategy = strategy;
    }

    public static AdapterHandler create(NamingStrategy strategy) {
        return new AdapterHandler(strategy);
    }

    public <I, O> O adaptOut(I input, Class<O> outClass) {
        if (Collection.class.isAssignableFrom(outClass)) {
            CollectionAdapter adapter = CollectionAdapter.create((Class) (((ParameterizedType) outClass.getGenericSuperclass()).getActualTypeArguments()[0]),
                    (Class<? extends Collection>) outClass, this);
            return outClass.cast(adapter.write((Collection) input));
        }

        if (Map.class.isAssignableFrom(outClass)) {
            MapAdapter adapter = MapAdapter.create(((Class) (((ParameterizedType) outClass.getGenericSuperclass()).getActualTypeArguments()[1])),
                    this);
            return outClass.cast(adapter.write((Map) input));
        }

        if (outClass.isArray()) {
            ArrayAdapter adapter = ArrayAdapter.create(outClass.getComponentType(), this);
            return outClass.cast(adapter.write(input));
        }

        if (outClass.isPrimitive() && input.getClass().isPrimitive() && outClass == input.getClass()) {
            return outClass.cast(input);
        }

        if (input instanceof String && outClass == String.class) {
            return outClass.cast(input);
        }

        ObjectAdapter<?, ?> oldAdapter = ADAPTERS.get(input.getClass());

        if (oldAdapter == null) {
            MemorySection section = InternalsHelper.newInstanceWithoutInit(SerializableMemorySection.class,
                    MemorySection.class);
            Field[] fields = input.getClass().getFields();

            for (Field field : fields) {
                Object value = InternalsHelper.getField(field, input);

                if (value != null)
                    section.set(namingStrategy.rename(field.getName()), adaptOut(value, field.getDeclaringClass()));
            }

            return outClass.cast(section);
        }

        ObjectAdapter<I, O> adapter;

        try {
            adapter = (ObjectAdapter<I, O>) oldAdapter;
        } catch (ClassCastException ex) {
            throw new ClassStructureException(outClass.getName() + " does not match registered adapter" +
                    " for " + input.getClass().getName() + "!");
        }

        return adapter.write(input);
    }

    public <I, O> I adaptIn(ConfigurationSection section, String key, Class<I> inClass) {
        if (inClass.isArray()) {
            ArrayAdapter adapter = ArrayAdapter.create(inClass.getComponentType(), this);
            return inClass.cast(adapter.read(key, section));
        }

        if (Collection.class.isAssignableFrom(inClass)) {
            CollectionAdapter adapter = CollectionAdapter.create((Class) (((ParameterizedType) inClass.getGenericSuperclass()).getActualTypeArguments()[0]),
                    (Class<? extends Collection>) inClass, this);
            return inClass.cast(adapter.read(key, section));
        }

        if (Map.class.isAssignableFrom(inClass)) {
            MapAdapter adapter = MapAdapter.create(((Class) (((ParameterizedType) inClass.getGenericSuperclass()).getActualTypeArguments()[1])),
                    this);
            return inClass.cast(adapter.read(key, section));
        }

        if (inClass.isPrimitive() || inClass == String.class) {
            return inClass.cast(section.get(key));
        }

        ObjectAdapter<?, ?> oldAdapter = ADAPTERS.get(inClass);

        if (oldAdapter == null) {
            ConfigurationSection readingSection = (key == null) ? section : section.getConfigurationSection(key);
            I instance = InternalsHelper.newInstance(inClass);
            Field[] fields = inClass.getClass().getFields();

            for (Field field : fields) {
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }

                String name = namingStrategy.rename(field.getName());

                if (!readingSection.contains(name)) {
                    if (field.getAnnotation(RequiredField.class) != null) {
                        String message = "Could not find the required field, " + name;

                        if (key != null) {
                            message += ", in section " + key;
                        }

                        throw new InvalidConfigurationException(message);
                    }

                    continue;
                }

                InternalsHelper.setField(field, instance, adaptIn(readingSection, name, field.getDeclaringClass()));
            }

            return instance;
        }

        ObjectAdapter<I, O> adapter;

        try {
            adapter = (ObjectAdapter<I, O>) oldAdapter;
        } catch (ClassCastException ex) {
            throw new ClassStructureException(ex);
        }

        return adapter.read(key, section);
    }
}
