/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.hvn.wireguarddemo

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import kotlinx.coroutines.CoroutineScope

fun Context.resolveAttribute(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

val Any.applicationScope: CoroutineScope
    get() = Application.getCoroutineScope()

