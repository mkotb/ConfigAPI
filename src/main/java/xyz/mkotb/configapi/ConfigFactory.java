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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.mkotb.configapi.ex.ClassStructureException;
import xyz.mkotb.configapi.ex.InternalProcessingException;
import xyz.mkotb.configapi.internal.InternalsHelper;
import xyz.mkotb.configapi.internal.SerializableMemorySection;
import xyz.mkotb.configapi.internal.adapt.AdapterHandler;
import xyz.mkotb.configapi.internal.dummy.CentralDummyHolder;
import xyz.mkotb.configapi.internal.naming.NamingStrategies;
import xyz.mkotb.configapi.internal.naming.NamingStrategy;

import java.io.File;
import java.io.IOException;

public final class ConfigFactory {
    private volatile NamingStrategy namingStrategy = NamingStrategies.from("camelcase");
    private final JavaPlugin plugin;

    private ConfigFactory(JavaPlugin plugin) {
        this.plugin = plugin;
        File directory = configDirectory();

        if (directory.exists()) {
            directory.mkdirs();
        }
    }

    public static ConfigFactory newFactory(JavaPlugin plugin) {
        return new ConfigFactory(plugin);
    }

    public <T> T fromFile(String name, Class<T> classOf) {
        File config = new File(configDirectory(), name + ".yml");

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

            save(name, dummy);
            dummyHolder.insertDummy(classOf, dummy);
            return dummy;
        }

        return null;
    }

    public <T> void save(String name, T object) {
        File config = new File(configDirectory(), name + ".yml");

        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException ex) {
                throw new InternalProcessingException("Could not create file!", ex);
            }
        }

        FileConfiguration data = YamlConfiguration.loadConfiguration(config);
        AdapterHandler handler = AdapterHandler.create(namingStrategy);
        ConfigurationSection section = handler.adaptOut(object, ConfigurationSection.class);

        ((SerializableMemorySection) section).map().forEach(data::set);

        try {
            data.save(config);
        } catch (IOException ex) {
            throw new InternalProcessingException("Unable to save config to file!", ex);
        }
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
