package dev.deltasound.client.command;

import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.client.gui.DeltasoundScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;

public final class DeltasoundCommands {
	private DeltasoundCommands() {
	}

	public static void register(DeltasoundClient mod) {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			var open = ClientCommands.literal("deltasound")
					.executes(context -> openGui(mod));
			dispatcher.register(open);
			dispatcher.register(ClientCommands.literal("ds").executes(context -> openGui(mod)));
		});
	}

	private static int openGui(DeltasoundClient mod) {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> client.setScreen(new DeltasoundScreen(client.screen, mod)));
		return 1;
	}
}
