package dev.deltasound.client.gui;

import dev.deltasound.Deltasound;
import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.client.config.TriggerEntry;
import dev.deltasound.client.sound.UserSoundPack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Create-trigger form: name, chat activator, and sound (upload or Minecraft picker).
 */
public final class AddTriggerScreen extends Screen {
	private final Screen parent;
	private final DeltasoundClient mod;

	private HeaderAndFooterLayout layout;
	private EditBox nameBox;
	private EditBox matchBox;
	private StringWidget soundLabel;
	private StringWidget status;
	private String selectedSound = "minecraft:entity.player.levelup";

	public AddTriggerScreen(Screen parent, DeltasoundClient mod) {
		super(Component.literal("Add trigger"));
		this.parent = parent;
		this.mod = mod;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 32, 64);
		layout.addTitleHeader(title, font);

		LinearLayout body = LinearLayout.vertical().spacing(8);
		body.defaultCellSetting().alignHorizontallyCenter();

		status = body.addChild(new StringWidget(Component.literal("Name the trigger, set chat text, then choose a sound."), font));

		nameBox = body.addChild(new EditBox(font, 300, 20, Component.literal("Name")));
		nameBox.setMaxLength(64);
		nameBox.setHint(Component.literal("Readable name"));
		nameBox.setValue("RNG Drop");

		matchBox = body.addChild(new EditBox(font, 300, 20, Component.literal("Activator")));
		matchBox.setMaxLength(256);
		matchBox.setHint(Component.literal("Chat text that activates this trigger"));
		matchBox.setValue("RNG Drop!");

		soundLabel = body.addChild(new StringWidget(Component.literal(soundLabelText()), font));

		LinearLayout soundRow = LinearLayout.horizontal().spacing(6);
		soundRow.addChild(Button.builder(Component.literal("Upload sound"), b -> openFilePicker()).width(110).build());
		soundRow.addChild(Button.builder(Component.literal("Minecraft sounds"), b ->
				minecraft.setScreen(new SoundPickerScreen(this, mod, this::setSound))
		).width(130).build());
		soundRow.addChild(Button.builder(Component.literal("Test sound"), b -> mod.soundBridge().play(selectedSound)).width(90).build());
		body.addChild(soundRow);

		layout.addToContents(body);

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Create"), b -> create()).width(100).build());
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

	private void openFilePicker() {
		Thread picker = new Thread(() -> {
			FileDialog dialog = new FileDialog((Frame) null, "Select sound (.ogg / .mp3)", FileDialog.LOAD);
			dialog.setFile("*.ogg;*.mp3");
			dialog.setVisible(true);
			String file = dialog.getFile();
			String dir = dialog.getDirectory();
			if (file == null || dir == null) {
				minecraft.execute(() -> setStatus("Upload cancelled."));
				return;
			}
			Path path = Path.of(dir, file);
			minecraft.execute(() -> importSound(path));
		}, "deltasound-file-dialog");
		picker.setDaemon(true);
		picker.start();
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

	private void create() {
		String name = nameBox.getValue().trim();
		String match = matchBox.getValue();
		if (name.isBlank()) {
			setStatus("Please enter a name.");
			return;
		}
		if (match.isBlank()) {
			setStatus("Please enter chat activator text.");
			return;
		}

		TriggerEntry entry = TriggerEntry.create(name, match, selectedSound);
		mod.configLoader().entries().add(entry);
		try {
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
}
