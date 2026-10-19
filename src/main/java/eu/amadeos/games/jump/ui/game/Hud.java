package eu.amadeos.games.jump.ui.game;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.games.jump.domain.Field;
import eu.amadeos.games.jump.domain.Game;
import eu.amadeos.games.jump.domain.LevelResult;
import eu.amadeos.games.jump.repository.Progress;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * The overlay: the coins, the clock and the falls at the top of the screen, the level's name at its start,
 * and the cards and texts of the title, the level choice, the pause and the goal. The canvas draws it after
 * the world, at the full size of the screen, so the letters are sharp in every render mode.
 */
final class Hud {
    static final Color CHALK = Color.WHITE;
    static final Color GOLD = new Color(0xFFD84A);
    static final Color GOLD_DARK = new Color(0xC89020);
    static final Color GREEN = new Color(0xA6E86A);
    static final Color RED = new Color(0xFF6050);
    static final Color GREY = new Color(0x9098A8);
    static final Color VEIL = new Color(0, 0, 0, 150);
    static final Color CARD = new Color(0x20242E);
    static final Color CARD_CHOSEN = new Color(0x34405A);
    static final Color SHADOW = new Color(0, 0, 0, 120);
    /** How long the level's name shows at its start, and a checkpoint's message. */
    static final double BANNER_SECONDS = 2.2;
    static final double CARD_WIDTH = 176;
    static final double CARD_HEIGHT = 150;

    private final Messages msg;
    private final Map<Integer, Font> fonts = new HashMap<>();
    private final Rectangle2D.Double veil = new Rectangle2D.Double(0, 0, Field.WIDTH, Field.HEIGHT);
    private final Rectangle2D.Double rectangle = new Rectangle2D.Double();
    private final Ellipse2D.Double ellipse = new Ellipse2D.Double();

    Hud(Messages msg) {
        this.msg = msg;
    }

    // --8<-- [start:status]
    /** Coins, clock and falls along the top; the level's name for a moment at its start. */
    void status(Graphics2D g, Game game, double sinceCheckpoint) {
        coin(g, 14, 10, 14);
        g.setFont(font(15, true));
        shadowed(g, game.coins() + " / " + game.level().coins(), 34, 23, CHALK);
        String clock = clock(game.time());
        shadowed(g, clock, (Field.WIDTH - g.getFontMetrics().stringWidth(clock)) / 2, 23, CHALK);
        String falls = msg.format("hud.falls", game.deaths());
        shadowed(g, falls, Field.WIDTH - 14 - g.getFontMetrics().stringWidth(falls), 23, CHALK);
        if (game.time() < BANNER_SECONDS) {
            int number = game.level().number();
            fadingText(g, msg.format("play.level", number, name(number)), 120, 26, GOLD, game.time());
        } else if (sinceCheckpoint < BANNER_SECONDS) {
            fadingText(g, msg.get("play.checkpoint"), 120, 22, GREEN, sinceCheckpoint);
        }
    }
    // --8<-- [end:status]

    /** A text that stays and then fades out in the last half second of the banner time. */
    private void fadingText(Graphics2D g, String line, double y, int size, Color colour, double shown) {
        double left = BANNER_SECONDS - shown;
        int alpha = (int) Math.round(255 * Math.clamp(left / 0.5, 0, 1));
        g.setFont(font(size, true));
        double x = (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2;
        Color faded = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
        g.setColor(new Color(0, 0, 0, alpha / 2));
        g.drawString(line, (float) x + 2, (float) y + 2);
        g.setColor(faded);
        g.drawString(line, (float) x, (float) y);
    }

    /** A gold coin, drawn in code at the overlay's resolution. */
    void coin(Graphics2D g, double x, double y, double size) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(GOLD_DARK);
        ellipse.setFrame(x, y, size, size);
        g.fill(ellipse);
        g.setColor(GOLD);
        ellipse.setFrame(x + 2, y + 2, size - 4, size - 4);
        g.fill(ellipse);
        g.setColor(GOLD_DARK);
        rectangle.setRect(x + size / 2 - 1, y + 4, 2, size - 8);
        g.fill(rectangle);
    }

