package dev.deltasound.client.sound;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.deltasound.Deltasound;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Maintains a folder resource pack under the game {@code resourcepacks/} directory
 * so imported {@code .ogg} files can be played as {@code deltasound:<name>}.
 */
public final class UserSoundPack {
	public static final String PACK_FOLDER_NAME = "deltasound_user";
	public static final String PACK_ID = "file/" + PACK_FOLDER_NAME;
	public static final String SOUND_NAMESPACE = Deltasound.MOD_ID;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private UserSoundPack() {
	}

	public static Path packRoot(Minecraft client) {
		return client.getResourcePackDirectory().resolve(PACK_FOLDER_NAME);
	}

	public static Path soundsDir(Minecraft client) {
		return packRoot(client).resolve("assets").resolve(SOUND_NAMESPACE).resolve("sounds");
	}

	public static void ensurePack(Minecraft client) throws IOException {
		Path root = packRoot(client);
		Files.createDirectories(soundsDir(client));
		Path meta = root.resolve("pack.mcmeta");
		if (Files.notExists(meta)) {
			Files.writeString(meta, """
					{
					  "pack": {
					    "description": "Deltasound user-imported sounds",
					    "min_format": 84,
					    "max_format": 84
					  }
					}
					""");
		}
		rewriteSoundsJson(client);
	}

	public static String importOgg(Minecraft client, Path source) throws IOException {
		ensurePack(client);
		String fileName = source.getFileName().toString();
		if (!fileName.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
			throw new IOException("Only .ogg files are supported");
		}

		String base = fileName.substring(0, fileName.length() - 4)
				.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9/_-]", "_");
		if (base.isBlank()) {
			base = "custom_sound";
		}

		Path target = soundsDir(client).resolve(base + ".ogg");
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		rewriteSoundsJson(client);
		enableAndReload(client);
		return SOUND_NAMESPACE + ":" + base;
	}

	public static List<String> listImportedSoundIds(Minecraft client) {
		Path dir = soundsDir(client);
		if (Files.notExists(dir)) {
			return List.of();
		}
		try (Stream<Path> stream = Files.list(dir)) {
			List<String> ids = new ArrayList<>();
			stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
					.sorted()
					.forEach(path -> {
						String name = path.getFileName().toString();
						ids.add(SOUND_NAMESPACE + ":" + name.substring(0, name.length() - 4));
					});
			return ids;
		} catch (IOException ex) {
			Deltasound.LOGGER.warn("Failed listing imported sounds", ex);
			return List.of();
		}
	}

	public static void enableAndReload(Minecraft client) {
		try {
			ensurePack(client);
		} catch (IOException ex) {
			Deltasound.LOGGER.error("Failed preparing user sound pack", ex);
			return;
		}

		client.getResourcePackRepository().reload();
		List<String> selected = new ArrayList<>(client.options.resourcePacks);
		if (!selected.contains(PACK_ID)) {
			selected.add(PACK_ID);
			client.options.resourcePacks = selected;
		}
		client.options.updateResourcePacks(client.getResourcePackRepository());
		client.reloadResourcePacks();
		Deltasound.LOGGER.info("Enabled user sound pack {}", PACK_ID);
	}

	private static void rewriteSoundsJson(Minecraft client) throws IOException {
		Path soundsJson = packRoot(client)
				.resolve("assets")
				.resolve(SOUND_NAMESPACE)
				.resolve("sounds.json");
		Files.createDirectories(soundsJson.getParent());

		JsonObject root = new JsonObject();
		for (String soundId : listImportedSoundIds(client)) {
			String path = soundId.substring(soundId.indexOf(':') + 1);
			JsonObject entry = new JsonObject();
			JsonArray sounds = new JsonArray();
			sounds.add(SOUND_NAMESPACE + ":" + path);
			entry.add("sounds", sounds);
			root.add(path, entry);
		}

		try (Writer writer = Files.newBufferedWriter(soundsJson)) {
			GSON.toJson(root, writer);
		}
	}
}
