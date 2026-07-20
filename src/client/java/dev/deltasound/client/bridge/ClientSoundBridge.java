package dev.deltasound.client.bridge;

import dev.deltasound.Deltasound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Thin sound playback adapter. Port checklist: {@link SimpleSoundInstance},
 * {@link SoundEvent#createVariableRangeEvent(Identifier)}, {@link Identifier} factory.
 */
public final class ClientSoundBridge {
	public void play(String soundId) {
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

		SoundEvent event = SoundEvent.createVariableRangeEvent(id);
		client.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0f, 1.0f));
		Deltasound.LOGGER.debug("Playing sound {} ({})", id, SoundSource.MASTER);
	}
}
