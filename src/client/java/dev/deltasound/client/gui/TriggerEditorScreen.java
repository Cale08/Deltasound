package dev.deltasound.client.gui;

import dev.deltasound.Deltasound;
import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.client.config.TriggerEntry;
import dev.deltasound.client.sound.UserSoundPack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Shared create / edit form for chat triggers.
 */
public final class TriggerEditorScreen extends Screen {
	private final Screen parent;
	private final DeltasoundClient mod;
	private final TriggerEntry editing;
	private final boolean createMode;

	private HeaderAndFooterLayout layout;
	private EditBox nameBox;
	private EditBox matchBox;
	private StringWidget soundLabel;
	private StringWidget status;
	private VolumeSlider volumeSlider;
	private String selectedSound;
	private float selectedVolume;

	public static TriggerEditorScreen create(Screen parent, DeltasoundClient mod) {
		return new TriggerEditorScreen(parent, mod, null);
	}

	public static TriggerEditorScreen edit(Screen parent, DeltasoundClient mod, TriggerEntry entry) {
		return new TriggerEditorScreen(parent, mod, entry);
	}

	private TriggerEditorScreen(Screen parent, DeltasoundClient mod, TriggerEntry editing) {
		super(Component.literal(editing == null ? "Add trigger" : "Edit trigger"));
		this.parent = parent;
		this.mod = mod;
		this.editing = editing;
		this.createMode = editing == null;
		this.selectedSound = editing != null && editing.sound != null
				? editing.sound
				: "minecraft:entity.player.levelup";
		this.selectedVolume = editing != null ? editing.volumeOrDefault() : 1.0f;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 32, 64);
		layout.addTitleHeader(title, font);

		LinearLayout body = LinearLayout.vertical().spacing(8);
		body.defaultCellSetting().alignHorizontallyCenter();

		status = body.addChild(new StringWidget(
				Component.literal(createMode
						? "Name the trigger, set chat text, choose a sound and volume."
						: "Update this trigger, then save."),
				font
		));
		status.setMaxWidth(320, StringWidget.TextOverflow.CLAMPED);

		nameBox = body.addChild(new EditBox(font, 300, 20, Component.literal("Name")));
		nameBox.setMaxLength(64);
		nameBox.setHint(Component.literal("Readable name"));
		nameBox.setValue(editing != null ? nullToEmpty(editing.name) : "RNG Drop");

		matchBox = body.addChild(new EditBox(font, 300, 20, Component.literal("Activator")));
		matchBox.setMaxLength(256);
		matchBox.setHint(Component.literal("Chat text that activates this trigger"));
		matchBox.setValue(editing != null ? nullToEmpty(editing.match) : "RNG Drop!");

		soundLabel = body.addChild(new StringWidget(Component.literal(soundLabelText()), font));
		soundLabel.setMaxWidth(320, StringWidget.TextOverflow.CLAMPED);

		LinearLayout soundRow = LinearLayout.horizontal().spacing(6);
		soundRow.addChild(Button.builder(Component.literal("Browse files"), b -> openFileBrowser()).width(110).build());
		soundRow.addChild(Button.builder(Component.literal("Minecraft sounds"), b ->
				minecraft.setScreen(new SoundPickerScreen(this, mod, this::setSound))
		).width(130).build());
		soundRow.addChild(Button.builder(Component.literal("Test sound"), b ->
				mod.soundBridge().play(selectedSound, selectedVolume)
		).width(90).build());
		body.addChild(soundRow);

		volumeSlider = body.addChild(new VolumeSlider(0, 0, 300, 20, selectedVolume));
		body.addChild(new StringWidget(Component.literal("Drag to set playback volume."), font));

		layout.addToContents(body);

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(
				Component.literal(createMode ? "Create" : "Save"),
				b -> save()
		).width(100).build());
		footer.addChild(Button.builder(Component.literal("Cancel"), b -> onClose()).width(100).build());
		layout.addToFooter(footer);

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		setInitialFocus(nameBox);
	}

	@Override
	protected void repositionElements() {
		if (layout != null) {
			layout.arrangeElements();
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void onFilesDrop(java.util.List<Path> paths) {
		for (Path path : paths) {
			String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
			if (lower.endsWith(".ogg") || lower.endsWith(".mp3")) {
				importSound(path);
				return;
			}
		}
		setStatus("Drop an .ogg or .mp3 file.");
	}

	void setSound(String soundId) {
		selectedSound = soundId;
		if (soundLabel != null) {
			soundLabel.setMessage(Component.literal(soundLabelText()));
		}
	}

	private String soundLabelText() {
		return "Sound: " + selectedSound;
	}

	private void openFileBrowser() {
		Path start = FabricLoader.getInstance().getGameDir();
		minecraft.setScreen(new FileBrowserScreen(this, start, this::importSound));
	}

	private void importSound(Path path) {
		try {
			String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
			String soundId;
			if (lower.endsWith(".ogg")) {
				soundId = UserSoundPack.importOgg(Minecraft.getInstance(), path);
			} else if (lower.endsWith(".mp3")) {
				soundId = mod.customSounds().importFile(path);
			} else {
				setStatus("Only .ogg and .mp3 are supported.");
				return;
			}
			setSound(soundId);
			setStatus("Imported " + soundId);
		} catch (Exception ex) {
			Deltasound.LOGGER.error("Import failed", ex);
			setStatus("Import failed: " + ex.getMessage());
		}
	}

	private void save() {
		String name = nameBox.getValue().trim();
		String match = matchBox.getValue();
		selectedVolume = volumeSlider.volume();

		if (name.isBlank()) {
			setStatus("Please enter a name.");
			return;
		}
		if (match.isBlank()) {
			setStatus("Please enter chat activator text.");
			return;
		}

		try {
			if (createMode) {
				TriggerEntry entry = TriggerEntry.create(name, match, selectedSound, selectedVolume);
				mod.configLoader().entries().add(entry);
			} else {
				editing.name = name;
				editing.match = match;
				editing.sound = selectedSound;
				editing.volume = selectedVolume;
			}
			mod.configLoader().saveAndApply(mod.triggerEngine());
		} catch (Exception ex) {
			Deltasound.LOGGER.error("Failed saving trigger", ex);
			setStatus("Save failed: " + ex.getMessage());
			return;
		}

		if (parent instanceof DeltasoundScreen main) {
			main.refreshList();
		}
		minecraft.setScreen(parent);
	}

	private void setStatus(String message) {
		if (status != null) {
			status.setMessage(Component.literal(message));
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private final class VolumeSlider extends AbstractSliderButton {
		VolumeSlider(int x, int y, int width, int height, float initial) {
			super(x, y, width, height, Component.empty(), initial);
			updateMessage();
		}

		float volume() {
			return (float) value;
		}

		@Override
		protected void updateMessage() {
			int percent = (int) Math.round(value * 100.0);
			setMessage(Component.literal("Volume: " + percent + "%"));
			selectedVolume = (float) value;
		}

		@Override
		protected void applyValue() {
			selectedVolume = (float) value;
		}
	}
}
