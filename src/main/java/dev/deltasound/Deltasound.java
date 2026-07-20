package dev.deltasound;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared mod constants. Keep this class free of Minecraft game code so version
 * ports only touch {@code client.bridge} (and similar) packages.
 */
public final class Deltasound {
	public static final String MOD_ID = "deltasound";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Deltasound() {
	}
}
