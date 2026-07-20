package dev.deltasound.client.bridge;

import dev.deltasound.Deltasound;
import dev.deltasound.client.sound.CustomAudioPlayer;
import dev.deltasound.client.sound.CustomSoundLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Thin sound playback adapter. Port checklist: {@link SimpleSoundInstance},
 * {@link SoundEvent#createVariableRangeEvent(Identifier)}, {@link Identifier} factory.
 */
public final class ClientSoundBridge {
	private static final float VOLUME = 1.0f;
	private static final float PITCH = 1.0f;

	private final CustomSoundLibrary customSounds;
	private final CustomAudioPlayer customAudioPlayer = new CustomAudioPlayer();

	public ClientSoundBridge(CustomSoundLibrary customSounds) {
		this.customSounds = customSounds;
	}

	public void play(String soundId) {
		if (soundId == null || soundId.isBlank()) {
			return;
		}

		if (customSounds.isCustom(soundId)) {
			Path path = customSounds.pathFor(soundId).orElse(null);
			if (path == null || !Files.isRegularFile(path)) {
				Deltasound.LOGGER.warn("Custom sound '{}' is missing on disk", soundId);
				return;
			}
			String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
			if (lower.endsWith(".mp3")) {
				customAudioPlayer.play(path);
				Deltasound.LOGGER.info("Playing custom MP3 {}", path.getFileName());
				return;
			}
			// OGG customs also live in the user resource pack under deltasound:<name>
		}

		Identifier id;
		try {
			id = Identifier.parse(soundId);
		} catch (RuntimeException ex) {
			Deltasound.LOGGER.warn("Invalid sound id '{}': {}", soundId, ex.getMessage());
			return;
		}

		Minecraft client = Minecraft.getInstance();
		if (client.getSoundManager() == null) {
			return;
		}

		WeighedSoundEvents registered = client.getSoundManager().getSoundEvent(id);
		if (registered == null) {
			Deltasound.LOGGER.warn(
					"Sound '{}' is not loaded. Pick a Minecraft sound or import an audio file in /ds.",
					id
			);
			return;
		}

		SoundEvent event = SoundEvent.createVariableRangeEvent(id);

		if (client.player != null && client.level != null) {
			client.level.playLocalSound(
					client.player.getX(),
					client.player.getY(),
					client.player.getZ(),
					event,
					SoundSource.MASTER,
					VOLUME,
					PITCH,
					false
			);
			Deltasound.LOGGER.info("Playing {} at player (Master)", id);
			return;
		}

		SimpleSoundInstance instance = new SimpleSoundInstance(
				id,
				SoundSource.MASTER,
				VOLUME,
				PITCH,
				RandomSource.create(),
				false,
				0,
				SoundInstance.Attenuation.NONE,
				0.0,
				0.0,
				0.0,
				true
		);
		SoundEngine.PlayResult result = client.getSoundManager().play(instance);
		Deltasound.LOGGER.info("Playing {} (Master, menu fallback) -> {}", id, result);
	}
}
