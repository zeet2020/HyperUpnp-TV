package app.vbt.hyperupnp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        val toolbar2 = findViewById<MaterialToolbar>(R.id.toolbar2)
        setSupportActionBar(toolbar2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar2.setNavigationOnClickListener { onBackPressed() }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val playerPref = findPreference<androidx.preference.ListPreference>("settings_choose_player")
            playerPref?.let { setupPlayerList(it) }
        }

        private fun setupPlayerList(preference: androidx.preference.ListPreference) {
            val pm = requireContext().packageManager
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intent.type = "video/*"
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            val entries = mutableListOf<CharSequence>("Open/Stream the File")
            val entryValues = mutableListOf<CharSequence>("try_to_open")

            for (resolveInfo in resolveInfos) {
                val appName = resolveInfo.loadLabel(pm)
                val packageName = resolveInfo.activityInfo.packageName
                val activityName = resolveInfo.activityInfo.name
                val componentName = android.content.ComponentName(packageName, activityName).flattenToString()
                
                // Avoid duplicates if multiple activities from same app or if we want to filter
                 entries.add("Stream in $appName")
                 entryValues.add(componentName)
            }

            preference.entries = entries.toTypedArray()
            preference.entryValues = entryValues.toTypedArray()
        }
    }
}