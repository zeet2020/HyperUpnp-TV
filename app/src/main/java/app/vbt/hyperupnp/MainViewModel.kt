package app.vbt.hyperupnp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import app.vbt.hyperupnp.models.CustomListItem
import app.vbt.hyperupnp.models.DeviceModel
import app.vbt.hyperupnp.models.ItemModel
import java.util.*

class MainViewModel : ViewModel() {

    val deviceList: ArrayList<CustomListItem> = ArrayList()
    val itemList: ArrayList<CustomListItem> = ArrayList()
    val folders: Stack<ItemModel> = Stack()

    var currentDevice: DeviceModel? = null

    private val _isShowingDeviceList = MutableLiveData(true)
    val isShowingDeviceList: LiveData<Boolean> = _isShowingDeviceList

    fun setShowingDeviceList(value: Boolean) {
        _isShowingDeviceList.value = value
    }

    fun getShowingDeviceList(): Boolean = _isShowingDeviceList.value ?: true
}
