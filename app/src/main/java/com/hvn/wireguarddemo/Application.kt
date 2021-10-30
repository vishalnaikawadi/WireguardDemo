package com.hvn.wireguarddemo

import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.android.util.ModuleLoader
import com.wireguard.android.util.RootShell
import com.wireguard.android.util.ToolsInstaller
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.lang.ref.WeakReference
import java.util.*

class Application : android.app.Application() {

    private val futureBackend = CompletableDeferred<Backend>()
    private lateinit var rootShell: RootShell
    private lateinit var moduleLoader: ModuleLoader
    private var backend: Backend? = null
    private lateinit var toolsInstaller: ToolsInstaller
    private lateinit var tunnelManager: TunnelManager
    private lateinit var preferencesDataStore: DataStore<Preferences>
    private val coroutineScope = CoroutineScope(Job() + Dispatchers.Main.immediate)

    init {
        weakSelf = WeakReference(this)
    }

    override fun onCreate() {
        super.onCreate()

        rootShell = RootShell(applicationContext)
        moduleLoader = ModuleLoader(applicationContext, rootShell, USER_AGENT)
        preferencesDataStore =
            PreferenceDataStoreFactory.create { applicationContext.preferencesDataStoreFile("settings") }

        tunnelManager = TunnelManager(FileConfigStore(applicationContext))

        coroutineScope.launch(Dispatchers.IO) {
            try {
                backend = determineBackend()
                futureBackend.complete(backend!!)
            } catch (e: Throwable) {
                Log.e("Application: ", Log.getStackTraceString(e))
            }
        }

    }

    companion object {

        val USER_AGENT = String.format(
            Locale.ENGLISH,
            "WireGuard/%s (Android %d; %s; %s; %s %s; %s)",
            BuildConfig.VERSION_NAME,
            Build.VERSION.SDK_INT,
            if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else "unknown ABI",
            Build.BOARD,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.FINGERPRINT
        )

        private lateinit var weakSelf: WeakReference<Application>

        @JvmStatic
        fun get(): Application {
            return weakSelf.get()!!
        }

        @JvmStatic
        suspend fun getBackend() = get().futureBackend.await()

        @JvmStatic
        fun getModuleLoader() = get().moduleLoader

        @JvmStatic
        fun getTunnelManager() = get().tunnelManager

        @JvmStatic
        fun getPreferencesDataStore() = get().preferencesDataStore

        @JvmStatic
        fun getCoroutineScope() =
            get().coroutineScope
    }


    private suspend fun determineBackend(): Backend {
        var backend: Backend? = null
        var didStartRootShell = false
        if (!ModuleLoader.isModuleLoaded() && moduleLoader.moduleMightExist()) {
            try {
                rootShell.start()
                didStartRootShell = true
                moduleLoader.loadModule()
            } catch (ignored: Exception) {
            }
        }
        if (!UserKnobs.disableKernelModule.first() && ModuleLoader.isModuleLoaded()) {
            try {
                if (!didStartRootShell)
                    rootShell.start()
                val wgQuickBackend = WgQuickBackend(applicationContext, rootShell, toolsInstaller)
                wgQuickBackend.setMultipleTunnels(UserKnobs.multipleTunnels.first())
                backend = wgQuickBackend
                UserKnobs.multipleTunnels.onEach {
                    wgQuickBackend.setMultipleTunnels(it)
                }.launchIn(coroutineScope)
            } catch (ignored: Exception) {
            }
        }
        if (backend == null) {
            backend = GoBackend(applicationContext)
            GoBackend.setAlwaysOnCallback {
                get().applicationScope.launch {
                    //todo commenting this out for now
                    /*get().tunnelManager.restoreState(
                        true
                    )*/
                }
            }
        }
        return backend
    }


}