package ltd.evilcorp.atox.infrastructure.tox

import android.content.Context
import java.io.File
import javax.inject.Inject
import ltd.evilcorp.atox.R
import ltd.evilcorp.atox.infrastructure.settings.Settings
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeJsonSource

class AndroidBootstrapNodeJsonSource @Inject constructor(
    private val context: Context,
    private val settings: Settings,
) : IBootstrapNodeJsonSource {
    override fun load(): String? = runCatching {
        if (settings.bootstrapNodeSource == BootstrapNodeSource.BuiltIn) {
            context.resources.openRawResource(R.raw.nodes).use { String(it.readBytes()) }
        } else {
            File(context.filesDir, "user_nodes.json").takeIf(File::exists)?.readBytes()?.decodeToString()
        }
    }.getOrNull()
}
