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
package xyz.mkotb.configapi;

import org.bukkit.ChatColor;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mkotb.configapi.comment.CommentHelper;
import xyz.mkotb.configapi.ex.ClassStructureException;
import xyz.mkotb.configapi.ex.InternalProcessingException;
import xyz.mkotb.configapi.internal.InternalsHelper;
import xyz.mkotb.configapi.internal.SerializableMemorySection;
import xyz.mkotb.configapi.internal.adapt.AdapterHandler;
import xyz.mkotb.configapi.internal.dummy.CentralDummyHolder;
import xyz.mkotb.configapi.internal.naming.CamelCaseNamingStrategy;
import xyz.mkotb.configapi.internal.naming.NamingStrategy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ConfigFactory {
    private volatile NamingStrategy namingStrategy = new CamelCaseNamingStrategy();
    private final JavaPlugin plugin;

    private ConfigFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        File directory = plugin.getDataFolder();

        if (directory.exists()) {
            directory.mkdirs();
        }
    }

    public static ConfigFactory newFactory(JavaPlugin plugin) {
        return new ConfigFactory(plugin);
    }

    public <T> T fromFile(String name, Class<T> classOf) {
        return fromFile(new File(configDirectory(), name + ".yml"), classOf);
    }

    public <T> T fromFile(File config, Class<T> classOf) {
        if (!config.exists()) {
            CentralDummyHolder dummyHolder = CentralDummyHolder.instance();
            T dummy;

            if ((dummy = dummyHolder.dummyFrom(classOf)) == null) {
                dummy = InternalsHelper.newInstance(classOf);

                if (dummy == null) {
                    throw new ClassStructureException("An accessible no-args constructor could not " +
                            "be found in " + classOf.getName());
                }
            }

            colourizeFields(dummy);
            save(config, dummy);
            dummyHolder.insertDummy(classOf, dummy);
            return dummy;
        }

        AdapterHandler handler = AdapterHandler.create(namingStrategy);
        FileConfiguration data = YamlConfiguration.loadConfiguration(config);
        return colourizeFields(handler.adaptIn(data, null, classOf));
    }

    public <T> void save(String name, T object) {
        save(new File(configDirectory(), name + ".yml"), object);
    }

    public <T> void save(File config, T object) {
        if (!config.exists()) {
            try {
                config.getParentFile().mkdirs();
                config.createNewFile();
            } catch (IOException ex) {
                throw new InternalProcessingException("Could not create file!", ex);
            }
        }

        StringBuilder sb = new StringBuilder();
        Map<String, String[]> comments = CommentHelper.extractComments(object, namingStrategy);
        String[] header = CommentHelper.extractHeader(object.getClass());
        AdapterHandler handler = AdapterHandler.create(namingStrategy);
        MemorySection section = handler.adaptOut(object, MemorySection.class);

        if (header != null) {
            CommentHelper.encodeComments(header, sb);
        }

        ((SerializableMemorySection) section).map().forEach((k, v) -> {
            FileConfiguration fieldData = new YamlConfiguration();

            if (comments.containsKey(k)) {
                CommentHelper.encodeComments(comments.get(k), sb);
            }

            fieldData.set(k, v);
            sb.append(fieldData.saveToString());
        });

        try (PrintWriter out = new PrintWriter(config)){
            out.print(sb.toString());
        } catch (IOException ex) {
            throw new InternalProcessingException("Unable to save config to file!", ex);
        }
    }

    private <T> T colourizeFields(T object) {
        for (Field field : object.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(Coloured.class)) {
                continue;
            }

            if (field.getType() == String.class) {
                Coloured annotation = field.getDeclaredAnnotation(Coloured.class);
                String value = InternalsHelper.getField(field, object);

                if (value != null) {
                    InternalsHelper.setField(field, object, AdapterHandler.translateAlternateColorCodes(
                            ChatColor.COLOR_CHAR, annotation.value(), value));
                }
            } else if (field.getType() == List.class) {
                Class<?> genericType = (Class<?>) ((ParameterizedType) field.getGenericType())
                        .getActualTypeArguments()[0];

                if (genericType.equals(String.class)) {
                    Coloured annotation = field.getDeclaredAnnotation(Coloured.class);
                    List<String> value = InternalsHelper.getField(field, object);

                    if (value != null) {
                        List<String> translated = value
                                .stream()
                                .map(s -> AdapterHandler.translateAlternateColorCodes(
                                        ChatColor.COLOR_CHAR, annotation.value(), s
                                )).collect(Collectors.toList());

                        InternalsHelper.setField(field, object, translated);
                    }
                }
            }
        }

        return object;
    }

    private File configDirectory() {
        return plugin.getDataFolder();
    }

    public NamingStrategy namingStrategy() {
        return namingStrategy;
    }

    public void setPreferredStrategy(NamingStrategy preferredStrategy) {
        this.namingStrategy = preferredStrategy;
    }
}
