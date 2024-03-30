package io.github.yuko1101.provider.services;

import io.github.yuko1101.provider.patch.LunarCoreEntrypointPatch;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.game.GameProvider;
import net.fabricmc.loader.impl.game.GameProviderHelper;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.metadata.BuiltinModMetadata;
import net.fabricmc.loader.impl.metadata.ContactInformationImpl;
import net.fabricmc.loader.impl.util.Arguments;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.version.StringVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class LunarCoreGameProvider implements GameProvider {

    protected static final String[] ENTRYPOINTS = new String[]{"emu.lunarcore.LunarCore"};
    private static final Set<String> SENSITIVE_ARGS = new HashSet<>(List.of());

    private Arguments arguments;
    private String entrypoint;
    private Path launchDir;
    private Path libDir;
    private Path gameJar;
    private boolean development = false;
    private final List<Path> miscGameLibraries = new ArrayList<>();
    private Collection<Path> validParentClassPath;
    private static StringVersion gameVersion;

    private static final GameTransformer TRANSFORMER = new GameTransformer(new LunarCoreEntrypointPatch());

    @Override
    public String getGameId() {
        return "lunarcore";
    }

    @Override
    public String getGameName() {
        return "LunarCore";
    }

    @Override
    public String getRawGameVersion() {
        if (gameVersion == null) {
            try {
                Class<?> buildConfig = Class.forName("emu.lunarcore.BuildConfig", true, getClass().getClassLoader());
                var field = buildConfig.getDeclaredField("VERSION");
                field.setAccessible(true);
                String version = (String) field.get(null);
                gameVersion = new StringVersion(version);
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return gameVersion.getFriendlyString();
    }

    @Override
    public String getNormalizedGameVersion() {
        return getRawGameVersion();
    }

    @Override
    public Collection<BuiltinMod> getBuiltinMods() {
        HashMap<String, String> lunarCoreInfo = new HashMap<>();
        lunarCoreInfo.put("homepage", "https://github.com/Melledy/LunarCore");
        lunarCoreInfo.put("issues", "https://github.com/Melledy/LunarCore/issues");
        lunarCoreInfo.put("discord", "https://discord.gg/lunar-core-1163718404067303444");

        BuiltinModMetadata.Builder metadata =
                new BuiltinModMetadata.Builder(getGameId(), getNormalizedGameVersion())
                        .setName(getGameName())
                        .addAuthor("Melledy", lunarCoreInfo)
                        .setContact(new ContactInformationImpl(lunarCoreInfo))
                        .setDescription("A game server reimplementation for a certain turn-based anime game");

        return Collections.singletonList(new BuiltinMod(Collections.singletonList(gameJar), metadata.build()));
    }

    @Override
    public String getEntrypoint() {
        return entrypoint;
    }

    @Override
    public Path getLaunchDirectory() {
        if (arguments == null) {
            return Paths.get(".");
        }
        return getLaunchDirectory(arguments);
    }

    @Override
    public boolean isObfuscated() {
        return false;
    }

    @Override
    public boolean requiresUrlClassLoader() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean locateGame(FabricLauncher launcher, String[] args) {
        this.arguments = new Arguments();
        arguments.parse(args);

        if (Objects.equals(System.getProperty(SystemProperties.DEVELOPMENT), "true")) {
            development = true;
        }

        Path commonJarPath = GameProviderHelper.getCommonGameJar();
        Path envJarPath = GameProviderHelper.getEnvGameJar(launcher.getEnvironmentType());
        try {
            LibClassifier<GameLibraries> classifier = new LibClassifier<>(GameLibraries.class, launcher.getEnvironmentType(), this);
            GameLibraries gameLib = GameLibraries.LUNAR_CORE_GAME_PROVIDER;

            if (commonJarPath != null) {
                classifier.process(commonJarPath);
            } else if (envJarPath != null) {
                classifier.process(envJarPath);
            } else {
                // You can pass the path to the game jar via the classpath
                // when you launch Knot, but for simple cases a hard-coded list is fine.
                Optional<Path> path = Stream.of(
                        Path.of("./LunarCore.jar"),
                        Path.of("./lunarcore.jar")
                ).filter(Files::exists).findFirst();
                if (path.isPresent()) {
                    classifier.process(path.get());
                }
            }
            classifier.process(launcher.getClassPath());

            entrypoint = classifier.getClassName(gameLib);
            gameJar = classifier.getOrigin(gameLib);

            miscGameLibraries.addAll(classifier.getUnmatchedOrigins());
            validParentClassPath = classifier.getSystemLibraries();
        } catch (Exception e) {
            e.printStackTrace();
        }

        processArgumentMap(arguments);

        return true;
    }

    @Override
    public void initialize(FabricLauncher launcher) {
        launcher.setValidParentClassPath(validParentClassPath);
        TRANSFORMER.locateEntrypoints(launcher, Collections.singletonList(gameJar));
    }

    @Override
    public GameTransformer getEntrypointTransformer() {
        return TRANSFORMER;
    }

    @Override
    public void unlockClassPath(FabricLauncher launcher) {
        launcher.addToClassPath(gameJar);

        for (Path lib : miscGameLibraries) {
            launcher.addToClassPath(lib);
        }
    }

    @Override
    public void launch(ClassLoader loader) {
        String targetClass = entrypoint;

        try {
            Class<?> c = loader.loadClass(targetClass);
            Method m = c.getMethod("main", String[].class);
            m.invoke(null, (Object) arguments.toArray());
        } catch(InvocationTargetException e) {
            throw new FormattedException("The game has crashed!", e.getCause());
        } catch(ReflectiveOperationException e) {
            throw new FormattedException("Failed to start the game", e);
        }
    }

    @Override
    public Arguments getArguments() {
        return arguments;
    }

    @Override
    public String[] getLaunchArguments(boolean sanitize) {
        if (arguments == null) return new String[0];

        String[] ret = arguments.toArray();
        if (!sanitize) return ret;

        int writeIdx = 0;

        for (int i = 0; i < ret.length; i++) {
            String arg = ret[i];

            if (i + 1 < ret.length && arg.startsWith("-") && SENSITIVE_ARGS.contains(arg.substring(1).toLowerCase(Locale.US))) {
                i++;
            } else {
                ret[writeIdx++] = arg;
            }
        }

        if (writeIdx < ret.length) ret = Arrays.copyOf(ret, writeIdx);

        return ret;
    }

    private void processArgumentMap(Arguments arguments) {
        if (!arguments.containsKey("gameDir")) {
            arguments.put("gameDir", getLaunchDirectory(arguments).toAbsolutePath().normalize().toString());
        }

        launchDir = Path.of(arguments.get("gameDir"));
        System.out.println("Launch directory is " + launchDir);
        libDir = launchDir.resolve(Path.of("./lib"));
    }

    private static Path getLaunchDirectory(Arguments arguments) {
        return Paths.get(arguments.getOrDefault("gameDir", "."));
    }
}
