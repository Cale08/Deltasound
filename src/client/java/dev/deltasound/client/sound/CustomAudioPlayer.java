package dev.deltasound.client.sound;

import dev.deltasound.Deltasound;
import javazoom.jl.player.Player;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Plays custom files that Minecraft's sound engine cannot load (notably MP3).
 * Volume is accepted for API consistency; stock JLayer has no reliable gain control,
 * so volume {@code <= 0} skips playback and other values play at device default.
 */
public final class CustomAudioPlayer {
	private final AtomicReference<Thread> active = new AtomicReference<>();
	private final AtomicReference<Player> activeMp3 = new AtomicReference<>();

	public void play(Path path) {
		play(path, 1.0f);
	}

	public void play(Path path, float volume) {
		stop();
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		Thread thread = new Thread(() -> {
			try {
				if (name.endsWith(".mp3")) {
					if (volume <= 0.001f) {
						return;
					}
					playMp3(path);
				} else {
					Deltasound.LOGGER.warn("CustomAudioPlayer only supports MP3; got {}", path);
				}
			} catch (Exception ex) {
				Deltasound.LOGGER.error("Failed playing custom audio {}", path, ex);
			} finally {
				active.compareAndSet(Thread.currentThread(), null);
			}
		}, "deltasound-audio");
		thread.setDaemon(true);
		active.set(thread);
		thread.start();
	}

	public void stop() {
		Player player = activeMp3.getAndSet(null);
		if (player != null) {
			try {
				player.close();
			} catch (Exception ignored) {
			}
		}
		Thread thread = active.getAndSet(null);
		if (thread != null) {
			thread.interrupt();
		}
	}

	private void playMp3(Path path) throws Exception {
		try (InputStream in = new BufferedInputStream(Files.newInputStream(path))) {
			Player player = new Player(in);
			activeMp3.set(player);
			player.play();
		} finally {
			activeMp3.set(null);
		}
	}
}
