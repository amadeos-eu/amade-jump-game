package eu.amadeos.games.jump.repository;

import com.google.inject.Inject;
import eu.amadeos.commons.config.XdgDirectories;
import eu.amadeos.gameengine.save.SaveFile;
import eu.amadeos.games.jump.domain.LevelResult;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * How far the player has come, in {@code ~/.local/share/amadeos/games/jump/progress.save}: the levels that
 * are open, and for each finished one the best time and the most coins. Written as a {@link SaveFile},
 * so a crash in the middle of saving loses nothing, and read back at every start.
 *
 * <pre>
 * amade-save 1
 * level.1.best=71.258
 * level.1.coins=48
 * open=2
 * crc32 5e1f0c2a
 * </pre>
 */
// --8<-- [start:progress]
public final class Progress {
    /** The version of the format; a later game that changes it can still read version 1. */
    static final int VERSION = 1;

    /** What a finished level brought: a new best time, more coins than before, a level opened. */
    public record Record(boolean bestTime, boolean mostCoins, boolean opened) {
    }

    private final SaveFile file;
    private final Map<String, String> values = new HashMap<>();

    @Inject
    public Progress(XdgDirectories xdg) {
        this(SaveFile.of(xdg, "jump", "progress"));
    }

    public Progress(SaveFile file) {
        this.file = file;
        file.load().ifPresent(saved -> values.putAll(saved.values()));
    }

    /** The highest level that may be played; at least the first. */
    public int open() {
        return Math.max(1, number("open", 1));
    }

    /** The best time of a level in seconds; empty if it was never finished. */
    public OptionalDouble best(int level) {
        String value = values.get("level." + level + ".best");
        if (value == null) return OptionalDouble.empty();
        try {
            return OptionalDouble.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /** The most coins ever brought to the goal of a level. */
    public int coins(int level) {
        return number("level." + level + ".coins", 0);
    }

    /** Enters a finished level, opens the next one of {@code levels}, and saves at once. */
    public Record finished(LevelResult result, int levels) {
        int level = result.level();
        OptionalDouble best = best(level);
        boolean bestTime = best.isEmpty() || result.seconds() < best.getAsDouble();
        boolean mostCoins = result.coins() > coins(level);
        boolean opened = level < levels && open() <= level;
        if (bestTime) values.put("level." + level + ".best", String.format(Locale.ROOT, "%.3f", result.seconds()));
        if (mostCoins) values.put("level." + level + ".coins", Integer.toString(result.coins()));
        if (opened) values.put("open", Integer.toString(level + 1));
        if (bestTime || mostCoins || opened) file.store(VERSION, values);
        return new Record(bestTime, mostCoins, opened);
    }
    // --8<-- [end:progress]

    private int number(String key, int fallback) {
        try {
            return Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
