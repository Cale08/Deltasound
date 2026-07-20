package dev.deltasound.client.gui;

import dev.deltasound.client.DeltasoundClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Searchable list of loaded Minecraft / resource-pack sound ids.
 */
public final class SoundPickerScreen extends Screen {
	private final Screen parent;
	private final DeltasoundClient mod;
	private final Consumer<String> onPick;

	private HeaderAndFooterLayout layout;
	private EditBox searchBox;
	private SoundList list;
	private List<String> allSounds = List.of();

	public SoundPickerScreen(Screen parent, DeltasoundClient mod, Consumer<String> onPick) {
		super(Component.literal("Choose sound"));
		this.parent = parent;
		this.mod = mod;
		this.onPick = onPick;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 56, 56);
		layout.addTitleHeader(title, font);

		allSounds = loadSounds();

		LinearLayout header = LinearLayout.vertical().spacing(4);
		header.defaultCellSetting().alignHorizontallyCenter();
		searchBox = header.addChild(new EditBox(font, 280, 20, Component.literal("Search")));
		searchBox.setHint(Component.literal("Search sounds…"));
		searchBox.setResponder(value -> rebuildList());
		layout.addToHeader(header);

		list = new SoundList(minecraft, layout.getContentHeight(), 24);
		layout.addToContents(list);
		rebuildList();

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Select"), b -> selectFocused()).width(100).build());
		footer.addChild(Button.builder(Component.literal("Cancel"), b -> onClose()).width(100).build());
		layout.addToFooter(footer);

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
		setInitialFocus(searchBox);
	}

	@Override
	protected void repositionElements() {
		if (layout != null) {
			layout.arrangeElements();
			if (list != null) {
				list.updateSize(width, layout);
			}
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private List<String> loadSounds() {
		List<String> sounds = new ArrayList<>();
		for (Identifier id : minecraft.getSoundManager().getAvailableSounds()) {
			sounds.add(id.toString());
		}
		sounds.addAll(mod.customSounds().soundIds());
		sounds.sort(Comparator.naturalOrder());
		return sounds;
	}

	private void rebuildList() {
		if (list == null) {
			return;
		}
		String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
		list.resetEntries();
		for (String sound : allSounds) {
			if (query.isEmpty() || sound.toLowerCase(Locale.ROOT).contains(query)) {
				list.addRow(new SoundRow(sound));
			}
		}
	}

	private void selectFocused() {
		SoundRow selected = list.getSelected();
		if (selected == null) {
			return;
		}
		onPick.accept(selected.soundId);
		minecraft.setScreen(parent);
	}

	private final class SoundList extends ObjectSelectionList<SoundRow> {
		SoundList(Minecraft client, int height, int itemHeight) {
			super(client, SoundPickerScreen.this.width, height, 0, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return Math.min(420, SoundPickerScreen.this.width - 40);
		}

		void resetEntries() {
			clearEntries();
		}

		void addRow(SoundRow row) {
			addEntry(row);
		}
	}

	private final class SoundRow extends ObjectSelectionList.Entry<SoundRow> {
		private final String soundId;

		SoundRow(String soundId) {
			this.soundId = soundId;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int color = hovered || equals(list.getSelected()) ? 0xFFFFFF : 0xC0C0C0;
			graphics.text(font, soundId, getContentX() + 4, getContentY() + 6, color, false);
		}

		@Override
		public Component getNarration() {
			return Component.literal(soundId);
		}

		@Override
		public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
			list.setSelected(this);
			if (doubleClick) {
				selectFocused();
			} else {
				mod.soundBridge().play(soundId);
			}
			return true;
		}
	}
}
