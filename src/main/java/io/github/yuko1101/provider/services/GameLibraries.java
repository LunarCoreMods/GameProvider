package io.github.yuko1101.provider.services;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.game.LibClassifier;

import java.util.Arrays;

public enum GameLibraries implements LibClassifier.LibraryType {
    LUNAR_CORE_GAME_PROVIDER(Arrays.stream(LunarCoreGameProvider.ENTRYPOINTS).map(s -> s.replace('.', '/') + ".class").toArray(String[]::new));

    private final String[] paths;

    GameLibraries(String... paths) {
        this.paths = paths;
    }

    @Override
    public boolean isApplicable(EnvType env) {
        return true;
    }

    @Override
    public String[] getPaths() {
        return paths;
    }
}
