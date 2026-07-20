package dev.deltasound.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTriggerEngineTest {
	@Test
	void matchesContainsLine() {
		ChatTriggerEngine engine = new ChatTriggerEngine();
		engine.setTriggers(List.of(
				ChatTrigger.compile(
						"rng_drop",
						"RNG Drop!",
						MatchMode.CONTAINS,
						"minecraft:entity.player.levelup",
						true,
						true,
						0L,
						false
				)
		));

		Optional<ChatTrigger.Match> match = engine.evaluate(
				"RNG Drop! Steve unlocked Hyperion!",
				false,
				"Steve",
				1L
		);

		assertTrue(match.isPresent());
	}

	@Test
	void matchesRegexCaptureGroups() {
		ChatTriggerEngine engine = new ChatTriggerEngine();
		engine.setTriggers(List.of(
				ChatTrigger.compile(
						"rng_drop",
						"RNG Drop! (.+) unlocked (.+)!",
						MatchMode.REGEX,
						"minecraft:entity.player.levelup",
						true,
						true,
						0L,
						false
				)
		));

		Optional<ChatTrigger.Match> match = engine.evaluate(
				"RNG Drop! Steve unlocked Hyperion!",
				false,
				"Steve",
				1L
		);

		assertTrue(match.isPresent());
		assertEquals("Steve", match.get().playerName());
		assertEquals("Hyperion", match.get().detail());
	}

	@Test
	void respectsSelfOnlyFilter() {
		ChatTriggerEngine engine = new ChatTriggerEngine();
		engine.setTriggers(List.of(
				ChatTrigger.compile(
						"rng_drop",
						"RNG Drop! (.+) unlocked (.+)!",
						MatchMode.REGEX,
						"minecraft:entity.player.levelup",
						true,
						true,
						0L,
						true
				)
		));

		assertTrue(engine.evaluate(
				"RNG Drop! Alex unlocked Hyperion!",
				false,
				"Steve",
				1L
		).isEmpty());
	}
}
