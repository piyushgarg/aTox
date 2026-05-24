package ltd.evilcorp.core.tox

import ltd.evilcorp.core.tox.enums.ToxConnection
import ltd.evilcorp.core.tox.enums.ToxFileKind
import ltd.evilcorp.core.tox.enums.ToxMessageType
import ltd.evilcorp.core.tox.enums.ToxUserStatus
import ltd.evilcorp.core.model.ConnectionStatus
import ltd.evilcorp.core.model.FileKind
import ltd.evilcorp.core.model.MessageType
import ltd.evilcorp.core.model.UserStatus

/**
 * Преобразует строковое HEX-представление в массив байтов.
 * Каждые два символа строки преобразуются в один байт.
 * @return Массив байтов [ByteArray].
 */
fun String.hexToBytes(): ByteArray = chunked(2).map { it.uppercase().toInt(radix = 16).toByte() }.toByteArray()

/**
 * Преобразует массив байтов в строковое HEX-представление в верхнем регистре.
 * Каждый байт форматируется в двухзначное шестнадцатеричное число.
 * @return Строка HEX [String].
 */
fun ByteArray.bytesToHex(): String = this.joinToString("") { "%02X".format(it) }

/**
 * Преобразует доменную модель статуса пользователя [UserStatus] во внутренний тип перечисления [ToxUserStatus].
 */
fun UserStatus.toToxType(): ToxUserStatus = when (this) {
    UserStatus.None -> ToxUserStatus.NONE
    UserStatus.Away -> ToxUserStatus.AWAY
    UserStatus.Busy -> ToxUserStatus.BUSY
}

/**
 * Преобразует доменную модель типа сообщения [MessageType] во внутренний тип перечисления [ToxMessageType].
 */
fun MessageType.toToxType(): ToxMessageType = when (this) {
    MessageType.Normal -> ToxMessageType.NORMAL
    MessageType.Action -> ToxMessageType.ACTION
    MessageType.FileTransfer -> throw Exception("Тип сообщения FileTransfer не поддерживается напрямую в протоколе Tox")
    MessageType.GroupEvent -> ToxMessageType.NORMAL
}

/**
 * Преобразует доменную модель типа файла [FileKind] в целочисленное представление [ToxFileKind] для JNI-слоя.
 */
fun FileKind.toToxtype(): Int = when (this) {
    FileKind.Avatar -> ToxFileKind.AVATAR.ordinal
    FileKind.Data -> ToxFileKind.DATA.ordinal
}
