/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.hvn.wireguarddemo.wireguard.preference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import com.hvn.wireguarddemo.Application
import com.hvn.wireguarddemo.R
import com.hvn.wireguarddemo.wireguard.activity.SettingsActivity
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.backend.WgQuickBackend
import com.hvn.wireguarddemo.wireguard.util.UserKnobs
import com.hvn.wireguarddemo.wireguard.util.activity
import com.hvn.wireguarddemo.wireguard.util.lifecycleScope
import kotlinx.coroutines.*
import kotlin.system.exitProcess

class KernelModuleDisablerPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    private var state = State.UNKNOWN

    init {
        isVisible = false
        lifecycleScope.launch {
            setState(if (Application.getBackend() is WgQuickBackend) State.ENABLED else State.DISABLED)
        }
    }

    override fun getSummary() = if (state == State.UNKNOWN) "" else context.getString(state.summaryResourceId)

    override fun getTitle() = if (state == State.UNKNOWN) "" else context.getString(state.titleResourceId)

    override fun onClick() {
        activity.lifecycleScope.launch {
            if (state == State.DISABLED) {
                setState(State.ENABLING)
                UserKnobs.setDisableKernelModule(false)
            } else if (state == State.ENABLED) {
                setState(State.DISABLING)
                UserKnobs.setDisableKernelModule(true)
            }
            val observableTunnels = Application.getTunnelManager().getTunnels()
            val downings = observableTunnels.map { async(SupervisorJob()) { it.setStateAsync(Tunnel.State.DOWN) } }
            try {
                downings.awaitAll()
                withContext(Dispatchers.IO) {
                    val restartIntent = Intent(context, SettingsActivity::class.java)
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    Application.get().startActivity(restartIntent)
                    exitProcess(0)
                }
            } catch (e: Throwable) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
    }

    private fun setState(state: State) {
        if (this.state == state) return
        this.state = state
        if (isEnabled != state.shouldEnableView) isEnabled = state.shouldEnableView
        if (isVisible != state.visible) isVisible = state.visible
        notifyChanged()
    }

    private enum class State(val titleResourceId: Int, val summaryResourceId: Int, val shouldEnableView: Boolean, val visible: Boolean) {
        UNKNOWN(0, 0, false, false),
        ENABLED(R.string.module_disabler_enabled_title, R.string.module_disabler_enabled_summary, true, true),
        DISABLED(R.string.module_disabler_disabled_title, R.string.module_disabler_disabled_summary, true, true),
        ENABLING(R.string.module_disabler_disabled_title, R.string.success_application_will_restart, false, true),
        DISABLING(R.string.module_disabler_enabled_title, R.string.success_application_will_restart, false, true);
    }

    companion object {
        private const val TAG = "WireGuard/KernelModuleDisablerPreference"
    }
}
