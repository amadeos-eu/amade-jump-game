package eu.amadeos.games.jump.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelTest {
    // --8<-- [start:test-reach]
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void theGoalAndEveryCoinCanBeReached(int number) {
        Level level = Level.load(number);
        Reach reach = Reach.of(level);
        assertTrue(reach.goal, () -> level.name() + ": the goal cannot be reached\n" + reach.picture());
        assertEquals(level.coins(), reach.coins.size(), () -> level.name() + ": coins out of reach (?)\n" + reach.picture());
    }
    // --8<-- [end:test-reach]
}
