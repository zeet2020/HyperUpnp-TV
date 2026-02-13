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
            // 1. Check for apps that handle generic video type
            val intentType = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intentType.setDataAndType(android.net.Uri.parse("content://dummy.mp4"), "video/*")
            val resolveInfosType = pm.queryIntentActivities(intentType, 0)

            // 2. Check for apps that strictly handle file scheme (common for some players)
            val intentFile = android.content.Intent(android.content.Intent.ACTION_VIEW)
            intentFile.setDataAndType(android.net.Uri.parse("file:///dummy.mp4"), "video/*")
            val resolveInfosFile = pm.queryIntentActivities(intentFile, 0)

            // 3. Combine and Deduplicate
            val allResolves = (resolveInfosType + resolveInfosFile).distinctBy {
                it.activityInfo.packageName
            }

            val entries = mutableListOf<CharSequence>("Open/Stream the File")
            val entryValues = mutableListOf<CharSequence>("try_to_open")

            for (resolveInfo in allResolves) {
                val appName = resolveInfo.loadLabel(pm)
                val packageName = resolveInfo.activityInfo.packageName
                val activityName = resolveInfo.activityInfo.name
                val componentName = android.content.ComponentName(packageName, activityName).flattenToString()

                entries.add("Stream in $appName")
                entryValues.add(componentName)
            }

            preference.entries = entries.toTypedArray()
            preference.entryValues = entryValues.toTypedArray()
        }
    }
}