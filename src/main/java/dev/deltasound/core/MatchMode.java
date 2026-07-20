package dev.deltasound.core;

/**
 * How a chat trigger compares against a chat line.
 */
public enum MatchMode {
	/** Plain substring match (GUI-friendly). */
	CONTAINS,
	/** Java regex ({@link java.util.regex.Pattern#find()}). */
	REGEX;

	public static MatchMode fromConfig(String raw) {
		if (raw == null || raw.isBlank()) {
			return CONTAINS;
		}
		return MatchMode.valueOf(raw.trim().toUpperCase());
	}
}
