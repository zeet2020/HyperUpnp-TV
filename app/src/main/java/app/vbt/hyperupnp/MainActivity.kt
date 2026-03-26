package app.vbt.hyperupnp

import android.content.*
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.IBinder
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.vbt.hyperupnp.androidupnp.*
import app.vbt.hyperupnp.databinding.ActivityMainBinding
import app.vbt.hyperupnp.models.CustomListItem
import app.vbt.hyperupnp.models.DeviceModel
import app.vbt.hyperupnp.models.ItemModel
import app.vbt.hyperupnp.upnp.cling.model.meta.Service
import com.google.android.material.color.DynamicColors
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(), Callbacks,
    SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private var recyclerview: RecyclerView? = null
    private var emptyIndicatorView: View? = null
    private var loadingIndicator: ProgressBar? = null
    private lateinit var drawerLayout: DrawerLayout

    private var isListView: Boolean = false
    private var mService: AndroidUpnpService? = null

    private val mListener: BrowseRegistryListener = BrowseRegistryListener(this, mService, this)

    private lateinit var mDeviceListAdapter: CustomListAdapter
    private lateinit var mItemListAdapter: CustomListAdapter

    private fun updateShowingDeviceListUI(isShowing: Boolean) {
        (emptyIndicatorView as TextView?)?.text =
            if (isShowing) this.getText(R.string.device_empty) else this.getText(R.string.looks_empty)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val state = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN
                )
                when (state) {
                    WifiManager.WIFI_STATE_ENABLED -> {
                        Timber.d("WiFi enabled, refreshing")
                        refreshDevices()
                        refreshCurrent()
                    }
                    WifiManager.WIFI_STATE_DISABLED -> {
                        Timber.d("WiFi disabled, clearing lists")
                        val mdlsize = viewModel.deviceList.size
                        val milsize = viewModel.itemList.size
                        viewModel.deviceList.clear()
                        viewModel.itemList.clear()
                        mDeviceListAdapter.notifyItemRangeRemoved(0, mdlsize)
                        mItemListAdapter.notifyItemRangeRemoved(0, milsize)
                    }
                    WifiManager.WIFI_STATE_UNKNOWN -> {
                        refreshDevices()
                        refreshCurrent()
                    }
                }
            }
        }
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mService = service as AndroidUpnpService
            val upnpService = mService ?: return
            Timber.d("UPnP service connected")
            upnpService.registry.addListener(mListener)
            upnpService.registry.devices.forEach { device ->
                mListener.deviceAdded(device)
            }
            upnpService.controlPoint.search()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Timber.d("UPnP service disconnected")
            mService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        DynamicColors.applyToActivitiesIfAvailable(application)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        recyclerview = findViewById<RecyclerViewPlus>(R.id.recyclerView)
        emptyIndicatorView = findViewById(R.id.looksempty)
        loadingIndicator = findViewById(R.id.loading_indicator)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        isListView = prefs.getBoolean("settings_list_view", false)

        recyclerview?.layoutManager = if (isListView) {
            androidx.recyclerview.widget.LinearLayoutManager(this)
        } else {
            GridLayoutManager(this, prefs.getInt("settings_grid_count", 4))
        }

        mDeviceListAdapter = CustomListAdapter(viewModel.deviceList,
            { c: CustomListItem -> navigateTo(c) }) { c: CustomListItem ->
            onLongClickCustomListItem(c)
        }
        mItemListAdapter = CustomListAdapter(viewModel.itemList,
            { c: CustomListItem -> navigateTo(c) }) { c: CustomListItem ->
            onLongClickCustomListItem(c)
        }
        mDeviceListAdapter.isListView = isListView
        mItemListAdapter.isListView = isListView

        viewModel.isShowingDeviceList.observe(this) { isShowing ->
            updateShowingDeviceListUI(isShowing)
            recyclerview?.adapter = if (isShowing) mDeviceListAdapter else mItemListAdapter
        }

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                navView.requestFocus()
            }
        })

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    viewModel.folders.clear()
                    viewModel.currentDevice = null
                    val milsize = viewModel.itemList.size
                    viewModel.itemList.clear()
                    mItemListAdapter.notifyItemRangeRemoved(0, milsize)
                    viewModel.setShowingDeviceList(true)
                    onDisplayDevices()
                    refreshDevices()
                }
                R.id.action_search -> {
                    openSearch()
                }
                R.id.action_toggle_view -> {
                    toggleViewMode()
                }
                R.id.action_refresh -> {
                    if (viewModel.getShowingDeviceList()) refreshDevices() else refreshCurrent()
                }
