package com.xiaoyiluck.meowtools.command;
import com.xiaoyiluck.meowtools.MeowTools;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
class InvViewListenerTest {
    @Test void closeNeverCommitsSnapshotsAndUnsafeCrossInventoryActionsAreCancelled() {
        var plugin = mock(MeowTools.class); when(plugin.getName()).thenReturn("MeowTools"); when(plugin.namespace()).thenReturn("meowtools");
        var top = mock(Inventory.class); when(top.getSize()).thenReturn(54);
        var holder = new InvCommand.TargetInventoryHolder(UUID.randomUUID()); when(top.getHolder()).thenReturn(holder);
        var view = mock(InventoryView.class); when(view.getTopInventory()).thenReturn(top);
        var player = mock(Player.class); when(view.getPlayer()).thenReturn(player);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            var command = new InvCommand(plugin); var listener = command.new InvViewListener();
            listener.onInventoryClose(new InventoryCloseEvent(view));
            verify(top, never()).getItem(anyInt());
            var click = mock(InventoryClickEvent.class); when(click.getView()).thenReturn(view);
            when(click.getRawSlot()).thenReturn(60); when(click.isShiftClick()).thenReturn(true);
            listener.onInventoryClick(click); verify(click).setCancelled(true);
            var drag = mock(InventoryDragEvent.class); when(drag.getView()).thenReturn(view);
            when(drag.getRawSlots()).thenReturn(Set.of(9, 60)); listener.onInventoryDrag(drag);
            verify(drag).setCancelled(true);
            var bottomDrag = mock(InventoryDragEvent.class); when(bottomDrag.getView()).thenReturn(view);
            when(bottomDrag.getRawSlots()).thenReturn(Set.of(60, 61)); listener.onInventoryDrag(bottomDrag);
            verify(bottomDrag, never()).setCancelled(true);
        }
    }
}
