package ltd.evilcorp.domain.model

/**
 * Custom Stable annotation to mark domain models as stable for the Jetpack Compose compiler
 * without introducing a direct dependency on the Compose runtime.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class Stable
