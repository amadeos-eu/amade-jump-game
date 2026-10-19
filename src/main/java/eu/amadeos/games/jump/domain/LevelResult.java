package eu.amadeos.games.jump.domain;

/** How a level was finished: in how many seconds, with how many of its coins, after how many falls. */
public record LevelResult(int level, double seconds, int coins, int totalCoins, int deaths) {
    public boolean allCoins() {
        return coins == totalCoins;
    }
}
