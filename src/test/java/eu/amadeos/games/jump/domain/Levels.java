package eu.amadeos.games.jump.domain;

import eu.amadeos.gameengine.tiled.TiledMap;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

/**
 * Small levels for tests, drawn as text and turned into the TMX that Tiled would save: {@code #} ground,
 * {@code =} platform, {@code ^} spikes, {@code ~} water, {@code o} a coin; {@code P} the start,
 * {@code G} the goal, {@code C} a checkpoint, {@code J} a spring, {@code b} a beetle walking left,
 * {@code v} a bird.
 */
final class Levels {
    private Levels() {
    }

    static Level of(String... rows) {
        int width = rows[0].length();
        int height = rows.length;
        StringJoiner data = new StringJoiner(",");
        StringBuilder objects = new StringBuilder();
        int id = 1;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                char c = rows[row].charAt(col);
                data.add(switch (c) {
                    case '#' -> "1";
                    case '=' -> "2";
                    case '^' -> "3";
                    case '~' -> "4";
                    case 'o' -> "5";
                    default -> "0";
                });
                String type = switch (c) {
                    case 'P' -> "start";
                    case 'G' -> "goal";
                    case 'C' -> "checkpoint";
                    case 'J' -> "spring";
                    case 'b' -> "beetle";
                    case 'v' -> "bird";
                    default -> null;
                };
                if (type != null) {
                    objects.append("<object id=\"%d\" type=\"%s\" x=\"%d\" y=\"%d\" width=\"16\" height=\"16\"/>\n"
                            .formatted(id++, type, col * 16, row * 16));
                }
            }
        }
        String tmx = """
                <?xml version="1.0" encoding="UTF-8"?>
                <map orientation="orthogonal" width="%d" height="%d" tilewidth="16" tileheight="16">
                 <tileset firstgid="1" name="test" tilewidth="16" tileheight="16" tilecount="5" columns="5">
                  <tile id="0" type="solid"/><tile id="1" type="platform"/><tile id="2" type="spikes"/>
                  <tile id="3" type="water"/><tile id="4" type="coin"/>
                 </tileset>
                 <layer name="ground" width="%d" height="%d"><data encoding="csv">%s</data></layer>
                 <objectgroup name="things">
                %s </objectgroup>
                </map>
                """.formatted(width, height, width, height, data, objects);
        return Level.read(1, TiledMap.parse("test.tmx", new ByteArrayInputStream(tmx.getBytes(StandardCharsets.UTF_8)),
                file -> null));
    }
}
