package dev.deltasound.client.gui;

import dev.deltasound.Deltasound;
import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.client.config.TriggerEntry;
import dev.deltasound.client.sound.UserSoundPack;
import dev.deltasound.core.MatchMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * In-game configuration UI opened via {@code /deltasound} or {@code /ds}.
 */
public final class DeltasoundScreen extends Screen {
	private final Screen parent;
	private final DeltasoundClient mod;

	private HeaderAndFooterLayout layout;
	private StringWidget statusWidget;
	private StringWidget indexWidget;
	private EditBox matchBox;
	private EditBox soundBox;
	private EditBox idBox;
	private CycleButton<MatchMode> modeButton;
	private CycleButton<Boolean> enabledBox;

	private int selectedIndex;
	private boolean suppressFieldCallbacks;

	public DeltasoundScreen(Screen parent, DeltasoundClient mod) {
		super(Component.literal("Deltasound"));
		this.parent = parent;
		this.mod = mod;
		if (mod.configLoader().entries().isEmpty()) {
			mod.configLoader().entries().add(TriggerEntry.createDefault());
		}
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 32, 64);
		layout.addTitleHeader(title, font);

		LinearLayout body = LinearLayout.vertical().spacing(6);
		body.defaultCellSetting().alignHorizontallyCenter();

		indexWidget = body.addChild(new StringWidget(Component.empty(), font));
		statusWidget = body.addChild(new StringWidget(Component.literal("Edit detection text, pick a sound, then Save."), font));

		LinearLayout nav = LinearLayout.horizontal().spacing(6);
		nav.addChild(Button.builder(Component.literal("Prev"), b -> selectRelative(-1)).width(60).build());
		nav.addChild(Button.builder(Component.literal("Next"), b -> selectRelative(1)).width(60).build());
		nav.addChild(Button.builder(Component.literal("Add"), b -> addTrigger()).width(60).build());
		nav.addChild(Button.builder(Component.literal("Delete"), b -> deleteTrigger()).width(60).build());
		body.addChild(nav);

		idBox = body.addChild(new EditBox(font, 280, 20, Component.literal("Trigger id")));
		idBox.setMaxLength(64);
		idBox.setHint(Component.literal("Trigger id"));
		idBox.setResponder(value -> updateSelected(entry -> entry.id = value));

		matchBox = body.addChild(new EditBox(font, 280, 20, Component.literal("Detection text")));
		matchBox.setMaxLength(256);
		matchBox.setHint(Component.literal("Chat text to look for"));
		matchBox.setResponder(value -> updateSelected(entry -> entry.match = value));

		modeButton = body.addChild(
				CycleButton.<MatchMode>builder(mode -> Component.literal("Mode: " + mode.name()), MatchMode.CONTAINS)
						.withValues(MatchMode.CONTAINS, MatchMode.REGEX)
						.create(0, 0, 280, 20, Component.literal("Mode"), (button, value) ->
								updateSelected(entry -> entry.mode = value.name()))
		);

		soundBox = body.addChild(new EditBox(font, 280, 20, Component.literal("Sound id")));
		soundBox.setMaxLength(128);
		soundBox.setHint(Component.literal("minecraft:entity.player.levelup"));
		soundBox.setResponder(value -> updateSelected(entry -> entry.sound = value));

		enabledBox = body.addChild(
				CycleButton.booleanBuilder(Component.literal("On"), Component.literal("Off"), true)
						.create(0, 0, 280, 20, Component.literal("Enabled"), (button, value) ->
								updateSelected(entry -> entry.enabled = value))
		);

		LinearLayout actions = LinearLayout.horizontal().spacing(6);
		actions.addChild(Button.builder(Component.literal("Import .ogg"), b -> openImportDialog()).width(100).build());
		actions.addChild(Button.builder(Component.literal("Test sound"), b -> testSound()).width(90).build());
		actions.addChild(Button.builder(Component.literal("Save"), b -> saveConfig()).width(70).build());
		body.addChild(actions);

