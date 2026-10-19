package eu.amadeos.games.jump.ui.game;

import eu.amadeos.gameengine.camera.Camera;
import eu.amadeos.gameengine.camera.Parallax;
import eu.amadeos.gameengine.collision.Box;
import eu.amadeos.gameengine.physics.Body;
import eu.amadeos.gameengine.sprite.Sprite;
import eu.amadeos.gameengine.sprite.SpriteSheet;
import eu.amadeos.gameengine.tiled.TiledMap;
import eu.amadeos.gameengine.tiled.TiledMap.Frame;
import eu.amadeos.gameengine.tiled.TiledMap.TileLayer;
import eu.amadeos.gameengine.tiled.TiledMap.Tileset;
import eu.amadeos.games.jump.domain.Enemy;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Game;
import eu.amadeos.games.jump.domain.Level;
import eu.amadeos.games.jump.domain.Player;
import eu.amadeos.games.jump.domain.Tile;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws a level as the camera sees it: the background, the map's two tile layers as Tiled made them, and
 * the things in it, the springs, flags, enemies and the player. All pictures are pixel art, 16 pixels for a
 * tile of 16 units, scaled with hard edges; tiles are drawn {@link Sprite#drawSeamless seamlessly}, so the
 * ground shows no lines between them at any window size.
 *
 * <p>The map is drawn from the map, not from the rules' tiles: which picture a tile has, and how it is
 * animated, is the level designer's business and lives in the tileset. The rules only say which coins are
 * gone.
 */
final class JumpPainter {
    static final double TILE = Field.TILE;
    static final String SPRITES = "/eu/amadeos/games/jump/sprites.png";
    static final Color POLE_OUTLINE = new Color(0x1C1A2C);
    static final Color POLE = new Color(0xC6CCD8);
    /** The sprite sheet's frames: the columns of rows 0 (right) and 1 (left) for the player. */
    static final int STAND = 0;
    static final int[] RUN = {1, 2, 3, 2};
    static final int JUMP = 4;
    static final int FALL = 5;
    static final int HURT = 6;
    static final int BLINK = 7;
    /** Seconds an animated tile runs behind its left neighbour. */
    static final double WAVE = 0.06;
    /** Units the player runs per step of the feet. */
    static final double STRIDE = 7;

    private final SpriteSheet sprites = SpriteSheet.load(JumpPainter.class, SPRITES, 8, 5, TILE, TILE).hardEdges();
    private final Map<Integer, Scenery> sceneries = new HashMap<>();
    private final Rectangle2D.Double rectangle = new Rectangle2D.Double();

    /** What a level needs to be drawn, made once for each level and kept. */
    // --8<-- [start:scenery]
    private static final class Scenery {
        final List<TileLayer> layers;
        /** The picture of each gid; null for none. */
        final Sprite[] pictures;
        /** The frames of each animated gid and their seconds; null for a tile that stands still. */
        final Sprite[][] frames;
        final double[][] seconds;
        final Parallax backdrop;

        Scenery(Level level) {
            TiledMap map = level.map();
            layers = map.layers();
            int gids = 1;
            for (Tileset set : map.tilesets()) gids = Math.max(gids, set.firstGid() + set.tileCount());
            pictures = new Sprite[gids];
            frames = new Sprite[gids][];
            seconds = new double[gids][];
            for (Tileset set : map.tilesets()) {
                SpriteSheet sheet = SpriteSheet.load(JumpPainter.class, Level.FOLDER + set.image(), set.columns(),
                        set.rows(), TILE, TILE).hardEdges();
                for (int id = 0; id < set.tileCount(); id++) pictures[set.firstGid() + id] = sheet.frame(id);
                set.tiles().forEach((id, info) -> {
                    List<Frame> animation = info.animation();
                    if (animation.isEmpty()) return;
                    int gid = set.firstGid() + id;
                    frames[gid] = new Sprite[animation.size()];
                    seconds[gid] = new double[animation.size()];
                    for (int i = 0; i < animation.size(); i++) {
                        frames[gid][i] = sheet.frame(animation.get(i).tileId());
                        seconds[gid][i] = animation.get(i).seconds();
                    }
                });
            }
            backdrop = Backdrop.of(level.theme(), level.height() * TILE);
        }

        /** The picture of {@code gid} at {@code time}: the frame of its animation, or its own. */
        Sprite picture(int gid, double time) {
            Sprite[] animation = frames[gid];
            if (animation == null) return pictures[gid];
            double[] lengths = seconds[gid];
            double total = 0;
            for (double length : lengths) total += length;
            double at = time % total;
            for (int i = 0; i < animation.length; i++) {
                at -= lengths[i];
                if (at < 0) return animation[i];
            }
            return animation[animation.length - 1];
        }
    }
    // --8<-- [end:scenery]

    private Scenery scenery(Level level) {
        return sceneries.computeIfAbsent(level.number(), number -> new Scenery(level));
    }

    // --8<-- [start:paint]
    /**
     * The level as the camera sees it, {@code alpha} of the way between the last two steps.
     *
     * @param time the clock of the animations, which runs on when the game's clock stops at the goal
     */
    void paint(Graphics2D g, Game game, Camera camera, Effects effects, double time, boolean player, double alpha) {
        Object smoothing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Scenery scenery = scenery(game.level());
        double cx = camera.x(alpha);
        double cy = camera.y(alpha);
        scenery.backdrop.draw(g, cx, cy);
        AffineTransform t = g.getTransform();
        g.translate(-cx, -cy);
        for (TileLayer layer : scenery.layers) {
            if (!layer.visible()) continue;
            if (layer.name().equals("ground")) things(g, game, time);
            tiles(g, scenery, layer, game, camera, cx, cy, time);
        }
        enemies(g, game.enemies(), alpha);
        if (player) player(g, game.player(), time, alpha);
        effects.render(g, alpha);
        g.setTransform(t);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smoothing);
    }

    /** The tiles of a layer that the camera sees, and no others: 41 × 26 of a map of thousands. */
    private void tiles(Graphics2D g, Scenery scenery, TileLayer layer, Game game, Camera camera, double cx,
                       double cy, double time) {
        boolean ground = layer.name().equals("ground");
        int firstCol = Math.max(0, (int) Math.floor(cx / TILE));
        int lastCol = Math.min(layer.width() - 1, (int) Math.floor((cx + camera.viewWidth()) / TILE));
        int firstRow = Math.max(0, (int) Math.floor(cy / TILE));
        int lastRow = Math.min(layer.height() - 1, (int) Math.floor((cy + camera.viewHeight()) / TILE));
        for (int row = firstRow; row <= lastRow; row++) {
            for (int col = firstCol; col <= lastCol; col++) {
                int gid = layer.gid(col, row);
                if (gid == 0 || gid >= scenery.pictures.length) continue;
                // A coin that was taken is air for the rules; the map still has it.
                if (ground && game.tiles().get(col, row) == Tile.AIR && game.level().tile(col, row) == Tile.COIN) {
                    continue;
                }
                // Each tile's animation runs a little behind the one to its left: a row of coins turns
                // like a wave instead of blinking in step.
                scenery.picture(gid, time + col * WAVE).drawSeamless(g, col * TILE, row * TILE);
            }
        }
    }
    // --8<-- [end:paint]

    /** Springs, checkpoints and the goal: behind the ground, so their feet stand in the grass. */
    private void things(Graphics2D g, Game game, double time) {
        Level level = game.level();
        for (int i = 0; i < level.springs().size(); i++) {
            Box spring = level.springs().get(i);
            sprites.frame(game.pressed(i) ? 7 : 6, 2).draw(g, spring.x(), spring.bottom() - TILE);
        }
        boolean wave = (int) (time * 4) % 2 == 0;
        for (int i = 0; i < level.checkpoints().size(); i++) {
            flag(g, level.checkpoints().get(i), game.reached(i) ? (wave ? 2 : 3) : 1);
        }
        flag(g, level.goal(), wave ? 4 : 5);
    }

    /** A pole as high as its object, with a flag on top. */
    private void flag(Graphics2D g, Box box, int flag) {
        double x = box.x();
        double between = box.height() - 2 * TILE;
        if (between > 0) {
            g.setColor(POLE_OUTLINE);
            rectangle.setRect(x + 3, box.y() + TILE, 3, between);
            g.fill(rectangle);
            g.setColor(POLE);
            rectangle.setRect(x + 4, box.y() + TILE, 1, between);
            g.fill(rectangle);
        }
        sprites.frame(0, 4).draw(g, x, box.bottom() - TILE);
        sprites.frame(flag, 4).draw(g, x, box.y());
    }

    private void enemies(Graphics2D g, List<Enemy> enemies, double alpha) {
        for (Enemy enemy : enemies) {
            if (enemy.gone()) continue;
            Body body = enemy.body();
            int row = enemy.facingLeft() ? 3 : 2;
            int flap = (int) (enemy.time() * (enemy.kind() == Enemy.Kind.BIRD ? 8 : 6)) % 2;
            double x = body.x(alpha) + body.width / 2 - TILE / 2;
            if (enemy.kind() == Enemy.Kind.BEETLE) {
                int frame = enemy.squashed() ? 2 : flap;
                sprites.frame(frame, row).draw(g, x, body.y(alpha) + body.height - TILE);
            } else {
                int frame = enemy.squashed() ? 5 : 3 + flap;
                sprites.frame(frame, row).draw(g, x, body.y(alpha) + body.height / 2 - TILE / 2);
            }
        }
    }

    // --8<-- [start:player]
    /** The player's picture: running, jumping, falling, tumbling, or standing and blinking now and then. */
    private void player(Graphics2D g, Player player, double time, double alpha) {
        Body body = player.body();
        int frame;
        if (player.dying()) frame = HURT;
        else if (!player.onGround()) frame = body.vy < 0 ? JUMP : FALL;
        else if (Math.abs(body.vx) > 5) frame = RUN[(int) (player.ran() / STRIDE) % RUN.length];
        else frame = time % 3 < 0.12 ? BLINK : STAND;
        double x = body.x(alpha) + body.width / 2 - TILE / 2;
        double y = body.y(alpha) + body.height - TILE;
        sprites.frame(frame, player.facingLeft() ? 1 : 0).draw(g, x, y);
    }
    // --8<-- [end:player]

    /** A frame of the sprite sheet, for the overlay and the effects. */
    Sprite sprite(int column, int row) {
        return sprites.frame(column, row);
    }

    /** The background of a level alone, for the title and the level cards. */
    void backdrop(Graphics2D g, Level level, double cx, double cy) {
        scenery(level).backdrop.draw(g, cx, cy);
    }
}
