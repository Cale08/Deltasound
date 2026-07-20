package dev.deltasound.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import dev.deltasound.Deltasound;
import dev.deltasound.core.ChatTrigger;
import dev.deltasound.core.ChatTriggerEngine;
import dev.deltasound.core.MatchMode;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

/**
 * Loads/saves chat triggers from {@code config/deltasound/triggers.json}.
 */
public final class TriggerConfigLoader {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path configDir = FabricLoader.getInstance().getConfigDir().resolve(Deltasound.MOD_ID);
	private final Path configPath = configDir.resolve("triggers.json");

	private final List<TriggerEntry> entries = new ArrayList<>();

	public Path configPath() {
		return configPath;
	}

	public Path configDir() {
		return configDir;
	}

	public List<TriggerEntry> entries() {
		return entries;
	}

	public void loadInto(ChatTriggerEngine engine) {
		try {
			Files.createDirectories(configDir);
			if (Files.notExists(configPath)) {
				entries.clear();
				entries.addAll(defaultEntries());
				save();
			} else {
				readIntoEntries();
			}
			applyTo(engine);
		} catch (IOException | JsonSyntaxException | IllegalArgumentException ex) {
			Deltasound.LOGGER.error("Failed to load {}; falling back to built-in defaults", configPath, ex);
			entries.clear();
			entries.addAll(defaultEntries());
			applyTo(engine);
		}
	}

	public void saveAndApply(ChatTriggerEngine engine) throws IOException, PatternSyntaxException {
		save();
		applyTo(engine);
	}

	public void save() throws IOException {
		Files.createDirectories(configDir);
		TriggerFile file = new TriggerFile();
		file.triggers = new ArrayList<>();
		for (TriggerEntry entry : entries) {
			file.triggers.add(TriggerEntry.copyOf(entry));
		}
		try (Writer writer = Files.newBufferedWriter(configPath)) {
			GSON.toJson(file, writer);
		}
		Deltasound.LOGGER.info("Saved {} trigger(s) to {}", entries.size(), configPath);
	}

	public void applyTo(ChatTriggerEngine engine) {
		List<ChatTrigger> compiled = new ArrayList<>();
		for (TriggerEntry entry : entries) {
			compiled.add(entry.compile());
		}
		engine.setTriggers(compiled);
	}

	private void readIntoEntries() throws IOException {
		try (Reader reader = Files.newBufferedReader(configPath)) {
			TriggerFile file = GSON.fromJson(reader, TriggerFile.class);
			entries.clear();
			if (file == null || file.triggers == null || file.triggers.isEmpty()) {
				entries.addAll(defaultEntries());
				return;
			}
			for (TriggerEntry entry : file.triggers) {
				if ((entry.match == null || entry.match.isBlank()) && entry.pattern != null && !entry.pattern.isBlank()) {
					entry.match = entry.pattern;
					if (entry.mode == null || entry.mode.isBlank()) {
						entry.mode = MatchMode.REGEX.name();
					}
				}
				entry.pattern = null;
				if (entry.mode == null || entry.mode.isBlank()) {
					entry.mode = MatchMode.CONTAINS.name();
				}
				entries.add(entry);
			}
		}
	}

	private static List<TriggerEntry> defaultEntries() {
		TriggerEntry rng = TriggerEntry.createDefault();
		rng.id = "rng_drop";
		rng.match = "RNG Drop!";
		rng.mode = MatchMode.CONTAINS.name();
		rng.sound = "minecraft:entity.player.levelup";
		rng.enabled = true;
		rng.ignoreOverlay = true;
		rng.cooldownMs = 1500L;
		rng.requireLocalPlayerName = false;
		return List.of(rng);
	}

	private static final class TriggerFile {
		@SerializedName("triggers")
		List<TriggerEntry> triggers;
	}
}
