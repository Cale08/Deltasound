package dev.deltasound.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * In-game file browser for selecting {@code .ogg} / {@code .mp3} files (no native OS dialog).
 */
public final class FileBrowserScreen extends Screen {
	private final Screen parent;
	private final Consumer<Path> onSelect;

	private HeaderAndFooterLayout layout;
	private StringWidget pathLabel;
	private FileList list;
	private Path currentDir;
	private Path selectedFile;

	public FileBrowserScreen(Screen parent, Path startDir, Consumer<Path> onSelect) {
		super(Component.literal("Select sound file"));
		this.parent = parent;
		this.onSelect = onSelect;
		this.currentDir = startDir != null && Files.isDirectory(startDir)
				? startDir.toAbsolutePath().normalize()
				: Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
	}

	@Override
	protected void init() {
		layout = new HeaderAndFooterLayout(this, 48, 56);
		layout.addTitleHeader(title, font);

		pathLabel = new StringWidget(Component.literal(currentDir.toString()), font);
		pathLabel.setMaxWidth(width - 40, StringWidget.TextOverflow.CLAMPED);
		layout.addToHeader(pathLabel);

		list = new FileList(minecraft, layout.getContentHeight(), 24);
		layout.addToContents(list);
		rebuildList();

		LinearLayout footer = LinearLayout.horizontal().spacing(8);
		footer.addChild(Button.builder(Component.literal("Up"), b -> goUp()).width(60).build());
		footer.addChild(Button.builder(Component.literal("Select"), b -> confirm()).width(90).build());
		footer.addChild(Button.builder(Component.literal("Cancel"), b -> onClose()).width(90).build());
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
			if (pathLabel != null) {
				pathLabel.setMaxWidth(Math.max(40, width - 40), StringWidget.TextOverflow.CLAMPED);
			}
		}
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private void goUp() {
		Path parentPath = currentDir.getParent();
		if (parentPath != null) {
			navigate(parentPath);
		} else {
			navigateRoots();
		}
	}

	private void navigate(Path dir) {
		currentDir = dir.toAbsolutePath().normalize();
		selectedFile = null;
		pathLabel.setMessage(Component.literal(currentDir.toString()));
		rebuildList();
	}

	private void navigateRoots() {
		currentDir = null;
		selectedFile = null;
		pathLabel.setMessage(Component.literal("Drives"));
		list.resetEntries();
		for (File root : File.listRoots()) {
			list.addRow(new FileRow(root.toPath(), true, root.getAbsolutePath()));
		}
	}

	private void rebuildList() {
		if (list == null) {
			return;
		}
		list.resetEntries();
		if (currentDir == null) {
			navigateRoots();
			return;
		}

		List<Path> dirs = new ArrayList<>();
		List<Path> files = new ArrayList<>();
		try (Stream<Path> stream = Files.list(currentDir)) {
			stream.forEach(path -> {
				try {
					if (Files.isDirectory(path)) {
						dirs.add(path);
					} else if (isSoundFile(path)) {
						files.add(path);
					}
				} catch (Exception ignored) {
				}
			});
		} catch (IOException ex) {
			pathLabel.setMessage(Component.literal("Cannot read: " + currentDir));
			return;
		}

		dirs.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));
		files.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)));

		for (Path dir : dirs) {
			list.addRow(new FileRow(dir, true, dir.getFileName().toString() + "/"));
		}
		for (Path file : files) {
			list.addRow(new FileRow(file, false, file.getFileName().toString()));
		}
	}

	private void confirm() {
		if (selectedFile == null || !Files.isRegularFile(selectedFile)) {
			return;
		}
		onSelect.accept(selectedFile);
		minecraft.setScreen(parent);
	}

	private static boolean isSoundFile(Path path) {
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".ogg") || name.endsWith(".mp3");
	}

	private final class FileList extends ObjectSelectionList<FileRow> {
		FileList(Minecraft client, int height, int itemHeight) {
			super(client, FileBrowserScreen.this.width, height, 0, itemHeight);
		}

		@Override
		public int getRowWidth() {
			return Math.min(460, FileBrowserScreen.this.width - 40);
		}

		void resetEntries() {
			clearEntries();
		}

		void addRow(FileRow row) {
			addEntry(row);
		}
	}

	private final class FileRow extends ObjectSelectionList.Entry<FileRow> {
		private final Path path;
		private final boolean directory;
		private final String label;

		FileRow(Path path, boolean directory, String label) {
			this.path = path;
			this.directory = directory;
			this.label = label;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			boolean selected = equals(list.getSelected());
			int color = selected || hovered ? 0xFFFFFF : 0xC0C0C0;
			String prefix = directory ? "[DIR] " : "      ";
			String text = font.plainSubstrByWidth(prefix + label, getContentWidth() - 8);
			graphics.text(font, text, getContentX() + 4, getContentY() + 6, color, false);
		}

		@Override
		public Component getNarration() {
			return Component.literal(label);
		}

		@Override
		public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
			list.setSelected(this);
			if (directory) {
				if (doubleClick || event.button() == 0) {
					navigate(path);
				}
			} else {
				selectedFile = path;
				if (doubleClick) {
					confirm();
				}
			}
			return true;
		}
	}
}
