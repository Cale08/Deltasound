package dev.deltasound.client.sound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.deltasound.Deltasound;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tracks imported custom audio files that are not (or not only) Minecraft resource-pack sounds.
 * MP3 files are played through {@link CustomAudioPlayer}; OGG files also register in the user pack.
 */
public final class CustomSoundLibrary {
	public static final String CUSTOM_PREFIX = "deltasound:custom/";

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

	private final Path root = FabricLoader.getInstance().getConfigDir().resolve(Deltasound.MOD_ID).resolve("custom_sounds");
	private final Path indexPath = root.resolve("index.json");
	/** soundId -> absolute file path string */
	private final Map<String, String> index = new HashMap<>();

	public void load() {
		try {
			Files.createDirectories(root);
			index.clear();
			if (Files.notExists(indexPath)) {
				save();
				return;
			}
			try (Reader reader = Files.newBufferedReader(indexPath)) {
				Map<String, String> loaded = GSON.fromJson(reader, MAP_TYPE);
				if (loaded != null) {
					index.putAll(loaded);
				}
			}
		} catch (IOException ex) {
			Deltasound.LOGGER.error("Failed loading custom sound library", ex);
		}
	}

	public void save() throws IOException {
		Files.createDirectories(root);
		try (Writer writer = Files.newBufferedWriter(indexPath)) {
			GSON.toJson(index, writer);
		}
	}

	public boolean isCustom(String soundId) {
		return soundId != null && index.containsKey(soundId);
	}

	public Optional<Path> pathFor(String soundId) {
		String stored = index.get(soundId);
		if (stored == null) {
			return Optional.empty();
		}
		return Optional.of(Path.of(stored));
	}

	public List<String> soundIds() {
		return index.keySet().stream().sorted().toList();
	}

	public String importFile(Path source) throws IOException {
		Files.createDirectories(root);
		String fileName = source.getFileName().toString();
		String lower = fileName.toLowerCase(Locale.ROOT);
		if (!lower.endsWith(".ogg") && !lower.endsWith(".mp3")) {
			throw new IOException("Only .ogg and .mp3 files are supported");
		}

		String base = fileName.substring(0, fileName.lastIndexOf('.'))
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9/_-]", "_");
		if (base.isBlank()) {
			base = "custom_sound";
		}

		String extension = lower.substring(lower.lastIndexOf('.'));
		Path target = root.resolve(base + extension);
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

		String soundId = CUSTOM_PREFIX + base;
		index.put(soundId, target.toAbsolutePath().toString());
		save();
		return soundId;
	}
}
