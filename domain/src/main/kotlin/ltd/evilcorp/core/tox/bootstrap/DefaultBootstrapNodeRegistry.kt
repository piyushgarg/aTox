package ltd.evilcorp.core.tox.bootstrap

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Стандартная реализация [BootstrapNodeRegistry] для управления узлами бутстрапа.
 * Загружает список узлов из [BootstrapNodeJsonSource], парсит его через [BootstrapNodeJsonParser]
 * и предоставляет перемешанную (shuffled) выборку узлов для подключения.
 */
@Singleton
class DefaultBootstrapNodeRegistry @Inject constructor(
    private val parser: BootstrapNodeJsonParser,
    private val source: BootstrapNodeJsonSource,
) : BootstrapNodeRegistry {
    private var nodes: List<BootstrapNode> = emptyList()

    init {
        reset()
    }

    /**
     * Предоставляет случайную выборку из [n] узлов бутстрапа.
     */
    override fun get(n: Int): List<BootstrapNode> =
        nodes.asSequence().shuffled().take(n).toList()

    /**
     * Сбрасывает состояние реестра и перезагружает кэш узлов из источника данных.
     */
    override fun reset() {
        nodes = source.load()?.let(parser::parse).orEmpty()
    }
}
