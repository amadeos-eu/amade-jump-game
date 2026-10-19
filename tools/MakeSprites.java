import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

/**
 * Draws the two pictures of Jump: the tileset {@code tiles.png} that the levels are painted with in Tiled,
 * and the sprite sheet {@code sprites.png} of the figures. Pixel art of 16 × 16 pixels per cell. The
 * figures are written below as text, one letter per pixel; most tiles are worked out from a few rules and
 * a fixed random generator, so that dirt and rock look rough but the same every time.
 *
 * <pre>
 * java tools/MakeSprites.java src/main/resources/eu/amadeos/games/jump
 * </pre>
 *
 * The tileset, 8 columns of 4 rows: 0 grass, 1 dirt, 2 and 3 grass with a corner to the left and right,
 * 4 stone, 5 brick, 6 cave floor, 7 cave rock; 8-10 a wooden platform (left, middle, right), 11 spikes,
 * 12-15 a coin turning; 16-23 things that only stand about (bush, flower, tall grass, fence, mushroom,
 * pebble, crystal, vine); 24 and 25 water (two waves), 26 deep water, 27 a cloud to stand on, 28 a sign,
 * 29 cave wall, 30 grass with both corners. Which tile is solid, a platform or deadly is said in
 * {@code levels/tiles.tsx}, not here.
 *
 * <p>The sprite sheet: row 0 the player looking right (standing, three steps of running, jumping,
 * falling, hit, blinking), row 1 the same looking left; row 2 the beetle (two steps, flat), the bird (wings
 * up and down, hit), the spring (up, pressed), row 3 the same looking left; row 4 a pole, the checkpoint's
 * flag (down, up and waving), the goal's flag (two waves) and the glitter of a coin. The files are part of
 * the repository; run this again only to change the pictures.
 */
public class MakeSprites {
    static final int CELL = 16;

    static final Map<Character, Integer> PALETTE = Map.ofEntries(
            Map.entry('.', 0x00000000),
            Map.entry('K', 0xFF1C1A2C),     // outline
            Map.entry('W', 0xFFF4F4F0),     // white
            Map.entry('S', 0xFFF6C49A),     // skin
            Map.entry('s', 0xFFC8865E),     // skin, shaded
            Map.entry('R', 0xFFE04040),     // red
            Map.entry('r', 0xFF902838),     // dark red
            Map.entry('B', 0xFF3C64D8),     // blue
            Map.entry('b', 0xFF253A88),     // dark blue
            Map.entry('Y', 0xFFFFD84A),     // yellow
            Map.entry('y', 0xFFC89020),     // dark yellow
            Map.entry('O', 0xFFF09030),     // orange
            Map.entry('o', 0xFF8A4A28),     // brown
            Map.entry('G', 0xFF58C040),     // green
            Map.entry('g', 0xFF2E7A30),     // dark green
            Map.entry('L', 0xFFA6E86A),     // light green
            Map.entry('E', 0xFF8C93A6),     // grey
            Map.entry('e', 0xFF545B70),     // dark grey
            Map.entry('H', 0xFFC6CCD8),     // light grey
            Map.entry('M', 0xFFF27AAA),     // pink
            Map.entry('C', 0xFF70D8F4),     // cyan
            Map.entry('c', 0xFF2A86B8));    // dark cyan

    static final int DIRT = 0xFFA0683A;
    static final int DIRT_DARK = 0xFF6A4026;
    static final int DIRT_LIGHT = 0xFFC08850;
    static final int GRASS = 0xFF58C040;
    static final int GRASS_DARK = 0xFF2E7A30;
    static final int GRASS_LIGHT = 0xFFA6E86A;
    static final int STONE = 0xFF8C93A6;
    static final int STONE_DARK = 0xFF545B70;
    static final int STONE_LIGHT = 0xFFC6CCD8;
    static final int BRICK = 0xFFB8483A;
    static final int BRICK_DARK = 0xFF7A2A26;
    static final int MORTAR = 0xFFD8C8B0;
    static final int CAVE = 0xFF5A4A72;
    static final int CAVE_DARK = 0xFF382C4C;
    static final int CAVE_LIGHT = 0xFF8A7AA6;
    static final int WOOD = 0xFFDAB078;
    static final int WOOD_DARK = 0xFF9A6A3A;
    static final int OUTLINE = 0xFF1C1A2C;
    static final int GOLD = 0xFFFFD84A;
    static final int GOLD_DARK = 0xFFC89020;
    static final int WHITE = 0xFFF4F4F0;
    static final int WATER = 0xD02A86B8;
    static final int WATER_DEEP = 0xE01E5A8A;
    static final int FOAM = 0xF0B8EEFF;
    static final int CLOUD = 0xFFF8F8FF;
    static final int CLOUD_SHADE = 0xFFC8D4EC;

