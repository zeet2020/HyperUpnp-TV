package app.vbt.hyperupnp.androidupnp

import android.content.Context
import app.vbt.hyperupnp.models.ItemModel
import app.vbt.hyperupnp.upnp.cling.model.action.ActionException
import app.vbt.hyperupnp.upnp.cling.model.action.ActionInvocation
import app.vbt.hyperupnp.upnp.cling.model.message.UpnpResponse
import app.vbt.hyperupnp.upnp.cling.model.meta.Device
import app.vbt.hyperupnp.upnp.cling.model.meta.Service
import app.vbt.hyperupnp.upnp.cling.model.types.ErrorCode
import app.vbt.hyperupnp.upnp.cling.support.contentdirectory.callback.Browse
import app.vbt.hyperupnp.upnp.cling.support.model.BrowseFlag
import app.vbt.hyperupnp.upnp.cling.support.model.DIDLContent
import app.vbt.hyperupnp.upnp.cling.support.model.DIDLObject
import app.vbt.hyperupnp.upnp.cling.support.model.SortCriterion
import app.vbt.hyperupnp.upnp.cling.support.model.item.Item
import timber.log.Timber
import java.net.URI

class CustomContentBrowseActionCallback(
    private val context: Context,
    private var service: Service<*, *>,
    id: String?,
    private var mCallbacks: Callbacks?
) :
    Browse(
        service as Service<out Device<*, *, *>, out Service<*, *>>,
        id,
        BrowseFlag.DIRECT_CHILDREN,
        "*",
        0,
        99999L,
        SortCriterion(true, "dc:title")
    ) {
    private fun createItemModel(item: DIDLObject): ItemModel? {
        val itemModel = ItemModel(context, service, item)
        var usableIcon: URI? = item.getFirstPropertyValue(DIDLObject.Property.UPNP.ICON::class.java)
        if (usableIcon == null || usableIcon.toString().isEmpty()) {
            usableIcon =
                item.getFirstPropertyValue(DIDLObject.Property.UPNP.ALBUM_ART_URI::class.java)
        }
        if (usableIcon != null) itemModel.iconUrl = usableIcon.toString()
        return itemModel
    }

    override fun received(actionInvocation: ActionInvocation<*>, didl: DIDLContent) {
        mCallbacks?.clearItems()
        try {
            for (childContainer in didl.containers) createItemModel(childContainer)?.let {
                mCallbacks?.addItem(it)
            }
            for (childItem in didl.items) createItemModel(childItem)?.let { mCallbacks?.addItem(it) }
        } catch (ex: Exception) {
            actionInvocation.failure = ActionException(
                ErrorCode.ACTION_FAILED,
                "Can't create list children: $ex", ex
            )
            failure(
                actionInvocation,
                UpnpResponse(UpnpResponse.Status.BAD_REQUEST),
                ex.message ?: ex.toString()
            )
        }
    }

    override fun updateStatus(status: Status) {}
    override fun failure(invocation: ActionInvocation<*>?, response: UpnpResponse?, s: String) {
        // Log the full error for debugging (Release build crash investigation)
        Timber.e(invocation?.failure, "Browse Action Failed: %s", s)

        var finalMessage = s
        if (invocation?.failure != null) {
             finalMessage += "\nCause: ${invocation.failure.message}"
             // Dig for root cause if available
             if (invocation.failure.cause != null) {
                 finalMessage += "\nRoot: ${invocation.failure.cause!!.message}"
             }
        }
        
        mCallbacks?.itemError(
            createDefaultFailureMessage(
                invocation,
                response
            ) + "\n\nDetails: $finalMessage"
        )
    }

    init {
        mCallbacks?.onDisplayDirectories()
    }
}