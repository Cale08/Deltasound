package dev.deltasound.client;

import dev.deltasound.Deltasound;
import dev.deltasound.client.bridge.ChatEventBridge;
import dev.deltasound.client.bridge.ClientSoundBridge;
import dev.deltasound.client.command.DeltasoundCommands;
import dev.deltasound.client.config.TriggerConfigLoader;
import dev.deltasound.client.sound.UserSoundPack;
import dev.deltasound.core.ChatTriggerEngine;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class DeltasoundClient implements ClientModInitializer {
	private static DeltasoundClient instance;

	private final ChatTriggerEngine triggerEngine = new ChatTriggerEngine();
	private final ClientSoundBridge soundBridge = new ClientSoundBridge();
	private final TriggerConfigLoader configLoader = new TriggerConfigLoader();

	public static DeltasoundClient get() {
		return instance;
	}

	public ChatTriggerEngine triggerEngine() {
		return triggerEngine;
	}

	public ClientSoundBridge soundBridge() {
		return soundBridge;
	}

	public TriggerConfigLoader configLoader() {
		return configLoader;
	}

	@Override
	public void onInitializeClient() {
		instance = this;
		configLoader.loadInto(triggerEngine);
		ChatEventBridge.register(this);
		DeltasoundCommands.register(this);

		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			try {
				UserSoundPack.ensurePack(client);
				if (!UserSoundPack.listImportedSoundIds(client).isEmpty()) {
					UserSoundPack.enableAndReload(client);
				}
			} catch (Exception ex) {
				Deltasound.LOGGER.warn("Could not prepare user sound pack on startup", ex);
			}
		});

		Deltasound.LOGGER.info("Deltasound ready ({} chat trigger(s))", triggerEngine.triggers().size());
	}
}
