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
        bulletSize = 30f,      // nhỏ bằng 1/2
        bulletRange = 1200f   // gấp đôi
    )
}