    // ---- the figures, looking right -------------------------------------------------------------

    static final String[] HEAD = {
            "................",
            "......KKKK......",
            ".....KRRRRK.....",
            "....KRRWRRRK....",
            "....KRRRRRRK....",
            "....KKKKKKKKK...",
            "....KSSSSKSK....",
            "....KSSSSSSK....",
            ".....KsSSSK.....",
            "....KKBBBBKK...."};
    static final String[] STAND = rows(HEAD,
            "...KSKBBBBKSK...",
            "...KKKBBBBKKK...",
            ".....KbbbbK.....",
            ".....KbKKbK.....",
            ".....KoK.KoK....",
            ".....KKK.KKKK...");
    static final String[] RUN_1 = rows(HEAD,
            "...KSKBBBBKSK...",
            "...KKKBBBBKKK...",
            "....KbbbbbK.....",
            "...KbbK.KbbK....",
            "..KoK....KoK....",
            "..KKK....KKKK...");
    static final String[] RUN_2 = rows(HEAD,
            "...KSKBBBBKSK...",
            "...KKKBBBBKKK...",
            ".....KbbbbK.....",
            "......KbbK......",
            "......KooK......",
            "......KKKKK.....");
    static final String[] RUN_3 = rows(HEAD,
            "...KSKBBBBKSK...",
            "...KKKBBBBKKK...",
            "....KbbbbbK.....",
            ".....KbKKbbK....",
            "....KoK..KoK....",
            "....KKK..KKKK...");
    static final String[] JUMP = {
            "................",
            "......KKKK......",
            ".....KRRRRK.....",
            "....KRRWRRRK....",
            "....KRRRRRRK....",
            "....KKKKKKKKK...",
            "....KSSSSKSK....",
            "....KSSSSSSK....",
            ".....KsSSSK.KK..",
            "....KKBBBBKKSK..",
            "...KSKBBBBKK....",
            "...KKKBBBBK.....",
            ".....KbbbbbK....",
            ".....KbKKKbK....",
            ".....KoK.KoK....",
            ".....KKK..KKK..."};
    static final String[] FALL = {
            "................",
            "......KKKK......",
            ".....KRRRRK.....",
            "....KRRWRRRK....",
            "....KRRRRRRK....",
            "....KKKKKKKKK...",
            "....KSSSSKSK....",
            "....KSSSSSSK....",
            "..KK.KsSSSK.KK..",
            "..KSKKBBBBKKSK..",
            "...KKKBBBBKKK...",
            "....KKBBBBKK....",
            ".....KbbbbK.....",
            ".....KbKKbK.....",
            ".....KoK.KoK....",
            ".....KKK.KKK...."};
    static final String[] HURT = replace(FALL, 6, "....KSSSSRSK....", 7, "....KSSSKSSK....");
    static final String[] BLINK = replace(STAND, 6, "....KSSSSsSK....");

