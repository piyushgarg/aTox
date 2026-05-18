package ltd.evilcorp.atox.di

import android.content.Context
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ltd.evilcorp.atox.tox.AndroidBootstrapNodeJsonSource
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeRegistry
import ltd.evilcorp.core.tox.bootstrap.BootstrapNodeJsonSource
import ltd.evilcorp.core.tox.bootstrap.DefaultBootstrapNodeRegistry
import ltd.evilcorp.core.tox.save.AndroidSaveManager
import ltd.evilcorp.core.tox.save.SaveManager

@Module
class AppModule {
    @Provides
    fun provideBootstrapNodeRegistry(nodeRegistry: DefaultBootstrapNodeRegistry): BootstrapNodeRegistry = nodeRegistry

    @Provides
    fun provideBootstrapNodeJsonSource(source: AndroidBootstrapNodeJsonSource): BootstrapNodeJsonSource = source

    @Provides
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(Dispatchers.Default)

    @Provides
    fun provideSaveManager(ctx: Context): SaveManager = AndroidSaveManager(ctx)
}
