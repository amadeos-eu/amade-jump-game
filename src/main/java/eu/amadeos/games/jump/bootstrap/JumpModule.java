package eu.amadeos.games.jump.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.gameengine.audio.Mixer;
import eu.amadeos.gameengine.input.Gamepads;
import eu.amadeos.gameengine.input.Keyboard;
import eu.amadeos.games.jump.repository.JumpSettings;
import eu.amadeos.games.jump.repository.Progress;
import eu.amadeos.games.jump.ui.game.Arena;
import eu.amadeos.games.jump.ui.game.JumpGame;
import eu.amadeos.games.jump.ui.main.JumpPresenter;
import eu.amadeos.games.jump.ui.main.JumpView;
import eu.amadeos.games.jump.ui.main.JumpWindow;

import java.util.Locale;

/**
 * amade-jump-game bindings. Theme, language and the XDG directories (where {@link Progress} saves) come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class JumpModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.games.jump.messages";

    @Override
    protected void configure() {
        bind(JumpSettings.class).in(Scopes.SINGLETON);
        bind(Progress.class).in(Scopes.SINGLETON);
        bind(Keyboard.class).in(Scopes.SINGLETON);
        bind(Mixer.class).in(Scopes.SINGLETON);
        bind(JumpGame.class).in(Scopes.SINGLETON);
        bind(Arena.class).to(JumpGame.class);
        bind(JumpWindow.class).in(Scopes.SINGLETON);
        bind(JumpView.class).to(JumpWindow.class);
        bind(JumpPresenter.class).in(Scopes.SINGLETON);
    }

    /** The gamepads of this computer, by the backend of its operating system. */
    @Provides
    @Singleton
    Gamepads gamepads() {
        return Gamepads.open();
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(Locale locale) {
        return Messages.load(JumpModule.class, MESSAGES, locale);
    }
}
