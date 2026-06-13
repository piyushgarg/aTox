package ltd.evilcorp.atox.ui

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import ltd.evilcorp.atox.DisableRestart
import ltd.evilcorp.atox.di.RestartConfigModule

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RestartConfigModule::class]
)
object TestRestartConfigModule {
    @Provides
    @DisableRestart
    @Suppress("FunctionOnlyReturningConstant")
    fun provideDisableRestart(): Boolean = true
}
