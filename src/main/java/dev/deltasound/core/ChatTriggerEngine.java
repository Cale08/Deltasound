package dev.deltasound.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates chat lines against registered triggers with simple cooldowns.
 */
public final class ChatTriggerEngine {
	private final List<ChatTrigger> triggers = new ArrayList<>();
	private final Map<String, Long> lastFiredAt = new HashMap<>();

	public void setTriggers(List<ChatTrigger> next) {
		triggers.clear();
		triggers.addAll(next);
		lastFiredAt.keySet().retainAll(next.stream().map(ChatTrigger::id).toList());
	}

	public List<ChatTrigger> triggers() {
		return List.copyOf(triggers);
	}

	public Optional<ChatTrigger.Match> evaluate(String plainText, boolean overlay, String localPlayerName, long nowMs) {
		for (ChatTrigger trigger : triggers) {
			if (overlay && trigger.ignoreOverlay()) {
				continue;
			}

			Optional<ChatTrigger.Match> match = trigger.tryMatch(plainText);
			if (match.isEmpty()) {
				continue;
			}

			ChatTrigger.Match hit = match.get();
			if (trigger.requireLocalPlayerName()) {
				if (localPlayerName == null || localPlayerName.isBlank()) {
					continue;
				}
				if (!localPlayerName.equalsIgnoreCase(hit.playerName())) {
					continue;
				}
			}

			Long last = lastFiredAt.get(trigger.id());
			if (last != null && nowMs - last < trigger.cooldownMs()) {
				continue;
			}

			lastFiredAt.put(trigger.id(), nowMs);
			return Optional.of(hit);
		}

		return Optional.empty();
	}
}
