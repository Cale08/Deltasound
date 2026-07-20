package dev.deltasound.core;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Pure Java chat trigger definition. No Minecraft types — survives version ports.
 */
public final class ChatTrigger {
	private final String id;
	private final String matchText;
	private final MatchMode mode;
	private final Pattern pattern;
	private final String soundId;
	private final float volume;
	private final boolean enabled;
	private final boolean ignoreOverlay;
	private final long cooldownMs;
	private final boolean requireLocalPlayerName;

	public ChatTrigger(
			String id,
			String matchText,
			MatchMode mode,
			Pattern pattern,
			String soundId,
			float volume,
			boolean enabled,
			boolean ignoreOverlay,
			long cooldownMs,
			boolean requireLocalPlayerName
	) {
		this.id = Objects.requireNonNull(id, "id");
		this.matchText = Objects.requireNonNull(matchText, "matchText");
		this.mode = Objects.requireNonNull(mode, "mode");
		this.pattern = Objects.requireNonNull(pattern, "pattern");
		this.soundId = Objects.requireNonNull(soundId, "soundId");
		this.volume = clampVolume(volume);
		this.enabled = enabled;
		this.ignoreOverlay = ignoreOverlay;
		this.cooldownMs = Math.max(0L, cooldownMs);
		this.requireLocalPlayerName = requireLocalPlayerName;
	}

	public static ChatTrigger compile(
			String id,
			String matchText,
			MatchMode mode,
			String soundId,
			float volume,
			boolean enabled,
			boolean ignoreOverlay,
			long cooldownMs,
			boolean requireLocalPlayerName
	) throws PatternSyntaxException {
		MatchMode resolved = mode == null ? MatchMode.CONTAINS : mode;
		String text = Objects.requireNonNull(matchText, "matchText");
		Pattern pattern = switch (resolved) {
			case CONTAINS -> Pattern.compile(Pattern.quote(text), Pattern.CASE_INSENSITIVE);
			case REGEX -> Pattern.compile(text);
		};
		return new ChatTrigger(
				id,
				text,
				resolved,
				pattern,
				soundId,
				volume,
				enabled,
				ignoreOverlay,
				cooldownMs,
				requireLocalPlayerName
		);
	}

	public static float clampVolume(float volume) {
		if (Float.isNaN(volume)) {
			return 1.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, volume));
	}

	public String id() {
		return id;
	}

	public String matchText() {
		return matchText;
	}

	public MatchMode mode() {
		return mode;
	}

	public String soundId() {
		return soundId;
	}

	public float volume() {
		return volume;
	}

	public boolean enabled() {
		return enabled;
	}

	public boolean ignoreOverlay() {
		return ignoreOverlay;
	}

	public long cooldownMs() {
		return cooldownMs;
	}

	public boolean requireLocalPlayerName() {
		return requireLocalPlayerName;
	}

	public Optional<Match> tryMatch(String plainText) {
		if (!enabled || plainText == null || plainText.isEmpty() || matchText.isEmpty()) {
			return Optional.empty();
		}

		if (mode == MatchMode.CONTAINS) {
			if (!plainText.toLowerCase(Locale.ROOT).contains(matchText.toLowerCase(Locale.ROOT))) {
				return Optional.empty();
			}
			return Optional.of(new Match(this, "", "", plainText));
		}

		Matcher matcher = pattern.matcher(plainText);
		if (!matcher.find()) {
			return Optional.empty();
		}

		String player = groupOrEmpty(matcher, 1);
		String detail = groupOrEmpty(matcher, 2);
		return Optional.of(new Match(this, player, detail, plainText));
	}

	private static String groupOrEmpty(Matcher matcher, int group) {
		if (group < 0 || group > matcher.groupCount()) {
			return "";
		}
		String value = matcher.group(group);
		return value == null ? "" : value;
	}

	public record Match(ChatTrigger trigger, String playerName, String detail, String rawText) {
	}
}
