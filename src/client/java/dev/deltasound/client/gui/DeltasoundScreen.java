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

import java.util.List;

/**
 * Main Deltasound config screen — modern trigger list + Add trigger.
 */
public final class DeltasoundScreen extends Screen {
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
		layout = new HeaderAndFooterLayout(this, 36, 56);
		layout.addTitleHeader(title, font);

		list = new TriggerList(minecraft, layout.getContentHeight(), 36);
		layout.addToContents(list);
		refreshList();

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Add trigger"), b ->
				minecraft.setScreen(new AddTriggerScreen(this, mod))
		).width(120).build());
		footer.addChild(Button.builder(Component.literal("Done"), b -> onClose()).width(80).build());
		layout.addToFooter(footer);

		status = new StringWidget(Component.literal(statusText()), font);
		layout.addToHeader(status);

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
			return Math.min(480, DeltasoundScreen.this.width - 40);
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
		private final Button testButton;
		private final Button deleteButton;

		TriggerRow(TriggerEntry entry) {
			this.entry = entry;
			this.testButton = Button.builder(Component.literal("Test"), b -> test())
					.size(56, 20)
					.build();
			this.deleteButton = Button.builder(Component.literal("Delete"), b -> delete())
					.size(56, 20)
					.build();
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

			deleteButton.setPosition(right - deleteButton.getWidth(), top + 8);
			testButton.setPosition(deleteButton.getX() - testButton.getWidth() - 6, top + 8);
			testButton.extractRenderState(graphics, mouseX, mouseY, partialTick);
			deleteButton.extractRenderState(graphics, mouseX, mouseY, partialTick);

			int textColor = hovered ? 0xFFFFFF : 0xE0E0E0;
			graphics.text(font, entry.displayName(), left + 4, top + 4, textColor, false);
			String detail = "\"" + nullToEmpty(entry.match) + "\"  →  " + nullToEmpty(entry.sound);
			graphics.text(font, detail, left + 4, top + 16, 0xA0A0A0, false);
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return List.of(testButton, deleteButton);
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return List.of(testButton, deleteButton);
		}
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
