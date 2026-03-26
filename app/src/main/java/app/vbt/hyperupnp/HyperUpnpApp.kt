package app.vbt.hyperupnp

import android.app.Application
import timber.log.Timber

class HyperUpnpApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
