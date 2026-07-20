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
				save();
			} else {
				readIntoEntries();
			}
			applyTo(engine);
		} catch (IOException | JsonSyntaxException | IllegalArgumentException ex) {
			Deltasound.LOGGER.error("Failed to load {}; starting with empty trigger list", configPath, ex);
			entries.clear();
			applyTo(engine);
		}
	}

	public void saveAndApply(ChatTriggerEngine engine) throws IOException {
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
			if (file == null || file.triggers == null) {
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
				if (entry.name == null || entry.name.isBlank()) {
					entry.name = entry.id != null ? entry.id : "Untitled";
				}
				entries.add(entry);
			}
		}
	}

	private static final class TriggerFile {
		@SerializedName("triggers")
		List<TriggerEntry> triggers;
	}
}
