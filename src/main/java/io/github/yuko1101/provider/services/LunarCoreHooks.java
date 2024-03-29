package io.github.yuko1101.provider.services;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LunarCoreHooks {
    public static final String INTERNAL_NAME = LunarCoreHooks.class.getName().replace('.', '/');

    @SuppressWarnings("unused")
    public static void init() {
        Path runDir = Paths.get(".");

        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;

        loader.prepareModInit(runDir, loader.getGameInstance());
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
    }
}
