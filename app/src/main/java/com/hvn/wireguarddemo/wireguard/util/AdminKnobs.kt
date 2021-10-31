/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.hvn.wireguarddemo.wireguard.util

import android.content.RestrictionsManager
import androidx.core.content.getSystemService
import com.hvn.wireguarddemo.Application

object AdminKnobs {
    private val restrictions: RestrictionsManager? = Application.get().getSystemService()
    val disableConfigExport: Boolean
        get() = restrictions?.applicationRestrictions?.getBoolean("disable_config_export", false)
                ?: false
}
