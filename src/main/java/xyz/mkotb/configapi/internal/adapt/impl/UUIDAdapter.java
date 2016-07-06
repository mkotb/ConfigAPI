package xyz.mkotb.configapi.internal.adapt.impl;

import org.bukkit.configuration.ConfigurationSection;
import xyz.mkotb.configapi.internal.adapt.ObjectAdapter;

import java.util.UUID;

public class UUIDAdapter implements ObjectAdapter<UUID, String> {
    @Override
    public UUID read(String key, ConfigurationSection section) {
        return UUID.fromString(section.getString(key));
    }

    @Override
    public String write(UUID obj) {
        return obj.toString();
    }
}
