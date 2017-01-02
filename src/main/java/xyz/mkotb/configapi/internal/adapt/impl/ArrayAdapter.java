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
import xyz.mkotb.configapi.internal.InternalsHelper;
import xyz.mkotb.configapi.internal.SerializableMemorySection;
import xyz.mkotb.configapi.internal.adapt.AdapterHandler;
import xyz.mkotb.configapi.internal.adapt.ObjectAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class ArrayAdapter<E> implements ObjectAdapter<Object, Object> {
    private final Class<E> type;
    private final AdapterHandler handler;

    private ArrayAdapter(Class<E> type, AdapterHandler handler) {
        this.type = type;
        this.handler = handler;
    }

    public static ArrayAdapter create(Class<?> type, AdapterHandler handler) {
        return new ArrayAdapter(type, handler);
    }

    @Override
    public Object read(String key, ConfigurationSection section) {
        List<?> list;

        if (section.isConfigurationSection(key)) {
            list = new ArrayList<>(section.getConfigurationSection(key).getValues(false).values());
        } else {
            list = section.getList(key);
        }

        Object array = Array.newInstance(type, list.size());

        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            ConfigurationSection dummySection = new MemoryConfiguration();

            dummySection.set("dummy", obj);
            Array.set(array, i, handler.adaptIn(dummySection, "dummy", obj.getClass()));
        }

        return array;
    }

    @Override
    public Object write(Object obj) {
        int length = Array.getLength(obj);

        if (!AdapterHandler.isSerializable(type) && !type.isPrimitive()) {
            MemorySection section = InternalsHelper.newInstanceWithoutInit(SerializableMemorySection.class);

            for (int i = 0; i < length; i++) {
                section.set(String.valueOf(i + 1), handler.adaptOut(Array.get(obj, i), AdapterHandler.outClass(type)));
            }

            return section;
        }

        List<Object> list = new ArrayList<>(length);

        for (int i = 0; i < length; i++) {
            Object object = Array.get(obj, i);

            if (type.isPrimitive() || type == String.class) {
                list.add(object);
                continue;
            }

            list.add(handler.adaptOut(object, AdapterHandler.outClass(type)));
        }

        return list;
    }
}
