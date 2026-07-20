package dev.deltasound.client.config;

import com.google.gson.annotations.SerializedName;
import dev.deltasound.core.ChatTrigger;
import dev.deltasound.core.MatchMode;

import java.util.UUID;
import java.util.regex.PatternSyntaxException;

/**
 * Editable trigger row used by config IO and the in-game GUI.
 */
public final class TriggerEntry {
	public String id;
	/** User-facing detection string (substring or regex depending on {@link #mode}). */
	public String match;
	/** Legacy field from early configs; migrated into {@link #match}. */
	public String pattern;
	public String mode;
	public String sound;
	public Boolean enabled;
	@SerializedName("ignore_overlay")
	public Boolean ignoreOverlay;
	@SerializedName("cooldown_ms")
	public Long cooldownMs;
	@SerializedName("require_local_player_name")
	public Boolean requireLocalPlayerName;

	public static TriggerEntry createDefault() {
		TriggerEntry entry = new TriggerEntry();
		entry.id = "trigger_" + UUID.randomUUID().toString().substring(0, 8);
		entry.match = "RNG Drop!";
		entry.mode = MatchMode.CONTAINS.name();
		entry.sound = "minecraft:entity.player.levelup";
		entry.enabled = true;
		entry.ignoreOverlay = true;
		entry.cooldownMs = 1500L;
		entry.requireLocalPlayerName = false;
		return entry;
	}

	public static TriggerEntry copyOf(TriggerEntry other) {
		TriggerEntry entry = new TriggerEntry();
		entry.id = other.id;
		entry.match = other.match;
		entry.mode = other.mode;
		entry.sound = other.sound;
		entry.enabled = other.enabled;
		entry.ignoreOverlay = other.ignoreOverlay;
		entry.cooldownMs = other.cooldownMs;
		entry.requireLocalPlayerName = other.requireLocalPlayerName;
		return entry;
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
				enabled == null || enabled,
				ignoreOverlay == null || ignoreOverlay,
				cooldownMs == null ? 0L : cooldownMs,
				requireLocalPlayerName != null && requireLocalPlayerName
		);
	}
}
