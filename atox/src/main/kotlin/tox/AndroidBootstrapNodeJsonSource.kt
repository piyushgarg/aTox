package ltd.evilcorp.atox.tox

import android.content.Context
import java.io.File
import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.settings.Settings
import ltd.evilcorp.core.model.BootstrapNodeSource
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeJsonSource

class AndroidBootstrapNodeJsonSource @Inject constructor(
    private val context: Context,
    private val settings: Settings,
) : BootstrapNodeJsonSource {
    override fun load(): String? = runCatching {
        if (settings.bootstrapNodeSource == BootstrapNodeSource.BuiltIn) {
            context.resources.openRawResource(R.raw.nodes).use { String(it.readBytes()) }
        } else {
            File(context.filesDir, "user_nodes.json").takeIf(File::exists)?.readBytes()?.decodeToString()
        }
    }.getOrNull()
}
