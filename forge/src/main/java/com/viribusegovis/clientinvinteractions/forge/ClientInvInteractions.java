package com.viribusegovis.clientinvinteractions.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ClientInvInteractions.MOD_ID)
public final class ClientInvInteractions {
    public static final String MOD_ID = "clientinvinteractions";

    public ClientInvInteractions() {
        // Client-only mod; on a dedicated server it loads but does nothing.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }
    }
}
