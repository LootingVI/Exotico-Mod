package de.flori.exotico.client.social;

import net.minecraft.client.MinecraftClient;

//? if !mojmap {
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
//?} else {
/*import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
*///?}
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class SocialScreen extends Screen {
    private final Screen parent;

    private static final int PANEL_W = 400;
    private static final int PANEL_H = 260;
    private static final int HEADER_H = 32;
    private static final int TAB_H = 26;

    private static final int COL_BG = 0xFF0E0E18;
    private static final int COL_PANEL = 0xFF12121F;
    private static final int COL_SURFACE = 0xFF1A1A2E;
    private static final int COL_HOVER = 0xFF242438;
    private static final int COL_BORDER = 0xFF353550;
    private static final int COL_BORDER_ACT = 0xFF6B5BFF;
    private static final int COL_GOLD = 0xFFFFAA00;
    private static final int COL_TEXT = 0xFFE8E8FF;
    private static final int COL_TEXT_DIM = 0xFF808099;
    private static final int COL_ACCENT = 0xFF7C5EFF;

    private int px, py;
    private int currentTab = 0; // 0=Global, 1=DMs, 2=Setup
    private String chatInput = "";
    private String friendInput = "";
    private String dmInput = "";
    private boolean chatActive = false;
    private boolean friendActive = false;
    private boolean dmActive = false;
    private int tickCounter = 0;
    private SocialManager.FriendInfo selectedFriend = null;

    public SocialScreen(Screen parent) {
        super(Text.literal("Exotico Social"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (width - PANEL_W) / 2;
        py = (height - PANEL_H) / 2;
    }

    //? if !mojmap {
    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        //?} else {
    /*@Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
    *///?}
        if (click.button() == 0) {
            double mouseX = click.x();
            double mouseY = click.y();
            // Check close button
            int clX = px + PANEL_W - 26, clY = py + 7;
            if (hover(mouseX, mouseY, clX, clY, 18, 18)) {
                //? if !mojmap {
                if (client != null)
                    client.setScreen(parent);
                //?} else {
                /*if (minecraft != null) {
                    //? if !guiScreenHolder {
                    minecraft.setScreen(parent);
                    //?} else {
                    minecraft.gui.setScreen(parent);
                    //?}
                }*/
                //?}
                return true;
            }

            // Check tabs
            String[] tabs = { "Global Chat", "DMs", "Friends Setup" };
            int tabW = PANEL_W / tabs.length;
            int tabY = py + HEADER_H;
            for (int i = 0; i < tabs.length; i++) {
                if (hover(mouseX, mouseY, px + i * tabW, tabY, tabW, TAB_H)) {
                    currentTab = i;
                    chatActive = false;
                    friendActive = false;
                    dmActive = false;
                    if (i == 1)
                        selectedFriend = null;
                    return true;
                }
            }

            int sy = py + HEADER_H + TAB_H;

            // Chat Tab logic
            if (currentTab == 0) {
                int ty = py + PANEL_H - 45;
                if (hover(mouseX, mouseY, px + 20, ty, PANEL_W - 120, 24)) {
                    chatActive = true;
                    return true;
                } else {
                    chatActive = false;
                }

                // Send Button
                if (hover(mouseX, mouseY, px + PANEL_W - 90, ty, 70, 24)) {
                    if (!chatInput.trim().isEmpty()) {
                        SocialManager.sendGlobalMessage(chatInput.trim());
                        chatInput = "";
                    }
                    return true;
                }

                // Admin Action Buttons (Delete & Ban)
                boolean isAdmin = SocialManager.selfRank.equals("ADMIN") || SocialManager.selfRank.equals("DEV")
                        || SocialManager.selfRank.equals("MOD");
                if (isAdmin && !SocialManager.globalMessages.isEmpty()) {
                    int listH = PANEL_H - HEADER_H - TAB_H - 55;
                    int yOff = sy + listH - 20;

                    for (int i = SocialManager.globalMessages.size() - 1; i >= 0; i--) {
                        SocialManager.ChatMessage msg = SocialManager.globalMessages.get(i);
                        if (yOff < sy)
                            break;

                        // Delete button [X]
                        if (!msg.isAdminDeleted && hover(mouseX, mouseY, px + PANEL_W - 35, yOff, 12, 12)) {
                            SocialManager.deleteMessage(msg.id);
                            return true;
                        }
                        // Ban button [B]
                        if (!msg.isAdminDeleted && hover(mouseX, mouseY, px + PANEL_W - 50, yOff, 12, 12)) {
                            SocialManager.banUser(msg.sender);
                            return true;
                        }
                        yOff -= 15;
                    }
                }

            } else if (currentTab == 1) {
                // DM Tab click logic
                if (selectedFriend == null) {
                    // Click in friend list
                    int yOff = sy + 30;
                    for (SocialManager.FriendInfo fi : SocialManager.friendsList) {
                        if (hover(mouseX, mouseY, px + 20, yOff, PANEL_W - 40, 20)) {
                            selectedFriend = fi;
                            SocialManager.fetchPrivateHistory(fi.uuid);
                            return true;
                        }
                        yOff += 22;
                    }
                } else {
                    // In DM view
                    int ty = py + PANEL_H - 45;
                    if (hover(mouseX, mouseY, px + 20, ty, PANEL_W - 120, 24)) {
                        dmActive = true;
                        return true;
                    } else {
                        dmActive = false;
                    }
                    if (hover(mouseX, mouseY, px + PANEL_W - 90, ty, 70, 24)) {
                        if (!dmInput.trim().isEmpty()) {
                            SocialManager.sendPrivateMessage(selectedFriend.uuid, dmInput.trim());
                            dmInput = "";
                        }
                        return true;
                    }
                }
            } else if (currentTab == 2) {
                // Setup logic
                int ty = py + HEADER_H + TAB_H + 80;
                if (hover(mouseX, mouseY, px + 20, ty, PANEL_W - 150, 24)) {
                    friendActive = true;
                    return true;
                }
                if (hover(mouseX, mouseY, px + PANEL_W - 120, ty, 100, 24)) {
                    if (!friendInput.trim().isEmpty()) {
                        SocialManager.sendFriendRequest(friendInput.trim());
                        friendInput = "";
                    }
                    return true;
                }

                // Accept Buttons for Pending Requests
                if (!SocialManager.pendingRequests.isEmpty()) {
                    int syP = py + HEADER_H + TAB_H;
                    int yOff = syP + 130;
                    for (int i = 0; i < SocialManager.pendingRequests.size(); i++) {
                        SocialManager.FriendRequest req = SocialManager.pendingRequests.get(i);
                        if (yOff > py + PANEL_H - 30)
                            break;

                        if (hover(mouseX, mouseY, px + PANEL_W - 80, yOff - 2, 60, 14)) {
                            SocialManager.acceptFriendRequest(req.uuid);
                            return true;
                        }
                        yOff += 18;
                    }
                }
                friendActive = false;
            }
        }
        return super.mouseClicked(click, bl);
    }

    //? if !mojmap {
    @Override
    public boolean charTyped(CharInput input) {
        //?} else {
    /*@Override
    public boolean charTyped(CharacterEvent input) {
    *///?}
        char chr = (char) input.codepoint();
        if (chr >= 32 && chr != 127) {
            if (currentTab == 0 && chatActive) {
                if (chatInput.length() < 256) {
                    chatInput += chr;
                    return true;
                }
            } else if (currentTab == 2 && friendActive) {
                if (friendInput.length() < 16) {
                    friendInput += chr;
                    return true;
                }
            } else if (currentTab == 1 && dmActive) {
                if (dmInput.length() < 256) {
                    dmInput += chr;
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }

    //? if !mojmap {
    @Override
    public boolean keyPressed(KeyInput input) {
        //?} else {
    /*@Override
    public boolean keyPressed(KeyEvent input) {
    *///?}
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (chatActive || friendActive) {
                chatActive = false;
                friendActive = false;
                return true;
            }
        }

        if (currentTab == 0 && chatActive) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !chatInput.isEmpty()) {
                chatInput = chatInput.substring(0, chatInput.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (!chatInput.trim().isEmpty()) {
                    SocialManager.sendGlobalMessage(chatInput.trim());
                    chatInput = "";
                }
                return true;
            }
        } else if (currentTab == 1 && friendActive) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !friendInput.isEmpty()) {
                friendInput = friendInput.substring(0, friendInput.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                //? if !mojmap {
                if (!friendInput.trim().isEmpty() && client != null && client.player != null) {
                    client.player.sendMessage(Text.literal("§aFriend request sent!"), false);
                    friendInput = "";
                }
                //?} else {
                /*if (!friendInput.trim().isEmpty() && minecraft != null && minecraft.player != null) {
                    minecraft.player.sendSystemMessage(Text.literal("§aFriend request sent!"));
                    friendInput = "";
                }
                *///?}
                return true;
            }
        }

        return super.keyPressed(input);
    }

    //? if !mojmap {
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //?} else {
    /*@Override
    public void extractRenderState(DrawContext ctx, int mouseX, int mouseY, float delta) {
    *///?}
        tickCounter++;

        ctx.fill(0, 0, width, height, 0xBB080814); // Background blur equivalent

        fillRect(ctx, px, py, PANEL_W, PANEL_H, COL_PANEL);
        drawBorder(ctx, px, py, PANEL_W, PANEL_H, COL_BORDER);

        fillRect(ctx, px, py, PANEL_W, HEADER_H, COL_BG);
        fillRect(ctx, px, py, 3, HEADER_H, COL_ACCENT);
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "⚕ EXOTICO SOCIAL", px + 12, py + 12, COL_ACCENT);

        // Close Button
        int clX = px + PANEL_W - 26, clY = py + 7;
        boolean clHov = hover(mouseX, mouseY, clX, clY, 18, 18);
        fillRect(ctx, clX, clY, 18, 18, clHov ? 0x88FF3344 : 0x44FF3344);
        drawBorder(ctx, clX, clY, 18, 18, clHov ? 0xFFFF5555 : 0xFF993333);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, "×", clX + 9, clY + 5, COL_TEXT);

        // Tabs
        String[] tabs = { "Global Chat", "DMs", "Friends Setup" };
        int tabW = PANEL_W / tabs.length;
        int tabY = py + HEADER_H;

        for (int i = 0; i < tabs.length; i++) {
            int tx = px + i * tabW;
            boolean act = currentTab == i;
            boolean hov = !act && hover(mouseX, mouseY, tx, tabY, tabW, TAB_H);
            fillRect(ctx, tx, tabY, tabW, TAB_H, act ? COL_SURFACE : (hov ? COL_HOVER : COL_BG));
            drawBorder(ctx, tx, tabY, tabW, TAB_H, act ? COL_BORDER_ACT : COL_BORDER);
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, tabs[i], tx + tabW / 2, tabY + 8,
                    act ? COL_TEXT : COL_TEXT_DIM);
        }

        int scX = px, scY = tabY + TAB_H;
        int scW = PANEL_W, scH = PANEL_H - HEADER_H - TAB_H;

        if (currentTab == 0) {
            renderChatTab(ctx, mouseX, mouseY, scX, scY, scW, scH);
        } else if (currentTab == 1) {
            renderDMTab(ctx, mouseX, mouseY, scX, scY, scW, scH);
        } else if (currentTab == 2) {
            renderFriendsTab(ctx, mouseX, mouseY, scX, scY, scW, scH);
        }

        //? if !mojmap {
        super.render(ctx, mouseX, mouseY, delta);
        //?} else {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);*/
        //?}
    }

    private void renderChatTab(DrawContext ctx, int mx, int my, int sx, int sy, int sw, int sh) {
        // Draw Chat Background Box
        int listH = sh - 55;
        fillRect(ctx, sx + 10, sy + 5, sw - 20, listH, 0xFF08080C);
        drawBorder(ctx, sx + 10, sy + 5, sw - 20, listH, COL_BORDER);

        boolean isAdmin = SocialManager.selfRank.equals("ADMIN") || SocialManager.selfRank.equals("DEV")
                || SocialManager.selfRank.equals("MOD");

        // Render Chat Messages safely from the bottom up!
        int yOff = sy + listH - 20;
        List<SocialManager.ChatMessage> msgs = SocialManager.globalMessages;
        for (int i = msgs.size() - 1; i >= 0; i--) {
            SocialManager.ChatMessage msg = msgs.get(i);
            if (yOff < sy + 5)
                break; // Out of bounds

            String rankColor = msg.rank.equalsIgnoreCase("USER") ? "§7" : "§c[" + msg.rank + "] §c";
            String displ = rankColor + msg.sender + "§8: "
                    + (msg.isAdminDeleted ? "§c[Deleted by Admin]" : "§f" + msg.content);

            // Abbreviate if too long (Quick hack for long messages)
            if (textRenderer.getWidth(displ) > sw - (isAdmin ? 60 : 30)) {
                displ = displ.substring(0, Math.min(displ.length(), 40)) + "...";
            }
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, displ, sx + 15, yOff, COL_TEXT);

            if (isAdmin && !msg.isAdminDeleted) {
                // Delete button [X]
                boolean hovDel = hover(mx, my, sx + sw - 35, yOff, 12, 12);
                fillRect(ctx, sx + sw - 35, yOff, 12, 12, hovDel ? 0x88FF3344 : 0x44FF3344);
                //? if !mojmap {
                ctx.drawTextWithShadow(
                        //?} else {
                        /*ctx.text(*/
                        //?}
                        textRenderer, "X", sx + sw - 32, yOff + 2, 0xFFFF5555);

                // Ban button [B]
                boolean hovBan = hover(mx, my, sx + sw - 50, yOff, 12, 12);
                fillRect(ctx, sx + sw - 50, yOff, 12, 12, hovBan ? 0x88FFAA00 : 0x44FFAA00);
                //? if !mojmap {
                ctx.drawTextWithShadow(
                        //?} else {
                        /*ctx.text(*/
                        //?}
                        textRenderer, "B", sx + sw - 47, yOff + 2, 0xFFFFAA00);
            }

            yOff -= 15;
        }

        int ty = sy + sh - 45;
        drawInput(ctx, sx + 20, ty, sw - 120, 24, chatInput, chatActive, "Enter global message...");
        drawButton(ctx, sx + sw - 90, ty, 70, 24, "Send", hover(mx, my, sx + sw - 90, ty, 70, 24));
    }

    private void renderDMTab(DrawContext ctx, int mx, int my, int sx, int sy, int sw, int sh) {
        if (selectedFriend == null) {
            //? if !mojmap {
            ctx.drawCenteredTextWithShadow(
                    //?} else {
                    /*ctx.centeredText(*/
                    //?}
                    textRenderer, "§6Direct Messages", sx + sw / 2, sy + 10, 0xFFFFFFFF);
            int yOff = sy + 30;
            if (SocialManager.friendsList.isEmpty()) {
                //? if !mojmap {
                ctx.drawCenteredTextWithShadow(
                        //?} else {
                        /*ctx.centeredText(*/
                        //?}
                        textRenderer, "§8No friends yet. Go to 'Setup' to add some!",
                        sx + sw / 2, sy + 60, 0xFFFFFFFF);
            } else {
                for (SocialManager.FriendInfo fi : SocialManager.friendsList) {
                    boolean hov = hover(mx, my, sx + 20, yOff, sw - 40, 20);
                    fillRect(ctx, sx + 20, yOff, sw - 40, 20, hov ? COL_HOVER : COL_SURFACE);
                    drawBorder(ctx, sx + 20, yOff, sw - 40, 20, COL_BORDER);

                    String pref = fi.online ? "§a● " : "§7○ ";
                    //? if !mojmap {
                    ctx.drawTextWithShadow(
                            //?} else {
                            /*ctx.text(*/
                            //?}
                            textRenderer, pref + "§f" + fi.name + " §7(" + fi.rank + ")", sx + 28,
                            yOff + 6, 0xFFFFFFFF);
                    yOff += 22;
                }
            }
        } else {
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, "§aDM with §l" + selectedFriend.name, sx + 15, sy + 10, 0xFFFFFFFF);

            int listH = sh - 75;
            fillRect(ctx, sx + 10, sy + 25, sw - 20, listH, 0xFF08080C);
            drawBorder(ctx, sx + 10, sy + 25, sw - 20, listH, COL_BORDER);

            // Render PMs
            int yChat = sy + 25 + listH - 15;
            List<SocialManager.ChatMessage> pms = SocialManager.privateMessages.getOrDefault(selectedFriend.uuid,
                    new ArrayList<>());
            for (int i = pms.size() - 1; i >= 0; i--) {
                if (yChat < sy + 30)
                    break;
                SocialManager.ChatMessage m = pms.get(i);
                //? if !mojmap {
                ctx.drawTextWithShadow(
                        //?} else {
                        /*ctx.text(*/
                        //?}
                        textRenderer,
                        "§b" + (m.sender.length() > 30 ? m.sender.substring(0, 10) : m.sender) + ": §f" + m.content,
                        sx + 15, yChat, 0xFFFFFFFF);
                yChat -= 12;
            }

            int ty = sy + sh - 45;
            drawInput(ctx, sx + 20, ty, sw - 120, 24, dmInput, dmActive, "Type private message...");
            drawButton(ctx, sx + sw - 90, ty, 70, 24, "Send", hover(mx, my, sx + sw - 90, ty, 70, 24));
        }
    }

    private void renderFriendsTab(DrawContext ctx, int mx, int my, int sx, int sy, int sw, int sh) {
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "Manage EXOTICO Friends", sx + 20, sy + 20, COL_GOLD);
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "Being friends allows you to send encrypted DMs", sx + 20, sy + 35,
                COL_TEXT_DIM);
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "and see when they join a server.", sx + 20, sy + 50, COL_TEXT_DIM);

        int ty = sy + 80;
        drawInput(ctx, sx + 20, ty, sw - 150, 24, friendInput, friendActive, "Friend Username");
        drawButton(ctx, sx + sw - 120, ty, 100, 24, "Add Friend", hover(mx, my, sx + sw - 120, ty, 100, 24));

        // Pending Requests
        //? if !mojmap {
        ctx.drawTextWithShadow(
                //?} else {
                /*ctx.text(*/
                //?}
                textRenderer, "Pending Requests:", sx + 20, sy + 115, COL_ACCENT);
        int yOff = sy + 130;
        if (SocialManager.pendingRequests.isEmpty()) {
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, "§8No pending requests.", sx + 30, yOff, COL_TEXT_DIM);
        } else {
            for (int i = 0; i < SocialManager.pendingRequests.size(); i++) {
                SocialManager.FriendRequest req = SocialManager.pendingRequests.get(i);
                if (yOff > sy + sh - 30)
                    break;

                //? if !mojmap {
                ctx.drawTextWithShadow(
                        //?} else {
                        /*ctx.text(*/
                        //?}
                        textRenderer, "§7- §f" + req.name, sx + 30, yOff, COL_TEXT);

                // Accept Button
                boolean hov = hover(mx, my, sx + sw - 80, yOff - 2, 60, 14);
                fillRect(ctx, sx + sw - 80, yOff - 2, 60, 14, hov ? 0xFF30CC30 : 0xFF208020);
                //? if !mojmap {
                ctx.drawCenteredTextWithShadow(
                        //?} else {
                        /*ctx.centeredText(*/
                        //?}
                        textRenderer, "Accept", sx + sw - 50, yOff + 1, COL_TEXT);

                yOff += 18;
            }
        }
    }

    private void drawInput(DrawContext ctx, int x, int y, int w, int h, String val, boolean active, String ph) {
        fillRect(ctx, x, y, w, h, active ? 0xFF181830 : 0xFF0C0C1A);
        drawBorder(ctx, x, y, w, h, active ? COL_BORDER_ACT : COL_BORDER);
        if (val.isEmpty()) {
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, ph, x + 8, y + 8, COL_TEXT_DIM);
        } else {
            String disp = val;
            if (textRenderer.getWidth(disp) > w - 24) {
                disp = "..." + disp.substring(Math.max(0, disp.length() - (w / 8)));
            }
            if (active && (tickCounter % 20 < 10)) {
                disp += "_";
            }
            //? if !mojmap {
            ctx.drawTextWithShadow(
                    //?} else {
                    /*ctx.text(*/
                    //?}
                    textRenderer, disp, x + 8, y + 8, COL_TEXT);
        }
    }

    private void drawButton(DrawContext ctx, int x, int y, int w, int h, String text, boolean hover) {
        fillRect(ctx, x, y, w, h, hover ? COL_HOVER : COL_SURFACE);
        drawBorder(ctx, x, y, w, h, hover ? COL_BORDER_ACT : COL_BORDER);
        //? if !mojmap {
        ctx.drawCenteredTextWithShadow(
                //?} else {
                /*ctx.centeredText(*/
                //?}
                textRenderer, text, x + w / 2, y + 8, COL_TEXT);
    }

    private void fillRect(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + h, color);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private boolean hover(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
