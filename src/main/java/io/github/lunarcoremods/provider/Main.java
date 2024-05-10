package io.github.lunarcoremods.provider;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.util.SystemProperties;

public final class Main {
    public static void main(String[] args) {
        System.setProperty(SystemProperties.SKIP_MC_PROVIDER, "true");

        Knot.launch(args, EnvType.SERVER);
    }
}
