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
package xyz.mkotb.configapi.internal.adapt.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.MemorySection;
import xyz.mkotb.configapi.ex.InternalProcessingException;
import xyz.mkotb.configapi.internal.InternalsHelper;
import xyz.mkotb.configapi.internal.SerializableMemorySection;
import xyz.mkotb.configapi.internal.adapt.AdapterHandler;
import xyz.mkotb.configapi.internal.adapt.ObjectAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionAdapter<E> implements ObjectAdapter<Collection, Object> {
    private final Class<E> type;
    private final Class<? extends Collection> implementationClass;
    private final AdapterHandler handler;

    private CollectionAdapter(Class<E> type, Class<? extends Collection> collectionClass, AdapterHandler handler) {
        this.type = type;
        this.implementationClass = findImplementation(collectionClass);
        this.handler = handler;
    }

    public static <E> CollectionAdapter<E> create(Class<E> type, Class<? extends Collection> collectionClass, AdapterHandler handler) {
        return new CollectionAdapter<>(type, collectionClass, handler);
    }

    private static Class<? extends Collection> findImplementation(Class<? extends Collection> cls) {
        if (List.class.isAssignableFrom(cls)) {
            return ArrayList.class;
        } else if (Set.class.isAssignableFrom(cls)) {
            return HashSet.class;
        } else if (Queue.class.isAssignableFrom(cls)) {
            return PriorityQueue.class;
        }

        return cls;
    }

    @Override
    public Collection read(String key, ConfigurationSection section) {
        Collection collection;
        List<?> originalList;

        if (section.isConfigurationSection(key)) {
            originalList = new ArrayList<>(section.getConfigurationSection(key).getValues(false).values());
        } else {
            originalList = section.getList(key);
        }

        try {
            collection = implementationClass.getDeclaredConstructor(int.class).newInstance(originalList.size());
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new InternalProcessingException("Could not create new instance of " + implementationClass.getName(), e);
        }

        for (Object obj: originalList) {
            ConfigurationSection dummySection = new MemoryConfiguration();

            dummySection.set("dummy", obj);
            collection.add(handler.adaptIn(dummySection, "dummy", type));
        }

        return collection;
    }

    @Override // can't use streams due to abstraction
    public Object write(Collection collection) {
        if (!AdapterHandler.isSerializable(type) && !type.isPrimitive()) {
            MemorySection section = InternalsHelper.newInstanceWithoutInit(SerializableMemorySection.class);
            int i = 0;

            for (Object o : collection) {
                section.set(String.valueOf(++i), handler.adaptOut(o, AdapterHandler.outClass(type)));
            }

            return section;
        }

        List<Object> list = new ArrayList<>(collection.size());

        for (Object o : collection) {
            list.add(handler.adaptOut(o, AdapterHandler.outClass(type)));
        }

        return list;
    }
}