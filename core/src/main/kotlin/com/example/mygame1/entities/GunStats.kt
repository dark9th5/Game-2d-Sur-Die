package com.example.mygame1.entities

data class GunStats(
    val fireRate: Float,      // số lần bắn mỗi giây
    val damage: Int,
    val bulletSize: Float,    // kích thước viên đạn
    val bulletRange: Float    // tầm xa viên đạn
)

fun getGunStats(type: GunType): GunStats = when(type) {
    GunType.Gun -> GunStats(
        fireRate = 1f,
        damage = 20,
        bulletSize = 40f,
        bulletRange = 300f // nửa 600
    )
    GunType.Machine -> GunStats(
        fireRate = 4f,
        damage = 5,
        bulletSize = 40f,
        bulletRange = 600f
    )
    GunType.Silencer -> GunStats(
        fireRate = 1f,
        damage = 10,
        bulletSize = 30f,
        bulletRange = 600f
    )
    GunType.Bomb -> GunStats(
        fireRate = 0.066f, // 1/15s chỉ để tránh chia 0
        damage = 50,
        bulletSize = 0f,
        bulletRange = 150f // bán kính nổ
    )
    GunType.Shield -> GunStats(
        fireRate = 0.1f,
        damage = 0,
        bulletSize = 0f,
        bulletRange = 0f
    ) // Shield không gây sát thương
    GunType.Trap -> GunStats(
        fireRate = 0.1f,
        damage = 0,
        bulletSize = 0f,
        bulletRange = 0f
    ) // Trap dùng range làm bán kính ảnh hưởng
}