    // --8<-- [start:card]
    /** One level on the choice screen: its name, and the best time and coins, or a lock. */
    void card(Graphics2D g, double x, double y, int number, int coins, boolean open, boolean chosen,
              Progress progress) {
        g.setColor(chosen ? CARD_CHOSEN : CARD);
        g.fill(new RoundRectangle2D.Double(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12));
        if (chosen) {
            g.setColor(GOLD);
            g.draw(new RoundRectangle2D.Double(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12));
        }
        double cx = x + CARD_WIDTH / 2;
        g.setFont(font(34, true));
        g.setColor(open ? GOLD : GREY);
        centred(g, Integer.toString(number), cx, y + 46);
        g.setFont(font(16, true));
        g.setColor(open ? CHALK : GREY);
        centred(g, name(number), cx, y + 72);
        g.setFont(font(11, false));
        if (!open) {
            lock(g, cx, y + 96);
            g.setColor(GREY);
            centred(g, msg.get("select.locked"), cx, y + 134);
            return;
        }
        OptionalDouble best = progress.best(number);
        g.setColor(CHALK);
        centred(g, best.isPresent() ? msg.format("select.best", clock(best.getAsDouble())) : msg.get("select.none"),
                cx, y + 100);
        int most = progress.coins(number);
        g.setColor(most == coins ? GOLD : CHALK);
        centred(g, msg.format("select.coins", most, coins), cx, y + 120);
    }
    // --8<-- [end:card]

    /** A padlock. */
    private void lock(Graphics2D g, double cx, double y) {
        g.setColor(GREY);
        g.setStroke(new java.awt.BasicStroke(3));
        g.draw(new RoundRectangle2D.Double(cx - 7, y - 2, 14, 16, 12, 12));
        rectangle.setRect(cx - 11, y + 7, 22, 16);
        g.fill(rectangle);
        g.setStroke(new java.awt.BasicStroke(1));
    }

    /** The goal: the time, the coins and the falls, and what was a record. */
    void cleared(Graphics2D g, LevelResult result, Progress.Record record, boolean last) {
        veil(g);
        text(g, msg.get("cleared.title"), 110, 34, true, GOLD);
        text(g, msg.format("cleared.time", clock(result.seconds())), 160, 18, false, CHALK);
        if (record.bestTime()) text(g, msg.get("cleared.best"), 180, 13, true, GREEN);
        text(g, msg.format("cleared.coins", result.coins(), result.totalCoins()), 212, 18, false,
                result.allCoins() ? GOLD : CHALK);
        if (result.allCoins()) text(g, msg.get("cleared.all"), 232, 13, true, GOLD);
        text(g, msg.format("cleared.falls", result.deaths()), 264, 18, false, CHALK);
        if (record.opened()) text(g, msg.format("cleared.opened", result.level() + 1), 300, 14, true, GREEN);
        text(g, msg.get(last ? "cleared.last" : "cleared.next"), 350, 14, false, CHALK);
    }

    /** The name of level {@code number} in the player's language. */
    String name(int number) {
        return msg.get("level." + number);
    }

    /** Seconds as minutes, seconds and tenths: 83.46 is "1:23.4". */
    static String clock(double seconds) {
        int tenths = (int) Math.floor(seconds * 10);
        return String.format(Locale.ROOT, "%d:%02d.%d", tenths / 600, tenths / 10 % 60, tenths % 10);
    }

    static void centred(Graphics2D g, String text, double x, double y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (x - fm.stringWidth(text) / 2.0), (float) y);
    }

    /** A text with a dark shadow, readable over the bright sky and the dark cave. */
    private static void shadowed(Graphics2D g, String text, double x, double y, Color colour) {
        g.setColor(SHADOW);
        g.drawString(text, (float) x + 1, (float) y + 1);
        g.setColor(colour);
        g.drawString(text, (float) x, (float) y);
    }

    /** Darkens the world so text over it stays readable. */
    void veil(Graphics2D g) {
        Object smoothing = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(VEIL);
        g.fill(veil);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, smoothing);
    }

    /** A line of text centred horizontally, its baseline at {@code y}. */
    void text(Graphics2D g, String line, double y, int size, boolean bold, Color colour) {
        g.setFont(font(size, bold));
        g.setColor(colour);
        g.drawString(line, (float) (Field.WIDTH - g.getFontMetrics().stringWidth(line)) / 2, (float) y);
    }

    Font font(int size, boolean bold) {
        return fonts.computeIfAbsent(2 * size + (bold ? 1 : 0),
                key -> new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, size));
    }
}
