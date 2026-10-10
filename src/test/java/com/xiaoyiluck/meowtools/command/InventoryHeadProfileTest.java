package com.xiaoyiluck.meowtools.command;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class InventoryHeadProfileTest {
    @Test void incompleteProfilesUseDefaultHeadWithoutMojangLookup() {
        var player = mock(Player.class); var profile = mock(PlayerProfile.class); var meta = mock(SkullMeta.class);
        when(player.getPlayerProfile()).thenReturn(profile); when(profile.hasTextures()).thenReturn(false);
        InvCommand.applyAvailableProfile(player, meta);
        verifyNoInteractions(meta); verify(profile, never()).update(); verify(profile, never()).complete();
    }

    @Test void existingTexturesArePreservedWithoutFetchingAnotherProfile() {
        var player = mock(Player.class); var profile = mock(PlayerProfile.class); var meta = mock(SkullMeta.class);
        when(player.getPlayerProfile()).thenReturn(profile); when(profile.hasTextures()).thenReturn(true);
        InvCommand.applyAvailableProfile(player, meta);
        verify(meta).setPlayerProfile(profile); verify(profile, never()).update(); verify(profile, never()).complete();
    }
}
