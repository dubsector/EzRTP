package org.bukkit;

import org.bukkit.block.Biome;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;

/** Minimal Registry stub for tests. */
public class Registry<T> implements Iterable<T> {
    public static Registry<Object> STRUCTURE_TYPE = new Registry<>();

    public static final Registry<Biome> BIOME = new Registry<Biome>() {
        @Override
        public Biome get(NamespacedKey key) {
            String raw = key.getKey(); // e.g. "minecraft:sulfur_caves"
            String name = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
            // Biome.valueOf in the test shim returns null on miss (no throw)
            return Biome.valueOf(name.toUpperCase(Locale.ROOT));
        }
        @Override
        public Iterator<Biome> iterator() {
            return Arrays.asList(Biome.values()).iterator();
        }
    };

    public Registry() {}

    public T get(NamespacedKey key) {
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return java.util.Collections.emptyIterator();
    }
}