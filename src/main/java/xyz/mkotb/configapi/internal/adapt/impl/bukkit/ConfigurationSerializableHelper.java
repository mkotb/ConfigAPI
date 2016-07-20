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
package xyz.mkotb.configapi.internal.adapt.impl.bukkit;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import xyz.mkotb.configapi.internal.InternalsHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationSerializableHelper {
    private ConfigurationSerializableHelper() {
    }

    public static Collection<Class<? extends ConfigurationSerializable>> registeredClasses() {
        Map<String, Class<? extends ConfigurationSerializable>> aliases = InternalsHelper.getField(
                InternalsHelper.staticFieldFor(ConfigurationSerialization.class, "aliases"), null
        );
        return aliases.values();
    }

    public static boolean isRegistered(Class<?> cls) {
        return registeredClasses().contains(cls);
    }

    public static <T> T deserialize(Map<String, Object> map, Class<T> cls) {
        return cls.cast(ConfigurationSerialization.deserializeObject(map, cls.asSubclass(ConfigurationSerializable.class)));
    }

    public static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new HashMap<>();

        section.getKeys(false).forEach(key -> map.put(key, section.get(key)));

        return map;
    }
}
