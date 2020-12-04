package com.example.splitter


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.MacAddress
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.splitter.App.Companion.REQUEST_LOCATION
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_main_tabbed.*
import kotlinx.android.synthetic.main.fragment_receive.*
import kotlinx.android.synthetic.main.fragment_share.*
import java.math.BigInteger
import java.net.InetAddress


class MainActivity : CaptureActivity() {
    private var state = State(this)
    private val qrEncoder = QREncoder(300)

    val wifiManager: WifiManager by lazy {
        getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    val p2pManager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }


    val serviceConnection = object : ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            state.accept(service as AudioServiceBinder, name?.let { State.fromServiceName(it)!! })
            updateUi()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            state.setIdle()
            updateUi()
        }
    }

    var channel: WifiP2pManager.Channel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.main_toolbar))

        channel = p2pManager?.initialize(this, mainLooper, null)
        bindService(Intent(this, CaptureService::class.java), serviceConnection, 0)
        bindService(Intent(this, ReceiveService::class.java), serviceConnection, 0)
    }

    class State (val ctxt: Context) {
        enum class Connection {
            Connecting,
            Playing,
            Paused
        }
        enum class Direction {
            Send,
            Receive
        }

        companion object {
            fun fromServiceName (componentName: ComponentName) : Direction? {
                if (componentName.equals(ComponentName(ctxt, CaptureService::class.java))) {
                    return Direction.Send
                }
                else if (componentName.equals(ComponentName(ctxt, ReceiveService::class.java))) {
                    return Direction.Receive
                }
                else {
                    return null
                }
            }
        }

        private var state : Pair<Direction, Connection>? = null
        val isIdle get() = state == null
        val direction get () = state?.first
        val connection get () = state?.second
        var qrCode : Bitmap? = null

        fun setIdle () {
            serviceControl?.close()
            serviceControl = null
            state = null
            qrCode = null
        }

        var serviceControl : AudioServiceBinder? = null
            private set

        fun accept (binder: AudioServiceBinder?, dir: Direction? = null) {
            if (dir?.equals(state?.first) != false && binder != null) {
                if (state?.second == Connection.Connecting) {
                    serviceControl = binder
                    state = Pair(direction!!, Connection.Playing)
                }
                else if (dir != null) {
                    state = Pair(dir, Connection.Playing)
                    serviceControl = binder
                }
            }
        }

        fun connect (dir: Direction) {
            state = Pair(dir, Connection.Connecting)
        }

        fun <A> dirSelect(send : A, recv : A) : A? {
            return when (state?.first) {
                null -> null
                Direction.Send -> send
                Direction.Receive -> recv
            }
        }

        fun pause () {
            if (this.state?.second == Connection.Playing) {
                serviceControl!!.pause()
                this.state = Pair(direction!!, Connection.Paused)
            }
        }

        fun play () {
            if (this.state?.second == Connection.Paused) {
                serviceControl!!.play()
                this.state = Pair(direction!!, Connection.Playing)
            }
        }
    }

    private val intentFilter = IntentFilter().apply {
        addAction(App.HANDLE_CONNECT_FAILED)
        addAction(App.HANDLE_CONNECT_SUCCEEDED)
        /* P2P */
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    }

    private val intentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                App.HANDLE_CONNECTION_CLOSED -> {
                    channel?.close()
                    state.setIdle()
                    updateUi()
                }
                App.HANDLE_CONNECT_FAILED -> {
                    Toast.makeText(
                        this@MainActivity,
                        "Connection failed",
                        Toast.LENGTH_LONG
                    ).show()
                    channel?.close()
                    state.setIdle()
                    updateUi()
                }
                App.HANDLE_CONNECT_SUCCEEDED -> {
                    val service : ComponentName = intent
                        .getParcelableExtra(App.EXTRA_SERVICE_CLASS) ?: return
                    bindService(Intent().setComponent(service), serviceConnection, 0)
                }
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val connection = intent.getIntExtra(
                        WifiP2pManager.EXTRA_WIFI_STATE,
                        WifiP2pManager.WIFI_P2P_STATE_DISABLED
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(intentReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(intentReceiver)
    }

    override fun onStart() {
        super.onStart()

        disconnect.hide()

        TabLayoutMediator(tabLayout, tabViewer.viewPager){ tab, pos ->
            if (pos == 0) {
                tab.text = resources.getText(R.string.tab_send)
                tab.icon = getDrawable(R.drawable.ic_send)
            }

            if (pos == 1) {
                tab.text = resources.getText(R.string.tab_recv)
                tab.icon = getDrawable(R.drawable.ic_receive)
            }
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                if (state.isIdle) {
                    if (positionOffsetPixels != 0) {
                        fabMain.hide()
                    }
                    else {
                        if (position == 0) {
                            fabMain.setImageResource(R.drawable.ic_send_white)
                        }
                        else {
                            fabMain.setImageResource(R.drawable.ic_receive_white)

                            if (recvStateImg?.drawable == null) {
                                recvStateImg.setImageResource(R.drawable.ic_disconnect)
                            }
                        }
                        fabMain.show()
                    }
                }
            }
        })
    }

    private fun checkWifiP2PPermission() : Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
            return false
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_WIFI_STATE),
                REQUEST_LOCATION
            )
            return false
        }
        return true
    }

    private fun startP2PConnection() {
        if (p2pManager == null) {
            Toast.makeText(
                this,
                "wifi-direct is unsupported on this device",
                LENGTH_SHORT
            ).show()
            return
        }

        if (! checkWifiP2PPermission()) return

        p2pManager!!.requestDeviceInfo(channel!!) { d ->
            if (d != null) {
                val passphrase = "12344321"
                val networkName = "DIRECT-SPLITTER"
                val config = WifiP2pConfig.Builder()
                    .setNetworkName(networkName)
                    .setPassphrase(passphrase)
                    .build()

                p2pManager!!.createGroup(channel!!, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        val data = "direct;;$networkName;;${d.deviceAddress};;${App.ServerPort};;$passphrase"
                        state.qrCode = qrEncoder.encodeAsBitmap(data)
                        updateUi()
                    }

                    override fun onFailure(reason: Int) {
                        p2pManager!!.requestP2pState(channel!!) { s->
                            if (s == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                                val data = "direct;;$networkName;;${d.deviceAddress};;${App.ServerPort}"
                                state.qrCode = qrEncoder.encodeAsBitmap(data)
                                updateUi()
                            }
                            else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "couldn't open wifi-direct $reason",
                                    LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                })
            }
        }
    }

    fun onFabClicked(view: View) {
        if (state.isIdle) {
            if (viewPager.currentItem == 0) {
                //start sending
                state.connect(State.Direction.Send)

                if (wifiNetwork.isChecked) {
                    val ip = wifiManager.connectionInfo.ipAddress.toLong()
                    val host = InetAddress.getByAddress(
                        BigInteger.valueOf(ip).toByteArray().apply{ reverse() })
                    val ssid = wifiManager.connectionInfo.ssid

                    val data = "local;;${ssid};;${host.hostAddress};;${App.ServerPort};;"
                    state.qrCode = qrEncoder.encodeAsBitmap(data)

                    updateUi()
                    startCapture()
                }
                else {
                    startCapture()
                    startP2PConnection()
                    updateUi()
                }
            }
            else {
                //start receiving
                state.connect(State.Direction.Receive)

                val intent = Intent(applicationContext, QrCodeScanner::class.java)
                startActivityForResult(intent, App.REQUEST_SCAN_QR_CODE)
            }
        }
        else if (state.connection == State.Connection.Playing) {
            state.pause()
            updateUi()
        }
        else if (state.connection == State.Connection.Paused) {
            state.play()
            updateUi()
        }
    }

    fun closeClicked(view: View) {
        state.serviceControl?.close() ?:
            stopService(state.serviceName!!).also {_ ->
                state.setIdle()
                updateUi()
            }

        channel?.close()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        updateUi()
    }

    private fun updateUi() {
        if (state.isIdle) {
            disconnect.hide()
            wifiNetwork.isEnabled = true

            if (viewPager.currentItem == 0) {
                fabMain.setImageResource(R.drawable.ic_send_white)
            }
            else {
                fabMain.setImageResource(R.drawable.ic_receive_white)
            }

            sendStateImg?.setImageResource(R.drawable.ic_disconnect)
            recvStateImg?.setImageResource(R.drawable.ic_disconnect)
        }
        else {
            disconnect.show()
            state.dirSelect(recvStateImg, sendStateImg)
                ?.setImageResource(R.drawable.ic_disconnect)
            wifiNetwork.isEnabled = state.direction == State.Direction.Send

            when (state.connection) {
                State.Connection.Connecting -> {
                    fabMain.setImageResource(R.drawable.animated_wait)
                    (fabMain.drawable as AnimatedVectorDrawable).start()

                    if (state.direction == State.Direction.Send)  {
                        state.qrCode?.also { sendStateImg.setImageBitmap(it) }
                    }
                    else {
                        recvStateImg?.setImageDrawable(null)
                    }
                }
                State.Connection.Playing -> {
                    fabMain.setImageResource(R.drawable.ic_pause)
                    state.dirSelect(sendStateImg, recvStateImg)
                        ?.setImageResource(R.drawable.ic_connect)
                }
                State.Connection.Paused -> {
                    fabMain.setImageResource(R.drawable.ic_play)
                    state.dirSelect(sendStateImg, recvStateImg)
                        ?.setImageResource(R.drawable.ic_connect)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == App.REQUEST_SCAN_QR_CODE) {
            if (resultCode != RESULT_OK) {
                state.setIdle()
                updateUi()
                return
            }

            val ret = data!!.getStringExtra(App.EXTRA_KEY_QR_CODE)!!
            val info = Regex(";;").split(ret)
            if (info.size != 5) {
                state.setIdle()
                updateUi()
                Toast.makeText(this, "invalid qr code", LENGTH_SHORT).show()
                return
            }

            if (info[0] == "local") {
                if (wifiManager.connectionInfo.ssid != info[1]) {
                    Toast.makeText(
                        this,
                        "you need to connect to the same Wi-Fi",
                        Toast.LENGTH_LONG
                    ).show()
                }

                val ip = info[2]
                val port = info[3].toInt()

                Intent(this, ReceiveService::class.java).apply {
                    putExtra(App.EXTRA_KEY_HOST, ip)
                    putExtra(App.EXTRA_KEY_PORT, port)
                    startService(this)
                }
            }
            else {
                val config = WifiP2pConfig.Builder()
                    .setNetworkName(info[1])
                    .setDeviceAddress(MacAddress.fromString(info[2]))
                    .setPassphrase(info[4])
                    .build()

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION
                    )
                    return
                }
                p2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        p2pManager?.requestConnectionInfo(channel) {connection ->
                            if (connection != null) {
                                val ip = connection.groupOwnerAddress.hostAddress
                                val port = info[3]

                                Intent(this@MainActivity, ReceiveService::class.java).apply {
                                    putExtra(App.EXTRA_KEY_HOST, ip)
                                    putExtra(App.EXTRA_KEY_PORT, port)
                                    startService(this)
                                }
                            }
                        }
                    }

                    override fun onFailure(reason: Int) {
                        state.setIdle()
                        updateUi()
                        Toast.makeText(
                            this@MainActivity,
                            "connection failed",
                            LENGTH_SHORT
                        ).show()
                        return
                    }

                })
            }
        }
    }
}