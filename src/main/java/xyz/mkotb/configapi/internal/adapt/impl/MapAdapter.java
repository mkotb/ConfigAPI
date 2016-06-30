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

import java.util.HashMap;
import java.util.Map;

public class MapAdapter<V> implements ObjectAdapter<Map, ConfigurationSection> {
    private final Class<V> valueClass;
    private final AdapterHandler handler;

    private MapAdapter(Class<V> valueClass, AdapterHandler handler) {
        this.valueClass = valueClass;
        this.handler = handler;
    }

    public static <V> MapAdapter<V> create(Class<V> valueClass, AdapterHandler handler) {
        return new MapAdapter<>(valueClass, handler);
    }

    @Override
    public Map read(String key, ConfigurationSection section) {
        ConfigurationSection originalMap = section.getConfigurationSection(key);
        Map map = new HashMap();

        originalMap.getValues(false).forEach((k, v) -> {
            ConfigurationSection dummySection = new MemoryConfiguration();

            dummySection.set("dummy", v);
            map.put(k, handler.adaptIn(dummySection, "dummy", valueClass));
        });

        return map;
    }

    @Override
    public ConfigurationSection write(Map obj) {
        MemorySection memorySection = InternalsHelper.newInstanceWithoutInit(SerializableMemorySection.class,
                MemorySection.class);
        obj.forEach((k, v) -> memorySection.set(k.toString(), handler.adaptOut(v, valueClass)));
        return memorySection;
    }
}
