package ltd.evilcorp.domain.features.group

data class GroupInvite(
    val friendNo: Int,
    val inviteData: ByteArray,
    val groupName: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupInvite) return false
        return friendNo == other.friendNo && inviteData.contentEquals(other.inviteData) && groupName == other.groupName
    }

    override fun hashCode(): Int {
        var result = friendNo
        result = 31 * result + inviteData.contentHashCode()
        result = 31 * result + groupName.hashCode()
        return result
    }
}
