package org.bukkit;

import org.bukkit.block.Biome;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

/** Minimal Registry stub for tests — must be an interface to match Paper API. */
public interface Registry<T> extends Iterable<T> {
    Registry<Object> STRUCTURE_TYPE = new Registry<Object>() {
        @Override public Object get(NamespacedKey key) { return null; }
        @Override public Iterator<Object> iterator() { return java.util.Collections.emptyIterator(); }
    };

    Registry<Biome> BIOME = new Registry<Biome>() {
        @Override
        public Biome get(NamespacedKey key) {
            String raw = key.getKey(); // e.g. "minecraft:sulfur_caves"
            String name = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
            return Biome.valueOf(name.toUpperCase(Locale.ROOT));
        }
        @Override
        public Iterator<Biome> iterator() {
            return Arrays.asList(Biome.values()).iterator();
        }
    };

    T get(NamespacedKey key);
}
