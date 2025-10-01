package com.example.mygame1.world

import com.badlogic.gdx.maps.tiled.TiledMap
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer
import com.badlogic.gdx.maps.objects.PolygonMapObject
import com.badlogic.gdx.maps.objects.RectangleMapObject
import com.badlogic.gdx.maps.objects.EllipseMapObject
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Ellipse
import ktx.tiled.x
import ktx.tiled.y

class CollisionManager(
    private val tileLayer: TiledMapTileLayer,
    map: TiledMap
) {
    val polygons = mutableListOf<Polygon>()
    val ellipses = mutableListOf<Ellipse>()

    init {
        val objectLayer = map.layers.get("Object Layer 1")
        for (obj in objectLayer.objects) {
            val collisionProp = obj.properties["Collision"]
            if (collisionProp == true || collisionProp == "true") {
                when (obj) {
                    is PolygonMapObject -> {
                        val poly = Polygon(obj.polygon.vertices)
                        poly.setPosition(obj.x, obj.y)
                        polygons.add(poly)
                    }
                    is RectangleMapObject -> {
                        val rect = obj.rectangle
                        val poly = Polygon(floatArrayOf(
                            0f, 0f,
                            rect.width, 0f,
                            rect.width, rect.height,
                            0f, rect.height
                        ))
                        poly.setPosition(rect.x, rect.y)
                        polygons.add(poly)
                    }
                    is EllipseMapObject -> {
                        val ellipse = obj.ellipse
                        ellipses.add(ellipse)
                    }
                }
            }
        }
    }

    // Chuyển Rectangle thành Polygon để kiểm tra overlap
    private fun Rectangle.toPolygon(): Polygon {
        return Polygon(floatArrayOf(
            x, y,
            x + width, y,
            x + width, y + height,
            x, y + height
        ))
    }

    // Kiểm tra overlap giữa ellipse và rect (bounding box)
    private fun ellipseOverlapsRect(ellipse: Ellipse, rect: Rectangle): Boolean {
        // Ellipse: x, y là góc trên trái, width, height là bán kính ngang/dọc * 2
        // Rect: x, y là góc trên trái, width, height
        // => Chuyển ellipse về tâm, bán kính, rồi kiểm tra sát mép

        val ellipseCenterX = ellipse.x + ellipse.width / 2f
        val ellipseCenterY = ellipse.y + ellipse.height / 2f
        val ellipseRadiusX = ellipse.width / 2f
        val ellipseRadiusY = ellipse.height / 2f

        // Tìm điểm gần nhất trên rect tới tâm ellipse
        val closestX = clamp(ellipseCenterX, rect.x, rect.x + rect.width)
        val closestY = clamp(ellipseCenterY, rect.y, rect.y + rect.height)

        val dx = ellipseCenterX - closestX
        val dy = ellipseCenterY - closestY

        // Chuẩn hóa khoảng cách theo bán kính ellipse
        val normDx = dx / ellipseRadiusX
        val normDy = dy / ellipseRadiusY

        return (normDx * normDx + normDy * normDy) <= 1f
    }

    private fun clamp(value: Float, min: Float, max: Float): Float {
        return Math.max(min, Math.min(value, max))
    }

    // Kiểm tra va chạm cho nhân vật di chuyển (dùng bounding box)
    fun isBlocked(rect: Rectangle): Boolean {
        // Không còn tile layer vật cản → bỏ kiểm tra tile

        // Kiểm tra polygon & rectangle vật cản
        val rectPoly = rect.toPolygon()
        for (poly in polygons) {
            if (Intersector.overlapConvexPolygons(rectPoly, poly)) {
                return true
            }
        }
        // Kiểm tra ellipse vật cản
        for (ellipse in ellipses) {
            if (ellipseOverlapsRect(ellipse, rect)) {
                return true
            }
        }
        return false
    }

    // Kiểm tra va chạm bounding box của viên đạn với polygon/ellipse/rectangle vật cản
    fun isBulletBlocked(bulletRect: Rectangle): Boolean {
        // Không còn tile layer vật cản → bỏ kiểm tra tile

        val bulletPoly = bulletRect.toPolygon()
        for (poly in polygons) {
            if (Intersector.overlapConvexPolygons(bulletPoly, poly)) {
                return true
            }
        }
        for (ellipse in ellipses) {
            if (ellipseOverlapsRect(ellipse, bulletRect)) {
                return true
            }
        }
        return false
    }

    fun isLineBlocked(start: com.badlogic.gdx.math.Vector2, end: com.badlogic.gdx.math.Vector2): Boolean {
        // Lấy mẫu từng bước nhỏ theo đường thẳng, tạo rect nhỏ kiểm tra
        val steps = 20
        val dx = (end.x - start.x) / steps
        val dy = (end.y - start.y) / steps
        val probeSize = 8f
        val rect = Rectangle()
        for (i in 0..steps) {
            val x = start.x + dx * i - probeSize/2f
            val y = start.y + dy * i - probeSize/2f
            rect.set(x, y, probeSize, probeSize)
            if (isBlocked(rect)) return true
        }
        return false
    }
}
