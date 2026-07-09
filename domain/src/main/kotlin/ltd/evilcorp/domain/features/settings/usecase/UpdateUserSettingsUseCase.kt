package ltd.evilcorp.domain.features.settings.usecase

import ltd.evilcorp.domain.core.network.bootstrap.IBootstrapNodeRegistry
import ltd.evilcorp.domain.features.settings.model.BackupFrequency
import ltd.evilcorp.domain.features.settings.model.BootstrapNodeSource
import ltd.evilcorp.domain.features.settings.model.ProxyType
import ltd.evilcorp.domain.features.settings.repository.IUserSettingsRepository
import javax.inject.Inject

sealed interface UpdateAction {
    data class UdpEnabled(val enabled: Boolean) : UpdateAction
    data class ProxyPort(val port: Int) : UpdateAction
    data class ProxyTypeAction(val type: ProxyType) : UpdateAction
    data class ProxyAddressAction(val address: String) : UpdateAction
    data class BootstrapNodeSourceAction(val source: BootstrapNodeSource) : UpdateAction
    data class BackupFrequencyAction(val frequency: BackupFrequency) : UpdateAction
    data class BackupDestinationOrdinals(val ordinals: Set<Int>) : UpdateAction
    data class SentMessageSoundUri(val uri: String) : UpdateAction
    data class CallRingtoneUri(val uri: String) : UpdateAction
    data class NotificationSoundUri(val uri: String) : UpdateAction
    data class ActiveChatSoundUri(val uri: String) : UpdateAction
    data class AutoSaveDirectoryUri(val uri: String) : UpdateAction
    data class LocalBackupDirectoryUri(val uri: String) : UpdateAction
}

/**
 * Use case to toggle run-at-startup preferences and configure receiver components.
 */
class UpdateUserSettingsUseCase @Inject constructor(
    private val userSettingsRepository: IUserSettingsRepository,
    private val nodeRegistry: IBootstrapNodeRegistry,
) {
    suspend fun execute(action: UpdateAction) {
        when (action) {
            is UpdateAction.UdpEnabled -> userSettingsRepository.updateUdpEnabled(action.enabled)
            is UpdateAction.ProxyPort -> userSettingsRepository.updateProxyPort(action.port)
            is UpdateAction.ProxyTypeAction -> userSettingsRepository.updateProxyType(action.type)
            is UpdateAction.ProxyAddressAction -> userSettingsRepository.updateProxyAddress(action.address)
            is UpdateAction.BootstrapNodeSourceAction -> {
                userSettingsRepository.updateBootstrapNodeSource(action.source)
                nodeRegistry.reset()
            }
            is UpdateAction.BackupFrequencyAction -> userSettingsRepository.updateBackupFrequency(action.frequency)
            is UpdateAction.BackupDestinationOrdinals -> userSettingsRepository.updateBackupDestinationOrdinals(action.ordinals)
            is UpdateAction.SentMessageSoundUri -> userSettingsRepository.updateSentMessageSoundUri(action.uri)
            is UpdateAction.CallRingtoneUri -> userSettingsRepository.updateCallRingtoneUri(action.uri)
            is UpdateAction.NotificationSoundUri -> userSettingsRepository.updateNotificationSoundUri(action.uri)
            is UpdateAction.ActiveChatSoundUri -> userSettingsRepository.updateActiveChatSoundUri(action.uri)
            is UpdateAction.AutoSaveDirectoryUri -> userSettingsRepository.updateAutoSaveDirectoryUri(action.uri)
            is UpdateAction.LocalBackupDirectoryUri -> userSettingsRepository.updateLocalBackupDirectoryUri(action.uri)
        }
    }
}
