package com.hvn.wireguarddemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), Tunnel {

    private lateinit var switch: SwitchCompat
    private var previousState: Tunnel.State? = null
    private val tunnelManager = Application.getTunnelManager()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switch = findViewById(R.id.switchConfig)

        switch.setOnCheckedChangeListener { compoundButton, isChecked ->
            setTunnelStateWithPermissionsResult(isChecked)
        }
    }

    private fun setTunnelStateWithPermissionsResult(checked: Boolean) {
        try {

            CoroutineScope(Dispatchers.Main).launch {
                setStateAsync(Tunnel.State.of(checked))
            }

        } catch (ex: Exception) {
            Log.e("exception", ex.toString())
            if (checked) {
                Log.e("exception", "error bringing up")
            } else {
                Log.e("exception", "error bringing down")
            }
        }
    }

    /**
     * Always use word suspend with function when using coroutine
     */
    private suspend fun setStateAsync(state: Tunnel.State) =
        withContext(Dispatchers.Main.immediate) {
            tunnelManager.setTunnelState(this@MainActivity, state)
        }

    private fun onStateChanged(state: Tunnel.State): Tunnel.State {
        previousState = state
        switch.isChecked = previousState == Tunnel.State.UP
        return state
    }

    override fun getName(): String {
        return "test"
    }

    override fun onStateChange(newState: Tunnel.State) {
        onStateChanged(newState)
    }
}