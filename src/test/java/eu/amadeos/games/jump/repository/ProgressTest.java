package eu.amadeos.games.jump.repository;

import eu.amadeos.gameengine.save.SaveFile;
import eu.amadeos.games.jump.domain.LevelResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressTest {
    @TempDir
    Path dir;

    // --8<-- [start:test-progress]
    @Test
    void aFinishedLevelOpensTheNextAndIsRememberedAfterARestart() {
        SaveFile file = new SaveFile(dir.resolve("progress.save"));
        Progress progress = new Progress(file);
        assertEquals(1, progress.open());
        assertTrue(progress.best(1).isEmpty());

        Progress.Record first = progress.finished(new LevelResult(1, 80.5, 30, 50, 2), 3);
        assertTrue(first.bestTime() && first.mostCoins() && first.opened());

        Progress later = new Progress(file);             // the next start of the game
        assertEquals(2, later.open());
        assertEquals(80.5, later.best(1).orElseThrow(), 1e-9);
        assertEquals(30, later.coins(1));

        Progress.Record slower = later.finished(new LevelResult(1, 95, 40, 50, 0), 3);
        assertFalse(slower.bestTime(), "slower is no record");
        assertTrue(slower.mostCoins(), "but more coins are");
        assertFalse(slower.opened(), "level 2 was open already");
        assertEquals(80.5, new Progress(file).best(1).orElseThrow(), 1e-9);
    }
    // --8<-- [end:test-progress]

    @Test
    void theLastLevelOpensNothing() {
        Progress progress = new Progress(new SaveFile(dir.resolve("progress.save")));
        progress.finished(new LevelResult(1, 50, 0, 10, 0), 3);
        progress.finished(new LevelResult(2, 50, 0, 10, 0), 3);
        assertFalse(progress.finished(new LevelResult(3, 50, 0, 10, 0), 3).opened());
        assertEquals(3, progress.open());
    }
}