R.id.action_shuffle -> {
                    if (viewModel.getShowingDeviceList()) {
                        mDeviceListAdapter.customListFilterList.shuffle()
                        mDeviceListAdapter.notifyItemRangeRemoved(0, viewModel.deviceList.size)
                    } else {
                        mItemListAdapter.customListFilterList.shuffle()
                        mItemListAdapter.notifyItemRangeRemoved(0, viewModel.itemList.size)
                    }
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                R.id.action_go_up -> {
                    if (!viewModel.getShowingDeviceList()) onBackPressed()
                    if (viewModel.getShowingDeviceList()) refreshDevices()
                }
                R.id.action_quit -> {
                    finishAffinity()
                    exitProcess(0)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        refreshDevices()
        refreshCurrent()
        prefs.registerOnSharedPreferenceChangeListener(this)
        val filter = IntentFilter()
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED")
        registerReceiver(receiver, filter)
        bindServiceConnection()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        updateToggleViewIcon()

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (viewModel.getShowingDeviceList()) {
                    mDeviceListAdapter.filter.filter(query)
                } else {
                    mItemListAdapter.filter.filter(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (viewModel.getShowingDeviceList()) {
                    mDeviceListAdapter.filter.filter(newText)
                } else {
                    mItemListAdapter.filter.filter(newText)
                }
                return true
            }
        })

        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                item.isVisible = false
                if (viewModel.getShowingDeviceList()) {
                    mDeviceListAdapter.filter.filter("")
                } else {
                    mItemListAdapter.filter.filter("")
                }
                return true
            }
        })

        (recyclerview as RecyclerViewPlus?)?.onEmpty = {
            emptyIndicatorView?.visibility = View.VISIBLE
        }
        (recyclerview as RecyclerViewPlus?)?.onNotEmpty = {
            emptyIndicatorView?.visibility = View.GONE
        }
        return true
    }

    private fun openSearch() {
        val searchItem = binding.toolbar.menu.findItem(R.id.action_search) ?: return
        searchItem.isVisible = true
        searchItem.expandActionView()
    }

    private fun updateToggleViewIcon() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.menu.findItem(R.id.action_toggle_view)?.setIcon(
            if (isListView) R.drawable.ic_icon_grid else R.drawable.ic_view_list
        )
        navView.menu.findItem(R.id.action_toggle_view)?.title =
            if (isListView) "Grid View" else "List View"
    }

    private fun toggleViewMode() {
        isListView = !isListView
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit().putBoolean("settings_list_view", isListView).apply()

        mDeviceListAdapter.isListView = isListView
        mItemListAdapter.isListView = isListView

        recyclerview?.layoutManager = if (isListView) {
            androidx.recyclerview.widget.LinearLayoutManager(this)
        } else {
            GridLayoutManager(
                this,
                PreferenceManager.getDefaultSharedPreferences(this).getInt("settings_grid_count", 4)
            )
        }

        updateToggleViewIcon()

        mDeviceListAdapter.notifyDataSetChanged()
        mItemListAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                currentFocus?.performClick()
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (!viewModel.getShowingDeviceList()) {
                    onBackPressed()
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                if (viewModel.getShowingDeviceList()) refreshDevices() else refreshCurrent()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun showLoading() {
        loadingIndicator?.visibility = View.VISIBLE
        loadingIndicator?.announceForAccessibility(getString(R.string.loading_content))
    }

    private fun hideLoading() {
        loadingIndicator?.visibility = View.GONE
    }

    private fun playItem(item: ItemModel) {
        var scp = ""
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val uri = Uri.parse(item.url)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "video/*|audio/*|image/*")
            intent.putExtra("title", item.title)
            scp = prefs.getString("settings_choose_player", "try_to_open") ?: "try_to_open"
            if (scp != "try_to_open") {
                val cn = ComponentName.unflattenFromString(scp)
                if (cn != null) {
                    intent.component = cn
                }
            }
            startActivity(intent)
        } catch (ex: NullPointerException) {
            Timber.w(ex, "Failed to start player activity")
            Toast.makeText(this, R.string.info_could_not_start_activity, Toast.LENGTH_SHORT).show()
        } catch (ex: ActivityNotFoundException) {
            Timber.w(ex, "No handler found for player: %s", scp)
            Toast.makeText(this, R.string.info_no_handler, Toast.LENGTH_SHORT).show()
        }
    }

    private fun onLongClickCustomListItem(item: CustomListItem): Boolean {
        if (item is DeviceModel) {
            AlertDialog.Builder(this)
                .setTitle("Choose an action:\n${item.title}")
                .setItems(
                    arrayOf(
                        "Open",
                        "Copy Device Title",
                        "Copy Base URL",
                        "Open Thumbnail URL",
                        "Copy Thumbnail URL"
                    )
                ) { _, which ->
                    when (which) {
                        0 -> navigateTo(item)
                        1 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Copied Device Title", item.title)
                            )
                        }
                        2 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText(
                                    "Copied Base URL",
                                    Uri.parse(item.iconUrl).host
                                )
                            )
                        }
                        3 -> {
                            val intent = Intent()
                            intent.action = Intent.ACTION_VIEW
                            intent.data = Uri.parse(item.iconUrl)
                            startActivity(intent)
                        }
                        4 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Copied Thumbnail URL", item.iconUrl)
                            )
                        }
                    }
                }
                .create()
                .show()
        }
        if (item is ItemModel) {
            AlertDialog.Builder(this)
                .setTitle("Choose an action:\n${item.title}")
                .setItems(
                    arrayOf(
                        "Open/Stream",
                        "Copy Title",
                        "Copy Stream URL",
                        "Open Thumbnail URL",
                        "Copy Thumbnail URL"
                    )
                ) { _, which ->
                    when (which) {
                        0 -> if (item.isContainer) navigateTo(item) else playItem(item)
                        1 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Copied Title", item.title)
                            )
                        }
                        2 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Copied Stream URL", item.url)
                            )
                        }
                        3 -> {
                            val intent = Intent()
                            intent.action = Intent.ACTION_VIEW
                            intent.data = Uri.parse(item.iconUrl)
                            startActivity(intent)
                        }
                        4 -> {
                            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                ClipData.newPlainText("Copied Thumbnail URL", item.iconUrl)
                            )
                        }
                    }
                }
                .create()
                .show()
        }
        return true
    }

    private fun navigateTo(model: Any?) {
        if (model is DeviceModel) {

            val device = model.device
            if (device.isFullyHydrated) {
                val conDir = model.contentDirectory
                if (conDir != null) {
                    showLoading()
                    mService?.controlPoint?.execute(
                        CustomContentBrowseActionCallback(this, conDir, "0", this)
                    )
                }
                onDisplayDirectories()
                viewModel.setShowingDeviceList(false)
                viewModel.currentDevice = model
            } else {
                Toast.makeText(this, R.string.info_still_loading, Toast.LENGTH_SHORT).show()
            }
        }
        if (model is ItemModel) {
            if (model.isContainer) {
    
                if (viewModel.folders.isEmpty()) viewModel.folders.push(model) else if (viewModel.folders.peek().id !== model.id) viewModel.folders.push(
                    model
                )
                (model.service as Service<*, *>?)?.let { service ->
                    showLoading()
                    mService?.controlPoint?.execute(
                        CustomContentBrowseActionCallback(
                            this,
                            service,
                            model.id,
                            this
                        )
                    )
                }
            } else {
                playItem(model)
            }
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else if (goBack()) {
            super.onBackPressed()
        }
    }

    private fun goBack(): Boolean {
        if (viewModel.folders.empty()) {
            if (!viewModel.getShowingDeviceList()) {
                viewModel.setShowingDeviceList(true)
                onDisplayDevices()
            } else {
                return true
            }
        } else {
            val item = viewModel.folders.pop()
            val parentID = item.container?.parentID ?: return false
            showLoading()
            mService?.controlPoint?.execute(
                CustomContentBrowseActionCallback(
                    this,
                    item.service as Service<*, *>,
                    parentID,
                    this
                )
            )
        }
        return false
    }

    fun refreshDevices() {
        val service = mService ?: return
        service.registry.removeAllRemoteDevices()
        for (device in service.registry.devices) mListener.deviceAdded(device)
        service.controlPoint.search()
    }

    fun refreshCurrent() {
        val service = mService ?: return
        if (viewModel.getShowingDeviceList()) {
            onDisplayDevices()
            service.registry.removeAllRemoteDevices()
            for (device in service.registry.devices) mListener.deviceAdded(device)
            service.controlPoint.search()
        } else {
            if (!viewModel.folders.empty()) {
                val item = viewModel.folders.peek() ?: return
                showLoading()
                service.controlPoint.execute(
                    CustomContentBrowseActionCallback(
                        this,
                        item.service as Service<*, *>,
                        item.id,
                        this
                    )
                )
            } else {
                val contentDir = viewModel.currentDevice?.contentDirectory
                if (contentDir != null) {
                    showLoading()
                    service.controlPoint.execute(
                        CustomContentBrowseActionCallback(this, contentDir, "0", this)
                    )
                }
            }
        }
    }

    private fun bindServiceConnection(): Boolean {
        bindService(
            Intent(this, AndroidUpnpServiceImpl::class.java),
            serviceConnection, Context.BIND_AUTO_CREATE
        )
        return true
    }

    private fun unbindServiceConnection(): Boolean {
        mService?.registry?.removeListener(mListener)
        unbindService(serviceConnection)
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unbindServiceConnection()
    }

    override fun onDisplayDevices() {
        lifecycleScope.launch(Dispatchers.Main) {
            hideLoading()
            viewModel.setShowingDeviceList(true)
        }
    }

    override fun onDisplayDirectories() {
        lifecycleScope.launch(Dispatchers.Main) {
            hideLoading()
            val milsize = viewModel.itemList.size
            viewModel.itemList.clear()
            mItemListAdapter.notifyItemRangeRemoved(0, milsize)
            viewModel.setShowingDeviceList(false)
        }
    }

    override fun addItem(Item: ItemModel) {
        lifecycleScope.launch(Dispatchers.Main) {
            hideLoading()
            viewModel.itemList.add(Item)
            mItemListAdapter.notifyItemInserted(viewModel.itemList.size - 1)
        }
    }

    override fun clearItems() {
        lifecycleScope.launch(Dispatchers.Main) {
            val milsize = viewModel.itemList.size
            viewModel.itemList.clear()
            mItemListAdapter.notifyItemRangeRemoved(0, milsize)
        }
    }

    override fun itemError(error: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            hideLoading()
            Timber.e("Item browsing error: %s", error)
            val milsize = viewModel.itemList.size
            viewModel.itemList.clear()
            mItemListAdapter.notifyItemRangeRemoved(0, milsize)
            viewModel.itemList.add(
                CustomListItem(
                    R.drawable.ic_warning,
                    resources.getString(R.string.info_error_list_folders),
                    error
                )
            )
            mItemListAdapter.notifyItemInserted(viewModel.itemList.size - 1)
        }
    }

    override fun addDevice(device: DeviceModel) {
        lifecycleScope.launch(Dispatchers.Main) {
            val position: Int = viewModel.deviceList.indexOf(device as CustomListItem)
            if (position >= 0) {
                viewModel.deviceList.remove(device)
                mDeviceListAdapter.notifyItemRemoved(position)
                mDeviceListAdapter.notifyItemRangeChanged(position, viewModel.deviceList.size - position)
                viewModel.deviceList.add(position, device)
                mDeviceListAdapter.notifyItemInserted(position)
            } else {
                viewModel.deviceList.add(device)
                mDeviceListAdapter.notifyItemInserted(viewModel.deviceList.size - 1)
            }
        }
    }

    override fun rmDevice(device: DeviceModel) {
        lifecycleScope.launch(Dispatchers.Main) {
            val position: Int = viewModel.deviceList.indexOf(device as CustomListItem)
            viewModel.deviceList.remove(device)
            mDeviceListAdapter.notifyItemRemoved(position)
            mDeviceListAdapter.notifyItemRangeChanged(position, viewModel.deviceList.size - position)
        }
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {
        if (p1 != null && p1 == "settings_grid_count")
            recreate()
        if (p1 != null && p1 == "settings_validate_devices") {
            refreshDevices()
            refreshCurrent()
        }
    }
}
