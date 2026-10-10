package com.xiaoyiluck.meowtools.command;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryEditorTest {
    Player viewer, target;
    PlayerInventory inventory, viewerInventory;
    InventoryView view;
    @BeforeEach void setup() {
        viewer = mock(Player.class); target = mock(Player.class);
        inventory = mock(PlayerInventory.class); viewerInventory = mock(PlayerInventory.class);
        view = mock(InventoryView.class);
        when(target.getInventory()).thenReturn(inventory);
        when(viewer.getInventory()).thenReturn(viewerInventory);
    }
    private ItemStack item(int amount) { return item(amount, Material.DIAMOND); }
    private ItemStack item(int amount, Material material) {
        ItemStack item = mock(ItemStack.class);
        AtomicInteger count = new AtomicInteger(amount);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenAnswer(call -> count.get());
        doAnswer(call -> { count.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.getMaxStackSize()).thenReturn(64);
        when(item.clone()).thenAnswer(call -> item(count.get(), material));
        when(item.isSimilar(any())).thenReturn(true);
        return item;
    }
    @Test void insertionImmediatelyUpdatesRealSlotAndCursor() {
        doReturn(item(12)).when(view).getCursor();
        InventoryEditor.edit(viewer, target, view, 9, ClickType.LEFT, -1);
        verify(inventory).setItem(eq(9), argThat(i -> i.getAmount() == 12));
        verify(view).setCursor(null);
    }
    @Test void pickupReadsLiveTargetInsteadOfGuiSnapshot() {
        doReturn(item(7)).when(inventory).getItem(9);
        InventoryEditor.edit(viewer, target, view, 9, ClickType.LEFT, -1);
        verify(view).setCursor(argThat(i -> i.getAmount() == 7)); verify(inventory).setItem(9, null);
        verify(view, never()).getTopInventory();
    }
    @Test void rightClickSplitsOddStackAndConservesItems() {
        doReturn(item(7)).when(inventory).getItem(9);
        InventoryEditor.edit(viewer, target, view, 9, ClickType.RIGHT, -1);
        verify(view).setCursor(argThat(i -> i.getAmount() == 4));
        verify(inventory).setItem(eq(9), argThat(i -> i.getAmount() == 3));
    }
    @Test void mergeHonorsStackLimitAndLeavesRemainderOnCursor() {
        doReturn(item(60)).when(inventory).getItem(9); doReturn(item(10)).when(view).getCursor();
        InventoryEditor.edit(viewer, target, view, 9, ClickType.LEFT, -1);
        verify(inventory).setItem(eq(9), argThat(i -> i.getAmount() == 64));
        verify(view).setCursor(argThat(i -> i.getAmount() == 6));
    }
    @Test void decorationAndCreativeCloneNeverWriteTarget() {
        InventoryEditor.edit(viewer, target, view, 8, ClickType.LEFT, -1);
        InventoryEditor.edit(viewer, target, view, 9, ClickType.MIDDLE, -1);
        verify(inventory, never()).setItem(anyInt(), any());
    }
    @Test void numberKeySwapsAuthoritativeHotbarItems() {
        doReturn(item(7)).when(inventory).getItem(9); doReturn(item(11)).when(viewerInventory).getItem(2);
        InventoryEditor.edit(viewer, target, view, 9, ClickType.NUMBER_KEY, 2);
        verify(inventory).setItem(eq(9), argThat(i -> i.getAmount() == 11));
        verify(viewerInventory).setItem(eq(2), argThat(i -> i.getAmount() == 7));
    }
    @Test void everyEquipmentAndHotbarSlotMapsCorrectly() {
        assertEquals(39, InventoryEditor.targetSlot(0)); assertEquals(36, InventoryEditor.targetSlot(3));
        assertEquals(40, InventoryEditor.targetSlot(4)); assertEquals(0, InventoryEditor.targetSlot(45));
        assertEquals(8, InventoryEditor.targetSlot(53)); assertEquals(-1, InventoryEditor.targetSlot(36));
    }
    @Test void emptyArmorInsertionTransfersOneItemAndKeepsCursorRemainder() {
        var helmet = mock(Material.class); when(helmet.getEquipmentSlot()).thenReturn(EquipmentSlot.HEAD);
        doReturn(item(3, helmet)).when(view).getCursor();
        InventoryEditor.edit(viewer, target, view, 0, ClickType.LEFT, -1);
        verify(inventory).setItem(eq(39), argThat(i -> i.getAmount() == 1));
        verify(view).setCursor(argThat(i -> i.getAmount() == 2));
    }
    @Test void secondViewerCannotTakeAnAlreadyRemovedItem() {
        var live = new java.util.concurrent.atomic.AtomicReference<ItemStack>(item(7));
        when(inventory.getItem(9)).thenAnswer(call -> live.get());
        doAnswer(call -> { live.set(call.getArgument(1)); return null; }).when(inventory).setItem(eq(9), any());
        InventoryEditor.edit(viewer, target, view, 9, ClickType.LEFT, -1);
        var second = mock(InventoryView.class);
        InventoryEditor.edit(mock(Player.class), target, second, 9, ClickType.LEFT, -1);
        verify(second, never()).setCursor(any());
    }
}