    static final String[] BEETLE_A = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......KKKKK.....",
            "....KKGLLGGKK...",
            "...KGLLGGGGGGK..",
            "..KGLGGGGGGGGKK.",
            "..KGGGGGGGGGKWKK",
            "..KgGGGGGGGGKKKK",
            "..KggggggggggKK.",
            "...KKKKKKKKKKK..",
            "...K..K..K..K...",
            "..K..K..K..K...."};
    static final String[] BEETLE_B = replace(BEETLE_A, 14, "....K..K..K..K..", 15, "....K..K..K..K..");
    static final String[] BEETLE_FLAT = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...KKKKKKKKKKK..",
            "..KgGGGGGGGGGKK.",
            "..KKKKKKKKKKKKK."};
    static final String[] BIRD_A = {
            "................",
            "................",
            "................",
            "...KKK..........",
            "...KBBK.........",
            "....KBBK.KKKK...",
            ".....KBBKMMMMK..",
            "....KMMMMMMMWKK.",
            "...KMMMMMMMMMKOO",
            "...KMMMMMMMMMKO.",
            "....KMMMMMMMMK..",
            ".....KKKKKKKK...",
            "......K...K.....",
            "................",
            "................",
            "................"};
    static final String[] BIRD_B = {
            "................",
            "................",
            "................",
            "................",
            "................",
            ".........KKKK...",
            "........KMMMMK..",
            "....KMMMMMMMWKK.",
            "...KMMMMMMMMMKOO",
            "..KBBKMMMMMMMKO.",
            ".KBBBKMMMMMMK...",
            ".KBBK.KKKKKKK...",
            "..KK..K...K.....",
            "................",
            "................",
            "................"};
    static final String[] BIRD_HIT = replace(BIRD_B, 7, "....KMMMMMMMRKK.");
    static final String[] SPRING_UP = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            ".KKKKKKKKKKKKKK.",
            ".KRRRRRRRRRRRRK.",
            ".KKKKKKKKKKKKKK.",
            "....KHHHHHHK....",
            "....KEEEEEEK....",
            "....KHHHHHHK....",
            ".KKKKKKKKKKKKKK.",
            ".KeeeeeeeeeeeeK."};
    static final String[] SPRING_DOWN = {
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            ".KKKKKKKKKKKKKK.",
            ".KRRRRRRRRRRRRK.",
            ".KKKKKKKKKKKKKK.",
            ".KeeeeeeeeeeeeK."};

    // ---- flags, pole and glitter (row 4) --------------------------------------------------------

    static final String POLE = "...KHK..........";
    static final String[] POLE_FOOT = replace(column(POLE), 14, "..KEEEK.........", 15, "..KKKKK.........");
    static final String[] FLAG_DOWN = replace(column(POLE), 0, "...KYK..........",
            9, "...KHKKKKK......", 10, "...KHKEEEEK.....", 11, "...KHKEEEEEK....", 12, "...KHKEEEEK.....",
            13, "...KHKKKKK......");
    static final String[] FLAG_UP_A = replace(column(POLE), 0, "...KYK..........",
            1, "...KHKKKKKK.....", 2, "...KHKGGGGGK....", 3, "...KHKGLGGGGK...", 4, "...KHKGGGGGK....",
            5, "...KHKKKKKK.....");
    static final String[] FLAG_UP_B = replace(column(POLE), 0, "...KYK..........",
            1, "...KHKKKK.......", 2, "...KHKGGGKKK....", 3, "...KHKGLGGGGK...", 4, "...KHKGGGGKK....",
            5, "...KHKKKK.......");
    static final String[] GOAL_A = replace(column(POLE), 0, "...KYK..........",
            1, "...KHKKKKKKKKK..", 2, "...KHKRRRRRRRRK.", 3, "...KHKRRRYRRRRK.", 4, "...KHKRRYYYRRRK.",
            5, "...KHKRRRYRRRRK.", 6, "...KHKrRRRRRRRK.", 7, "...KHKKKKKKKKKK.");
    static final String[] GOAL_B = replace(column(POLE), 0, "...KYK..........",
            1, "...KHKKKKKKKK...", 2, "...KHKRRRRRRRK..", 3, "...KHKRRRYRRRRK.", 4, "...KHKRRYYYRRK..",
            5, "...KHKRRRYRRRRK.", 6, "...KHKrRRRRRRK..", 7, "...KHKKKKKKKKKK.");
    static final String[] GLITTER_A = replace(empty(), 5, ".......Y........", 6, ".......Y........",
            7, ".....YYWYY......", 8, ".......Y........", 9, ".......Y........");
    static final String[] GLITTER_B = replace(empty(), 3, ".......W........", 7, "...W...Y...W....",
            11, ".......W........");

    public static void main(String[] args) throws IOException {
        File folder = new File(args[0]);
        new File(folder, "levels").mkdirs();
        ImageIO.write(tiles(), "png", new File(folder, "levels/tiles.png"));
        ImageIO.write(sprites(), "png", new File(folder, "sprites.png"));
    }

    // ---- the sprite sheet -----------------------------------------------------------------------

    static BufferedImage sprites() {
        BufferedImage sheet = new BufferedImage(8 * CELL, 5 * CELL, BufferedImage.TYPE_INT_ARGB);
        String[][] player = {STAND, RUN_1, RUN_2, RUN_3, JUMP, FALL, HURT, BLINK};
        String[][] things = {BEETLE_A, BEETLE_B, BEETLE_FLAT, BIRD_A, BIRD_B, BIRD_HIT, SPRING_UP, SPRING_DOWN};
        String[][] flags = {POLE_FOOT, FLAG_DOWN, FLAG_UP_A, FLAG_UP_B, GOAL_A, GOAL_B, GLITTER_A, GLITTER_B};
        for (int i = 0; i < 8; i++) {
            paint(sheet, i, 0, player[i], false);
            paint(sheet, i, 1, player[i], true);
            paint(sheet, i, 2, things[i], false);
            paint(sheet, i, 3, things[i], true);
            paint(sheet, i, 4, flags[i], false);
        }
        return sheet;
    }

    static void paint(BufferedImage sheet, int col, int row, String[] picture, boolean mirrored) {
        if (picture.length != CELL) throw new IllegalArgumentException(picture.length + " rows");
        for (int y = 0; y < CELL; y++) {
            if (picture[y].length() != CELL) {
                throw new IllegalArgumentException("row " + y + " is " + picture[y].length() + " wide: " + picture[y]);
            }
            for (int x = 0; x < CELL; x++) {
                Integer argb = PALETTE.get(picture[y].charAt(mirrored ? CELL - 1 - x : x));
                if (argb == null) throw new IllegalArgumentException("no colour " + picture[y].charAt(x));
                sheet.setRGB(col * CELL + x, row * CELL + y, argb);
            }
        }
    }

    // ---- the tileset ----------------------------------------------------------------------------

    static BufferedImage tiles() {
        BufferedImage set = new BufferedImage(8 * CELL, 4 * CELL, BufferedImage.TYPE_INT_ARGB);
        int[][] t = new int[32][];
        t[0] = grass(false, false);
        t[1] = dirt(1);
        t[2] = grass(true, false);
        t[3] = grass(false, true);
        t[4] = stone();
        t[5] = brick();
        t[6] = caveFloor();
        t[7] = rock(7, CAVE, CAVE_DARK, CAVE_LIGHT);
        t[8] = plank(true, false);
        t[9] = plank(false, false);
        t[10] = plank(false, true);
        t[11] = spikes();
        for (int i = 0; i < 4; i++) t[12 + i] = coin(new double[]{5.5, 3.5, 1, 3.5}[i]);
        t[16] = bush();
        t[17] = flower();
        t[18] = tallGrass();
        t[19] = fence();
        t[20] = mushroom();
        t[21] = pebble();
        t[22] = crystal();
        t[23] = vine();
        t[24] = water(0);
        t[25] = water(2);
        t[26] = deepWater();
        t[27] = cloud();
        t[28] = sign();
        t[29] = rock(29, CAVE_DARK, 0xFF2A2038, CAVE);
        t[30] = grass(true, true);
        t[31] = new int[CELL * CELL];
        for (int i = 0; i < 32; i++) {
            int[] pixels = t[i] == null ? new int[CELL * CELL] : t[i];
            set.setRGB((i % 8) * CELL, (i / 8) * CELL, CELL, CELL, pixels, 0, CELL);
        }
        return set;
    }

    static int[] dirt(long seed) {
        Random random = new Random(seed);
        int[] p = new int[CELL * CELL];
        for (int i = 0; i < p.length; i++) {
            double r = random.nextDouble();
            p[i] = r < 0.12 ? DIRT_DARK : r < 0.18 ? DIRT_LIGHT : DIRT;
        }
        // A few pebbles: a dark dot with a light one above it, wrapped round so that tiles join.
        for (int k = 0; k < 3; k++) {
            int x = random.nextInt(CELL);
            int y = 2 + random.nextInt(CELL - 3);
            p[y * CELL + x] = DIRT_DARK;
            p[y * CELL + (x + 1) % CELL] = DIRT_DARK;
            p[(y - 1) * CELL + x] = DIRT_LIGHT;
        }
        return p;
    }

    static int[] grass(boolean left, boolean right) {
        int[] p = dirt(2);
        Random random = new Random(3);
        for (int x = 0; x < CELL; x++) {
            // The grass hangs down a little unevenly, the same at both edges so that tiles join.
            int depth = 4 + (x * 7 % 5 == 0 ? 1 : 0) + random.nextInt(2);
            for (int y = 0; y < depth; y++) p[y * CELL + x] = y == 0 ? GRASS_LIGHT : y == depth - 1 ? GRASS_DARK : GRASS;
            if (x % 3 == 1) p[x] = GRASS;
        }
        if (left) corner(p, 0, 1);
        if (right) corner(p, CELL - 1, -1);
        return p;
    }

    /** Rounds off a top corner: pixels outside a quarter circle become air, the edge dark. */
    static void corner(int[] p, int edge, int inward) {
        for (int y = 0; y < CELL; y++) {
            int cut = y == 0 ? 3 : y == 1 ? 2 : y == 2 ? 1 : 0;
            for (int i = 0; i < cut; i++) p[y * CELL + edge + i * inward] = 0;
            p[y * CELL + edge + cut * inward] = y < 5 ? GRASS_DARK : DIRT_DARK;
        }
    }

    static int[] stone() {
        Random random = new Random(4);
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                int c = random.nextDouble() < 0.08 ? STONE_DARK : STONE;
                if (y == 0 || x == 0) c = STONE_LIGHT;
                if (y == CELL - 1 || x == CELL - 1) c = STONE_DARK;
                p[y * CELL + x] = c;
            }
        }
        return p;
    }

    static int[] brick() {
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            int course = y / 4;
            for (int x = 0; x < CELL; x++) {
                int joint = (x + (course % 2) * 4) % 8;
                int c = y % 4 == 3 || joint == 7 ? MORTAR : y % 4 == 0 ? 0xFFD06050 : joint == 6 ? BRICK_DARK : BRICK;
                p[y * CELL + x] = c;
            }
        }
        return p;
    }

    static int[] rock(long seed, int base, int dark, int light) {
        Random random = new Random(seed);
        int[] p = new int[CELL * CELL];
        for (int i = 0; i < p.length; i++) {
            double r = random.nextDouble();
            p[i] = r < 0.14 ? dark : r < 0.2 ? light : base;
        }
        return p;
    }

    static int[] caveFloor() {
        int[] p = rock(6, CAVE, CAVE_DARK, CAVE_LIGHT);
        for (int x = 0; x < CELL; x++) {
            p[x] = CAVE_LIGHT;
            p[CELL + x] = x % 4 == 0 ? CAVE : CAVE_LIGHT;
            p[2 * CELL + x] = CAVE_DARK;
        }
        return p;
    }

    static int[] plank(boolean left, boolean right) {
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < CELL; x++) {
                int c = y == 0 ? 0xFFF0D098 : y == 5 ? WOOD_DARK : (x + y * 3) % 9 == 0 ? WOOD_DARK : WOOD;
                if (x == 7 && y > 0 && y < 5) c = WOOD_DARK;      // the joint between two planks
                p[y * CELL + x] = c;
            }
        }
        if (left) for (int y = 0; y < 6; y++) p[y * CELL] = y == 0 || y == 5 ? 0 : OUTLINE;
        if (right) for (int y = 0; y < 6; y++) p[y * CELL + CELL - 1] = y == 0 || y == 5 ? 0 : OUTLINE;
        // A bracket under each end, a nail on each plank.
        if (left || right) {
            int x = left ? 2 : CELL - 3;
            for (int y = 6; y < 10; y++) p[y * CELL + x] = WOOD_DARK;
        }
        p[2 * CELL + 3] = STONE_DARK;
        p[2 * CELL + 11] = STONE_DARK;
        return p;
    }

    static int[] spikes() {
        int[] p = new int[CELL * CELL];
        for (int k = 0; k < 4; k++) {
            for (int y = 7; y < CELL; y++) {
                int half = (y - 7) / 4;
                int from = 4 * k + 1 - half;
                int to = 4 * k + 2 + half;
                for (int x = Math.max(4 * k, from); x <= Math.min(4 * k + 3, to); x++) {
                    p[y * CELL + x] = x <= 4 * k + 1 ? STONE_LIGHT : STONE;
                }
                if (y == 7) p[y * CELL + 4 * k + 1] = WHITE;
            }
        }
        for (int x = 0; x < CELL; x++) p[(CELL - 1) * CELL + x] = STONE_DARK;
        return p;
    }

    /** A coin seen from the side as it turns: an ellipse of half width {@code rx}. */
    static int[] coin(double rx) {
        int[] p = new int[CELL * CELL];
        double ry = 5.5;
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                double dx = (x + 0.5 - 8) / rx;
                double dy = (y + 0.5 - 8) / ry;
                double d = dx * dx + dy * dy;
                if (d > 1) continue;
                p[y * CELL + x] = d > 0.6 || rx < 1.5 ? GOLD_DARK : GOLD;
            }
        }
        if (rx > 3) {
            p[5 * CELL + 7] = WHITE;
            p[6 * CELL + 7] = WHITE;
        }
        return p;
    }

    static int[] bush() {
        int[] p = new int[CELL * CELL];
        blob(p, 5, 11, 4.5, GRASS, GRASS_DARK);
        blob(p, 10, 10, 5, GRASS, GRASS_DARK);
        blob(p, 8, 13, 4, GRASS, GRASS_DARK);
        p[8 * CELL + 9] = GRASS_LIGHT;
        p[8 * CELL + 10] = GRASS_LIGHT;
        p[9 * CELL + 4] = GRASS_LIGHT;
        return p;
    }

    static void blob(int[] p, double cx, double cy, double r, int fill, int edge) {
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                double d = Math.hypot(x + 0.5 - cx, y + 0.5 - cy);
                if (d <= r) p[y * CELL + x] = d > r - 1 ? edge : fill;
            }
        }
    }

    static int[] flower() {
        return picture("""
                ................
                ................
                ................
                ................
                ................
                ................
                ......M.M.......
                .....MMYMM......
                ......MMM.......
                .......g........
                .......g..g.....
                ....g..g.g......
                .....g.gg.......
                ......gg........
                .......g........
                .......g........""");
    }

    static int[] tallGrass() {
        return picture("""
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ..G.........G...
                ..G....G....G...
                ..GL...G...GL...
                ...G...GL..G....
                ...GL.GG...G..G.
                .G.GG.GG..GG.G..
                .GG.G.G.G.G.GG..
                ..GGgGGgGGgGG...
                ..gggggggggggg..""");
    }

    static int[] fence() {
        return picture("""
                ................
                ................
                ................
                ................
                ................
                ...oo......oo...
                ..oYYo....oYYo..
                ..oYYoooooYYYo..
                ..oYYYYYYYYYYo..
                ..oYYoooooYYYo..
                ..oYYo....oYYo..
                ..oYYoooooYYYo..
                ..oYYYYYYYYYYo..
                ..oYYoooooYYYo..
                ..oYYo....oYYo..
                ..oyyo....oyyo..""");
    }

    static int[] mushroom() {
        return picture("""
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ......KKKK......
                ....KKRWRRKK....
                ...KRRRRRWRRK...
                ...KRWRRRRRRK...
                ...KKKKKKKKKK...
                ......KWWK......
                ......KWWK......
                ......KWWK......
                .....KKKKKK.....""");
    }

    static int[] pebble() {
        return picture("""
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                ................
                .......KKKK.....
                ......KHHEEK....
                .....KHEEEEEK...
                ..KK.KEEEEEeK...
                .KHEK.KeeeeK....""");
    }

    static int[] crystal() {
        int[] p = picture("""
                ................
                ................
                ................
                ................
                ................
                ................
                .......K........
                ......KWK.......
                ......KCK...K...
                ......KCK..KWK..
                ..K..KCCcK.KCK..
                .KWK.KCCcK.KCcK.
                .KCK.KCCcKKCCcK.
                .KCcKKCCccKCCcK.
                KKCcKKCCccKCcKK.
                KKKKKKKKKKKKKKK.""");
        return p;
    }

    static int[] vine() {
        return picture("""
                ..g.....gg......
                ..g......g......
                ..gG.....g......
                ...g....Gg......
                ...g.....g......
                ..Gg.....gG.....
                ..g.......g.....
                ..g.......g.....
                .Gg.......gG....
                ..g........g....
                ..g.........G...
                ..gL............
                ...g............
                ...G............
                ................
                ................""");
    }

    static int[] water(int shift) {
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) {
                // A wave of 8 pixels, moved by two in the second picture.
                int wave = (int) Math.round(1.5 + 1.5 * Math.sin((x + shift) * Math.PI / 4));
                if (y < 3 + wave) continue;
                p[y * CELL + x] = y == 3 + wave ? FOAM : y > 10 && (x * 3 + y) % 11 == 0 ? FOAM : WATER;
            }
        }
        return p;
    }

    static int[] deepWater() {
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            for (int x = 0; x < CELL; x++) p[y * CELL + x] = (x * 5 + y * 3) % 23 == 0 ? WATER : WATER_DEEP;
        }
        return p;
    }

    static int[] cloud() {
        int[] p = new int[CELL * CELL];
        blob(p, 3, 6, 4, CLOUD, CLOUD_SHADE);
        blob(p, 8, 4.5, 4.5, CLOUD, CLOUD_SHADE);
        blob(p, 13, 6, 4, CLOUD, CLOUD_SHADE);
        for (int y = 5; y < 9; y++) for (int x = 0; x < CELL; x++) p[y * CELL + x] = y == 8 ? CLOUD_SHADE : CLOUD;
        for (int y = 9; y < CELL; y++) for (int x = 0; x < CELL; x++) p[y * CELL + x] = 0;
        for (int x = 0; x < CELL; x++) if (p[x] != 0) p[x] = WHITE;
        return p;
    }

    static int[] sign() {
        return picture("""
                ................
                ................
                ................
                ................
                ..KKKKKKKKKK....
                ..KYYYYYKYYYK...
                ..KYKKKKKKYYYK..
                ..KYYYYYKYYYK...
                ..KKKKKKKKKK....
                ......KoK.......
                ......KoK.......
                ......KoK.......
                ......KoK.......
                ......KoK.......
                ......KoK.......
                .....KKKKK......""");
    }

    // ---- helpers ---------------------------------------------------------------------------------

    static int[] picture(String text) {
        String[] lines = text.lines().map(String::strip).toArray(String[]::new);
        int[] p = new int[CELL * CELL];
        for (int y = 0; y < CELL; y++) {
            if (lines[y].length() != CELL) throw new IllegalArgumentException("row " + y + ": " + lines[y]);
            for (int x = 0; x < CELL; x++) {
                Integer argb = PALETTE.get(lines[y].charAt(x));
                if (argb == null) throw new IllegalArgumentException("no colour " + lines[y].charAt(x));
                p[y * CELL + x] = argb;
            }
        }
        return p;
    }

    static String[] rows(String[] top, String... bottom) {
        String[] all = new String[top.length + bottom.length];
        System.arraycopy(top, 0, all, 0, top.length);
        System.arraycopy(bottom, 0, all, top.length, bottom.length);
        return all;
    }

    /** A copy of {@code picture} with some rows replaced: pairs of a row number and its new text. */
    static String[] replace(String[] picture, Object... changes) {
        String[] copy = picture.clone();
        for (int i = 0; i < changes.length; i += 2) copy[(Integer) changes[i]] = (String) changes[i + 1];
        return copy;
    }

    static String[] column(String row) {
        String[] rows = new String[CELL];
        java.util.Arrays.fill(rows, row);
        return rows;
    }

    static String[] empty() {
        return column("................");
    }
}
