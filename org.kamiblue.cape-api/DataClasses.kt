data class CapeUser(
        val id: Long,
        var capes: ArrayList<Cape>,
        @SerializedName("is_premium")
        var isPremium: Boolean = false
)

data class Cape(
        @SerializedName("player_uuid")
        var playerUUID: String?,
        @SerializedName("cape_uuid")
        val capeUUID: String = UUID.randomUUID().toString().substring(0, 5),
        val type: CapeType
)

enum class CapeType(val realName: String, val imageKey: String) {
    BOOSTER("Booster", "booster"),
    CONTEST("Contest", "contest"),
    CONTRIBUTOR("Contributor", "github1"),
    DONOR("Donor", "donator2"),
    INVITER("Inviter", "inviter")
}