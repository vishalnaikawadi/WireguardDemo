/*
 * Copyright © 2017-2019 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.hvn.wireguarddemo

import com.hvn.wireguarddemo.Application.Companion.getBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Maintains and mediates changes to the set of available WireGuard tunnels,
 */
class TunnelManager(private val configStore: ConfigStore) {


    suspend fun setTunnelState(tunnel: Tunnel, state: Tunnel.State): Tunnel.State? = withContext(
        Dispatchers.Main.immediate
    ) {
        var newState: Tunnel.State? = null
        var throwable: Throwable? = null
        try {
            newState = withContext(Dispatchers.IO) {
                getBackend().setState(
                    tunnel,
                    state,
                    getTunnelConfig(tunnel)
                )
            }
//            if (newState == Tunnel.State.UP)
//                lastUsedTunnel = tunnel
        } catch (e: Throwable) {
            throwable = e
        }
//        tunnel.onStateChanged(newState)
        //saveState()
        if (throwable != null)
            throw throwable
        newState
    }

    private suspend fun getTunnelConfig(tunnel: Tunnel):
            Config = withContext(Dispatchers.Main.immediate) {
        withContext(Dispatchers.IO) {
            configStore.load(tunnel.name)
        }
    }


}