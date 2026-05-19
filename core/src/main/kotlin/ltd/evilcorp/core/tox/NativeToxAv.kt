package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.listener.ToxAvEventListener

class NativeToxAv {
    init {
        System.loadLibrary("nativetox")
    }

    external fun toxavNew(tox: Long): Long
    external fun toxavKill(toxav: Long)

    external fun toxavIterate(toxav: Long, listener: ToxAvEventListener)
    external fun toxavIterationInterval(toxav: Long): Int

    external fun toxavCall(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavAnswer(toxav: Long, friendNumber: Int, audioBitRate: Int, videoBitRate: Int): Boolean
    external fun toxavCallControl(toxav: Long, friendNumber: Int, control: Int): Boolean
    
    external fun toxavAudioSendFrame(toxav: Long, friendNumber: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int): Boolean

    // ===================================================================================
    // Передача видео-кадров и регулировка битрейта звонков
    // ===================================================================================

    /**
     * Отправляет видео-кадр (YUV420P) вашему собеседнику в активном видеозвонке.
     * @param toxav Указатель на нативный инстанс ToxAV.
     * @param friendNumber Номер друга.
     * @param width Ширина видео-кадра.
     * @param height Высота видео-кадра.
     * @param y Массив Y-плоскости (яркость).
     * @param u Массив U-плоскости (хроматическая синяя разность).
     * @param v Массив V-плоскости (хроматическая красная разность).
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxavVideoSendFrame(toxav: Long, friendNumber: Int, width: Int, height: Int, y: ByteArray, u: ByteArray, v: ByteArray): Boolean

    /**
     * Изменяет битрейт аудио «на лету» для текущего соединения звонка.
     * @param toxav Указатель на нативный инстанс ToxAV.
     * @param friendNumber Номер друга.
     * @param bitrate Новый аудио-битрейт в bps.
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxavAudioSetBitRate(toxav: Long, friendNumber: Int, bitrate: Int): Boolean

    /**
     * Изменяет битрейт видео «на лету» для текущего соединения звонка.
     * @param toxav Указатель на нативный инстанс ToxAV.
     * @param friendNumber Номер друга.
     * @param bitrate Новый видео-битрейт в bps.
     * @return true в случае успеха, false в случае ошибки.
     */
    external fun toxavVideoSetBitRate(toxav: Long, friendNumber: Int, bitrate: Int): Boolean

    // ===================================================================================
    // Аудио/Видео групповые чаты (ToxAV Group API)
    // ===================================================================================

    /**
     * Создает групповую аудио-конференцию на основе существующего инстанса Tox.
     * @param tox Указатель на нативный инстанс Tox.
     * @return Номер созданного голосового чата, либо -1 в случае ошибки.
     */
    external fun toxavAddAvGroupchat(tox: Long): Int

    /**
     * Присоединяется к существующей групповой аудио-конференции.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы Tox.
     * @return Номер присоединенной аудио-группы, либо -1 в случае ошибки.
     */
    external fun toxavJoinAvGroupchat(tox: Long, groupNumber: Int): Int

    /**
     * Отправляет аудио-кадр вашего голоса в групповой чат.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы Tox.
     * @param pcm Массив аудиоданных (PCM 16-бит).
     * @param sampleCount Количество аудио-сэмплов.
     * @param channels Количество каналов (обычно 1 - моно, или 2 - стерео).
     * @param samplingRate Частота дискретизации аудио (например, 48000).
     * @return 0 в случае успеха, либо код ошибки.
     */
    external fun toxavGroupSendAudio(tox: Long, groupNumber: Int, pcm: ShortArray, sampleCount: Int, channels: Int, samplingRate: Int): Int

    /**
     * Включает аудио/видео функции для указанного группового чата.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return 0 в случае успеха, либо код ошибки.
     */
    external fun toxavGroupchatEnableAv(tox: Long, groupNumber: Int): Int

    /**
     * Выключает аудио/видео функции для указанного группового чата.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return 0 в случае успеха, либо код ошибки.
     */
    external fun toxavGroupchatDisableAv(tox: Long, groupNumber: Int): Int

    /**
     * Проверяет, активны ли аудио/видео функции в указанном групповом чате.
     * @param tox Указатель на нативный инстанс Tox.
     * @param groupNumber Номер группы.
     * @return true если активны, false если отключены или произошла ошибка.
     */
    external fun toxavGroupchatAvEnabled(tox: Long, groupNumber: Int): Boolean
}
