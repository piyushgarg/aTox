package ltd.evilcorp.atox.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ltd.evilcorp.atox.DisableRestart

@Module
@InstallIn(SingletonComponent::class)
object RestartConfigModule {
    @Provides
    @DisableRestart
    @Suppress("FunctionOnlyReturningConstant")
    fun provideDisableRestart(): Boolean = false
}
