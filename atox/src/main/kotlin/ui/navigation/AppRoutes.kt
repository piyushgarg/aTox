package ltd.evilcorp.atox.ui.navigation

import androidx.navigation.NavController

private const val ARG_PUBLIC_KEY = "publicKey"
private const val ARG_TOX_ID = "toxId"

object AppRoutes {
    const val Launch = "launch"
    const val Unlock = "unlock"
    const val ContactList = "contact_list"
    const val Chats = "main/chats"
    const val AddContactTab = "main/add_contact"
    const val Profile = "main/profile"
    const val Settings = "main/settings"
    const val SettingsLanguage = "settings/language"
    const val SettingsTheme = "settings/theme"
    const val SettingsSounds = "settings/sounds"
    const val SettingsBackup = "settings/backup"
    const val SettingsAppearance = "settings/appearance"
    const val SettingsChat = "settings/chat"
    const val SettingsConnection = "settings/connection"
    const val CreateProfile = "create_profile"
    const val Chat = "chat/{$ARG_PUBLIC_KEY}"
    const val Call = "call/{$ARG_PUBLIC_KEY}"
    const val AddContact = "add_contact?${ARG_TOX_ID}={${ARG_TOX_ID}}"
    const val ForwardSelection = "chat/forward?message={message}"
    fun forwardSelection(message: String) = "chat/forward?message=${android.net.Uri.encode(message)}"

    fun chat(publicKey: String) = "chat/$publicKey"

    fun call(publicKey: String) = "call/$publicKey"

    fun addContact(toxId: String? = null): String {
        return if (toxId.isNullOrBlank()) {
            "add_contact"
        } else {
            "add_contact?$ARG_TOX_ID=$toxId"
        }
    }

    fun isChat(route: String?) = route?.startsWith("chat/") == true

    fun isCall(route: String?) = route?.startsWith("call/") == true

    fun isAddContact(route: String?) = route?.startsWith("add_contact") == true

    fun isMainTab(route: String?) = route == Chats ||
        route == AddContactTab ||
        route == Profile ||
        route == Settings

    const val PublicKeyArg = ARG_PUBLIC_KEY
    const val ToxIdArg = ARG_TOX_ID
}

fun NavController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