		layout.addToContents(body);

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Done"), b -> onClose()).width(100).build());
		layout.addToFooter(footer);

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		loadSelectedIntoFields();
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
	public void onFilesDrop(List<Path> paths) {
		for (Path path : paths) {
			if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg")) {
				importSound(path);
				return;
			}
		}
		setStatus("Drop an .ogg file to import it.");
	}

	private void selectRelative(int delta) {
		List<TriggerEntry> entries = mod.configLoader().entries();
		if (entries.isEmpty()) {
			return;
		}
		selectedIndex = Math.floorMod(selectedIndex + delta, entries.size());
		loadSelectedIntoFields();
	}

	private void addTrigger() {
		mod.configLoader().entries().add(TriggerEntry.createDefault());
		selectedIndex = mod.configLoader().entries().size() - 1;
		loadSelectedIntoFields();
		setStatus("Added trigger. Edit fields, then Save.");
	}

	private void deleteTrigger() {
		List<TriggerEntry> entries = mod.configLoader().entries();
		if (entries.isEmpty()) {
			return;
		}
		entries.remove(selectedIndex);
		if (entries.isEmpty()) {
			entries.add(TriggerEntry.createDefault());
			selectedIndex = 0;
		} else {
			selectedIndex = Math.min(selectedIndex, entries.size() - 1);
		}
		loadSelectedIntoFields();
		setStatus("Deleted trigger (remember to Save).");
	}

	private void loadSelectedIntoFields() {
		List<TriggerEntry> entries = mod.configLoader().entries();
		if (entries.isEmpty()) {
			return;
		}
		selectedIndex = Math.max(0, Math.min(selectedIndex, entries.size() - 1));
		TriggerEntry entry = entries.get(selectedIndex);

		suppressFieldCallbacks = true;
		idBox.setValue(nullToEmpty(entry.id));
		matchBox.setValue(nullToEmpty(entry.match));
		soundBox.setValue(nullToEmpty(entry.sound));
		modeButton.setValue(entry.matchMode());
		enabledBox.setValue(entry.enabled == null || entry.enabled);
		suppressFieldCallbacks = false;

		indexWidget.setMessage(Component.literal(
				"Trigger " + (selectedIndex + 1) + " / " + entries.size()
		));
	}

	private void updateSelected(java.util.function.Consumer<TriggerEntry> mutator) {
		if (suppressFieldCallbacks) {
			return;
		}
		List<TriggerEntry> entries = mod.configLoader().entries();
		if (entries.isEmpty()) {
			return;
		}
		mutator.accept(entries.get(selectedIndex));
	}

	private void saveConfig() {
		try {
			pushFieldsToSelected();
			mod.configLoader().saveAndApply(mod.triggerEngine());
			setStatus("Saved " + mod.configLoader().entries().size() + " trigger(s).");
		} catch (Exception ex) {
			Deltasound.LOGGER.error("Failed saving triggers", ex);
			setStatus("Save failed: " + ex.getMessage());
		}
	}

	private void pushFieldsToSelected() {
		updateSelected(entry -> {
			entry.id = idBox.getValue().trim();
			entry.match = matchBox.getValue();
			entry.sound = soundBox.getValue().trim();
			entry.mode = modeButton.getValue().name();
			entry.enabled = enabledBox.getValue();
		});
	}

	private void testSound() {
		pushFieldsToSelected();
		String sound = soundBox.getValue().trim();
		if (sound.isEmpty()) {
			setStatus("Set a sound id first.");
			return;
		}
		mod.soundBridge().play(sound);
		setStatus("Playing " + sound);
	}

	private void openImportDialog() {
		Thread picker = new Thread(() -> {
			FileDialog dialog = new FileDialog((Frame) null, "Import OGG sound", FileDialog.LOAD);
			dialog.setFile("*.ogg");
			dialog.setVisible(true);
			String file = dialog.getFile();
			String dir = dialog.getDirectory();
			if (file == null || dir == null) {
				minecraft.execute(() -> setStatus("Import cancelled."));
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
			String soundId = UserSoundPack.importOgg(Minecraft.getInstance(), path);
			soundBox.setValue(soundId);
			updateSelected(entry -> entry.sound = soundId);
			setStatus("Imported " + soundId + " (pack enabled).");
		} catch (IOException ex) {
			Deltasound.LOGGER.error("Sound import failed", ex);
			setStatus("Import failed: " + ex.getMessage());
		}
	}

	private void setStatus(String message) {
		if (statusWidget != null) {
			statusWidget.setMessage(Component.literal(message));
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
