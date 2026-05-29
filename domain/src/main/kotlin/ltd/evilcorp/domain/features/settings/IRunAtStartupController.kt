package ltd.evilcorp.domain.features.settings

/**
 * Interface to handle configuring run-at-startup component states.
 * Keeps business logic platform-agnostic and free of Android Context references.
 */
interface IRunAtStartupController {
    fun setRunAtStartup(enabled: Boolean)
}
