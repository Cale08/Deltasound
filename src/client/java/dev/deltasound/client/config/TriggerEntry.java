package dev.deltasound.client.config;

import com.google.gson.annotations.SerializedName;
import dev.deltasound.core.ChatTrigger;
import dev.deltasound.core.MatchMode;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

/**
 * Editable trigger row used by config IO and the in-game GUI.
 */
public final class TriggerEntry {
	public String id;
	/** Human-readable label shown in the list. */
	public String name;
	/** Chat text that activates this trigger (substring match by default). */
	public String match;
	/** Legacy field from early configs; migrated into {@link #match}. */
	public String pattern;
	public String mode;
	public String sound;
	/** 0.0 – 1.0 playback volume. */
	public Float volume;
	public Boolean enabled;
	@SerializedName("ignore_overlay")
	public Boolean ignoreOverlay;
	@SerializedName("cooldown_ms")
	public Long cooldownMs;
	@SerializedName("require_local_player_name")
	public Boolean requireLocalPlayerName;

	public static TriggerEntry createBlank() {
		TriggerEntry entry = new TriggerEntry();
		entry.id = "trigger_" + UUID.randomUUID().toString().substring(0, 8);
		entry.name = "New trigger";
		entry.match = "";
		entry.mode = MatchMode.CONTAINS.name();
		entry.sound = "minecraft:entity.player.levelup";
		entry.volume = 1.0f;
		entry.enabled = true;
		entry.ignoreOverlay = true;
		entry.cooldownMs = 750L;
		entry.requireLocalPlayerName = false;
		return entry;
	}

	public static TriggerEntry create(String name, String match, String sound, float volume) {
		TriggerEntry entry = createBlank();
		entry.name = name == null || name.isBlank() ? "Untitled" : name.trim();
		entry.match = match == null ? "" : match;
		entry.sound = sound == null || sound.isBlank() ? "minecraft:entity.player.levelup" : sound.trim();
		entry.volume = ChatTrigger.clampVolume(volume);
		entry.id = slug(entry.name) + "_" + UUID.randomUUID().toString().substring(0, 4);
		return entry;
	}

	public static TriggerEntry copyOf(TriggerEntry other) {
		TriggerEntry entry = new TriggerEntry();
		entry.id = other.id;
		entry.name = other.name;
		entry.match = other.match;
		entry.mode = other.mode;
		entry.sound = other.sound;
		entry.volume = other.volume;
		entry.enabled = other.enabled;
		entry.ignoreOverlay = other.ignoreOverlay;
		entry.cooldownMs = other.cooldownMs;
		entry.requireLocalPlayerName = other.requireLocalPlayerName;
		return entry;
	}

	public String displayName() {
		if (name != null && !name.isBlank()) {
			return name.trim();
		}
		if (id != null && !id.isBlank()) {
			return id;
		}
		return "Untitled";
	}

	public float volumeOrDefault() {
		return ChatTrigger.clampVolume(volume == null ? 1.0f : volume);
	}

	public MatchMode matchMode() {
		return MatchMode.fromConfig(mode);
	}

	public ChatTrigger compile() throws PatternSyntaxException {
		return ChatTrigger.compile(
				id == null || id.isBlank() ? "unnamed" : id.trim(),
				match == null ? "" : match,
				matchMode(),
				sound == null || sound.isBlank() ? "minecraft:entity.player.levelup" : sound.trim(),
				volumeOrDefault(),
				enabled == null || enabled,
				ignoreOverlay == null || ignoreOverlay,
				cooldownMs == null ? 0L : cooldownMs,
				requireLocalPlayerName != null && requireLocalPlayerName
		);
	}

	private static String slug(String value) {
		String slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
		slug = slug.replaceAll("^_|_$", "");
		return slug.isBlank() ? "trigger" : slug;
	}
}
