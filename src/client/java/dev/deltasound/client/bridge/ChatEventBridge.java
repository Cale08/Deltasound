package dev.deltasound.client.bridge;

import dev.deltasound.Deltasound;
import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.core.ChatTrigger;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Thin Fabric/Minecraft adapter for inbound chat. Port checklist: verify
 * {@link ClientReceiveMessageEvents} package + {@link Component#getString()} behavior.
 */
public final class ChatEventBridge {
	private ChatEventBridge() {
	}

	public static void register(DeltasoundClient mod) {
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handlePlain(mod, message.getString(), overlay));
	}

	/**
	 * Shared evaluation path for real chat and client-side test messages.
	 */
	public static void handlePlain(DeltasoundClient mod, String plain, boolean overlay) {
		if (plain == null || plain.isBlank()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();
		String localName = client.player != null ? client.player.getGameProfile().name() : null;
		long now = System.currentTimeMillis();

		mod.triggerEngine()
				.evaluate(plain, overlay, localName, now)
				.ifPresent(match -> play(mod, match));
	}

	public static void runClientTest(DeltasoundClient mod, String triggerText) {
		Minecraft client = Minecraft.getInstance();
		String line = "Deltasound Test>> " + triggerText;
		Component message = Component.literal(line);
		if (client.gui != null && client.gui.getChat() != null) {
			client.gui.getChat().addClientSystemMessage(message);
		}
		handlePlain(mod, line, false);
	}

	private static void play(DeltasoundClient mod, ChatTrigger.Match match) {
		String soundId = match.trigger().soundId();
		Deltasound.LOGGER.info(
				"Trigger '{}' matched -> {}",
				match.trigger().id(),
				soundId
		);
		mod.soundBridge().play(soundId, match.trigger().volume());
	}
}
