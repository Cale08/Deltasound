package dev.deltasound.client.gui;

import dev.deltasound.client.DeltasoundClient;
import dev.deltasound.client.bridge.ChatEventBridge;
import dev.deltasound.client.config.TriggerEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;

import java.util.List;

/**
 * Main Deltasound config screen — trigger list with edit / test / delete.
 */
public final class DeltasoundScreen extends Screen {
	private static final int ROW_HEIGHT = 44;
	private static final int BUTTON_WIDTH = 48;
	private static final int BUTTON_GAP = 4;

	private final Screen parent;
	private final DeltasoundClient mod;

	private HeaderAndFooterLayout layout;
	private TriggerList list;
	private StringWidget status;

	public DeltasoundScreen(Screen parent, DeltasoundClient mod) {
		super(Component.literal("Deltasound"));
		this.parent = parent;
		this.mod = mod;
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 40, 56);
		layout.addTitleHeader(title, font);

		status = new StringWidget(Component.literal(statusText()), font);
		status.setMaxWidth(Math.max(80, width - 40), StringWidget.TextOverflow.CLAMPED);
		layout.addToHeader(status);

		list = new TriggerList(minecraft, layout.getContentHeight(), ROW_HEIGHT);
		layout.addToContents(list);
		refreshList();

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Add trigger"), b ->
				minecraft.setScreen(TriggerEditorScreen.create(this, mod))
		).width(120).build());
		footer.addChild(Button.builder(Component.literal("Done"), b -> onClose()).width(80).build());
		layout.addToFooter(footer);

		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	@Override
	protected void repositionElements() {
		if (layout != null) {
			layout.arrangeElements();
			if (list != null) {
				list.updateSize(width, layout);
			}
			if (status != null) {
				status.setMaxWidth(Math.max(80, width - 40), StringWidget.TextOverflow.CLAMPED);
			}
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	void refreshList() {
		if (list == null) {
			return;
		}
		list.resetEntries();
		for (TriggerEntry entry : mod.configLoader().entries()) {
			list.addRow(new TriggerRow(entry));
		}
		if (status != null) {
			status.setMessage(Component.literal(statusText()));
		}
	}

	private String statusText() {
		int count = mod.configLoader().entries().size();
		return count == 0 ? "No triggers yet — add one to get started." : count + " trigger(s)";
	}

	private final class TriggerList extends ContainerObjectSelectionList<TriggerRow> {
		TriggerList(Minecraft client, int height, int itemHeight) {
			super(client, DeltasoundScreen.this.width, height, 0, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return Math.min(520, DeltasoundScreen.this.width - 40);
		}

		void resetEntries() {
			clearEntries();
		}

		void addRow(TriggerRow row) {
			addEntry(row);
		}
	}

	private final class TriggerRow extends ContainerObjectSelectionList.Entry<TriggerRow> {
		private final TriggerEntry entry;
		private final Button editButton;
		private final Button testButton;
		private final Button deleteButton;

		TriggerRow(TriggerEntry entry) {
			this.entry = entry;
			this.editButton = Button.builder(Component.literal("Edit"), b -> edit())
					.size(BUTTON_WIDTH, 20)
					.build();
			this.testButton = Button.builder(Component.literal("Test"), b -> test())
					.size(BUTTON_WIDTH, 20)
					.build();
			this.deleteButton = Button.builder(Component.literal("Delete"), b -> delete())
					.size(BUTTON_WIDTH, 20)
					.build();
		}

		private void edit() {
			minecraft.setScreen(TriggerEditorScreen.edit(DeltasoundScreen.this, mod, entry));
		}

		private void test() {
			String text = entry.match == null ? "" : entry.match;
			if (text.isBlank()) {
				status.setMessage(Component.literal("This trigger has no activator text."));
				return;
			}
			ChatEventBridge.runClientTest(mod, text);
			status.setMessage(Component.literal("Sent test: Deltasound Test>> " + text));
		}

		private void delete() {
			mod.configLoader().entries().remove(entry);
			try {
				mod.configLoader().saveAndApply(mod.triggerEngine());
			} catch (Exception ex) {
				status.setMessage(Component.literal("Delete failed: " + ex.getMessage()));
				return;
			}
			refreshList();
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			int left = getContentX();
			int top = getContentY();
			int right = getContentRight();
			int buttonY = top + (getContentHeight() - 20) / 2;

			deleteButton.setPosition(right - BUTTON_WIDTH, buttonY);
			testButton.setPosition(deleteButton.getX() - BUTTON_GAP - BUTTON_WIDTH, buttonY);
			editButton.setPosition(testButton.getX() - BUTTON_GAP - BUTTON_WIDTH, buttonY);

			editButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
			testButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
			deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

			int textMaxWidth = Math.max(40, editButton.getX() - left - 12);
			int textColor = hovered ? ARGB.opaque(0xFFFFFF) : ARGB.opaque(0xE0E0E0);
			String title = font.plainSubstrByWidth(entry.displayName(), textMaxWidth);
			String detail = font.plainSubstrByWidth(
					"\"" + nullToEmpty(entry.match) + "\"  ·  "
							+ Math.round(entry.volumeOrDefault() * 100) + "%  ·  "
							+ nullToEmpty(entry.sound),
					textMaxWidth
			);
			graphics.text(font, title, left + 4, top + 8, textColor, false);
			graphics.text(font, detail, left + 4, top + 22, ARGB.opaque(0xA0A0A0), false);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(editButton, testButton, deleteButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(editButton, testButton, deleteButton);
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
