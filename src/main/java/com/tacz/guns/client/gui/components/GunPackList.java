package com.tacz.guns.client.gui.components;

import net.minecraft.client.renderer.RenderPipelines;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.client.gui.GunSmithTableScreen;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.util.ARGB;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.*;

public class GunPackList extends ContainerObjectSelectionList<GunPackList.Entry> {
    private final GunSmithTableScreen parent;
    private final List<Checkbox> gunPackList = new ArrayList<>();
    private final Set<String> selectedNamespaces = new HashSet<>();
    private final Checkbox byHandCheckbox;
    private final EditBox byName;

    /*@Override
    protected void renderListBackground(GuiGraphics pGuiGraphics) {
        super.renderListBackground(pGuiGraphics);
    }*/

    public GunPackList(Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight,
                       Map<Identifier, List<Identifier>> recipes, GunSmithTableScreen parent) {
        super(pMinecraft, pWidth, pHeight, pY0, pItemHeight);
        this.parent = parent;
        Set<String> namespaces = new HashSet<>();
        for (List<Identifier> entry : recipes.values()) {
            entry.forEach((resourceLocation) -> namespaces.add(resourceLocation.getNamespace()));
        }

        this.byName = new EditBox(pMinecraft.font, 3, 0, 94, 10, Component.empty());
        this.byName.setHint(Component.translatable("gui.tacz.gun_smith_table.filter.search"));
        this.byName.setResponder((pText) -> {
            parent.init();
            parent.setIndexPage(0);
        });
        this.addEntry(new GunPackList.Entry(byName));

        this.byHandCheckbox = new Checkbox(0, 0, 10, 10, Component.translatable("gui.tacz.gun_smith_table.filter.handgun"), false) {
            @Override
            public void onPress(InputWithModifiers input) {
                super.onPress(input);
                parent.init();
                parent.setIndexPage(0);
            }
        };
        this.addEntry(new GunPackList.Entry(byHandCheckbox));

        Checkbox checkbox1 = new Checkbox(0, 0, 10, 10, Component.translatable("gui.tacz.gun_smith_table.filter.all"), true) {
            @Override
            public void onPress(InputWithModifiers input) {
                super.onPress(input);
                gunPackList.forEach((checkbox) -> checkbox.selected = this.selected);
                updateSelectedNamespaces();
            }
        };
        this.addEntry(new GunPackList.Entry(checkbox1));

        for (String namespace : namespaces) {
            PackInfo packInfo = ClientAssetsManager.INSTANCE.getPackInfo(namespace);
            Component name = packInfo == null ? Component.literal(namespace) : Component.translatable(packInfo.getName());

            Checkbox checkbox = new Checkbox(0, 0, 10, 10, name, namespace, true) {
                @Override
                public void onPress(InputWithModifiers input) {
                    super.onPress(input);
                    checkbox1.selected = gunPackList.stream().allMatch(Checkbox::selected);
                    updateSelectedNamespaces();
                }
            };
            gunPackList.add(checkbox);
            selectedNamespaces.add(namespace);
            this.addEntry(new GunPackList.Entry(checkbox));
        }
    }

    public String getSearchText() {
        return byName.getValue();
    }

    public boolean isByHandSelected() {
        return byHandCheckbox.selected;
    }

    public void setByHandSelected(boolean selected) {
        byHandCheckbox.selected = selected;
    }

    public Set<String> namespaceList() {
        return selectedNamespaces;
    }

    public void updateSelectedNamespaces() {
        selectedNamespaces.clear();
        gunPackList.forEach((checkbox) -> {
            if (checkbox.selected) {
                selectedNamespaces.add(checkbox.getId());
            }
        });
        parent.init();
        parent.setIndexPage(0);
    }

    /**
     * 原来这里整段抄了一遍原版的列表绘制循环，只为了加一层半透明底和自画滚动条。
     * 1.21.9 之后入口是 renderWidget，滚动条由 AbstractScrollArea 自己画，条目也自己知道位置，
     * 所以只剩下底色这一件事。
     */
    @Override
    protected void renderListBackground(GuiGraphics graphics) {
        super.renderListBackground(graphics);
        graphics.fill(this.getX(), this.getY(), this.getRight(), this.getBottom(), 0x80000000);
    }

    public int getRowLeft() {
        return this.getX() + 4;
    }

    public int getRowWidth() {
        return this.width;
    }

    public static class Entry extends ContainerObjectSelectionList.Entry<GunPackList.Entry> {
        private final AbstractWidget widget;

        public Entry(AbstractWidget widget) {
            this.widget = widget;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(widget);
        }

        @Override
        public void renderContent(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, boolean hovered, float pPartialTick) {
            this.widget.setX(this.getX());
            this.widget.setY(this.getY());
            this.widget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(widget);
        }
    }

    public static class Checkbox extends AbstractButton {
        private static final Identifier CHECKBOX = Identifier.withDefaultNamespace("textures/gui/sprites/widget/checkbox.png");
        private static final Identifier CHECKBOX_HIGHLIGHTED = Identifier.withDefaultNamespace("textures/gui/sprites/widget/checkbox_highlighted.png");
        private static final Identifier CHECKBOX_SELECTED = Identifier.withDefaultNamespace("textures/gui/sprites/widget/checkbox_selected.png");
        private static final Identifier CHECKBOX_SELECTED_HIGHLIGHTED = Identifier.withDefaultNamespace("textures/gui/sprites/widget/checkbox_selected_highlighted.png");
        protected boolean selected;
        protected final boolean showLabel;
        private String id;

        public Checkbox(int pX, int pY, int pWidth, int pHeight, Component pMessage, String id, boolean pSelected) {
            this(pX, pY, pWidth, pHeight, pMessage, pSelected, true);
            this.id = id;
        }

        public Checkbox(int pX, int pY, int pWidth, int pHeight, Component pMessage, boolean pSelected) {
            this(pX, pY, pWidth, pHeight, pMessage, pSelected, true);
        }

        public Checkbox(int pX, int pY, int pWidth, int pHeight, Component pMessage, boolean pSelected, boolean pShowLabel) {
            super(pX, pY, pWidth, pHeight, pMessage);
            this.selected = pSelected;
            this.showLabel = pShowLabel;
        }

        public String getId() {
            return id;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.selected = !this.selected;
        }

        public boolean selected() {
            return this.selected;
        }

        public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
            pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
            if (this.active) {
                if (this.isFocused()) {
                    pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused"));
                } else {
                    pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered"));
                }
            }

        }

        protected void renderContents(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            Minecraft minecraft = Minecraft.getInstance();
            Font font = minecraft.font;
            Identifier texture;
            if (this.isFocused()) {
                if (this.selected) {
                    texture = CHECKBOX_SELECTED_HIGHLIGHTED;
                } else {
                    texture = CHECKBOX_HIGHLIGHTED;
                }
            } else {
                if (this.selected) {
                    texture = CHECKBOX_SELECTED;
                } else {
                    texture = CHECKBOX;
                }
            }
            pGuiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, this.getX(), this.getY(), 0.0F, 0.0F, 10, 10, 10, 10, ARGB.white(this.alpha));
            if (this.showLabel) {
                pGuiGraphics.drawString(font, this.getMessage(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 14737632 | Mth.ceil(this.alpha * 255.0F) << 24);
            }

        }
    }
}