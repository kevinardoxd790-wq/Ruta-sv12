package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val AddLocationAlt: ImageVector by lazy {
        ImageVector.Builder(name = "AddLocationAlt", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.0f, 1.0f); verticalLineToRelative(3.0f); horizontalLineToRelative(3.0f)
                verticalLineToRelative(2.0f); horizontalLineToRelative(-3.0f); verticalLineToRelative(3.0f)
                horizontalLineToRelative(-2.0f); lineTo(18.0f, 6.0f); horizontalLineToRelative(-3.0f)
                lineTo(15.0f, 4.0f); horizontalLineToRelative(3.0f); lineTo(18.0f, 1.0f)
                horizontalLineToRelative(2.0f); close(); moveTo(12.0f, 13.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f); reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f)
                reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f); close(); moveTo(14.0f, 3.25f)
                lineTo(14.0f, 7.0f); horizontalLineToRelative(3.0f); verticalLineToRelative(3.0f)
                horizontalLineToRelative(2.92f); curveToRelative(0.05f, 0.39f, 0.08f, 0.79f, 0.08f, 1.2f); curveToRelative(0.0f, 3.32f, -2.67f, 7.25f, -8.0f, 11.8f)
                curveToRelative(-5.33f, -4.55f, -8.0f, -8.48f, -8.0f, -11.8f); curveTo(4.0f, 6.22f, 7.8f, 3.0f, 12.0f, 3.0f); curveToRelative(0.68f, 0.0f, 1.35f, 0.08f, 2.0f, 0.25f)
                close()
            }
        }.build()
    }

    val AltRoute: ImageVector by lazy {
        ImageVector.Builder(name = "AltRoute", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9.78f, 11.16f); lineToRelative(-1.42f, 1.42f); curveToRelative(-0.68f, -0.69f, -1.34f, -1.58f, -1.79f, -2.94f)
                lineToRelative(1.94f, -0.49f); curveTo(8.83f, 10.04f, 9.28f, 10.65f, 9.78f, 11.16f); close()
                moveTo(11.0f, 6.0f); lineTo(7.0f, 2.0f); lineTo(3.0f, 6.0f)
                horizontalLineToRelative(3.02f); curveTo(6.04f, 6.81f, 6.1f, 7.54f, 6.21f, 8.17f); lineToRelative(1.94f, -0.49f)
                curveTo(8.08f, 7.2f, 8.03f, 6.63f, 8.02f, 6.0f); horizontalLineTo(11.0f); close()
                moveTo(21.0f, 6.0f); lineToRelative(-4.0f, -4.0f); lineToRelative(-4.0f, 4.0f)
                horizontalLineToRelative(2.99f); curveToRelative(-0.1f, 3.68f, -1.28f, 4.75f, -2.54f, 5.88f); curveToRelative(-0.5f, 0.44f, -1.01f, 0.92f, -1.45f, 1.55f)
                curveToRelative(-0.34f, -0.49f, -0.73f, -0.88f, -1.13f, -1.24f); lineTo(9.46f, 13.6f); curveTo(10.39f, 14.45f, 11.0f, 15.14f, 11.0f, 17.0f)
                verticalLineToRelative(5.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(-5.0f)
                curveToRelative(0.0f, -2.02f, 0.71f, -2.66f, 1.79f, -3.63f); curveToRelative(1.38f, -1.24f, 3.08f, -2.78f, 3.2f, -7.37f); horizontalLineTo(21.0f)
                close()
            }
        }.build()
    }

    val Bookmark: ImageVector by lazy {
        ImageVector.Builder(name = "Bookmark", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.0f, 3.0f); horizontalLineTo(7.0f); curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(5.0f, 21.0f); lineToRelative(7.0f, -3.0f); lineToRelative(7.0f, 3.0f)
                verticalLineTo(5.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
            }
        }.build()
    }

    val BookmarkBorder: ImageVector by lazy {
        ImageVector.Builder(name = "BookmarkBorder", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.0f, 3.0f); lineTo(7.0f, 3.0f); curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                lineTo(5.0f, 21.0f); lineToRelative(7.0f, -3.0f); lineToRelative(7.0f, 3.0f)
                lineTo(19.0f, 5.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f); close()
                moveTo(17.0f, 18.0f); lineToRelative(-5.0f, -2.18f); lineTo(7.0f, 18.0f)
                lineTo(7.0f, 5.0f); horizontalLineToRelative(10.0f); verticalLineToRelative(13.0f)
                close()
            }
        }.build()
    }

    val ContentPaste: ImageVector by lazy {
        ImageVector.Builder(name = "ContentPaste", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.0f, 2.0f); horizontalLineToRelative(-4.18f); curveTo(14.4f, 0.84f, 13.3f, 0.0f, 12.0f, 0.0f)
                curveToRelative(-1.3f, 0.0f, -2.4f, 0.84f, -2.82f, 2.0f); lineTo(5.0f, 2.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(16.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(14.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); lineTo(21.0f, 4.0f); curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                close(); moveTo(12.0f, 2.0f); curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
                reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f); reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f); reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
                close(); moveTo(19.0f, 20.0f); lineTo(5.0f, 20.0f)
                lineTo(5.0f, 4.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(3.0f)
                horizontalLineToRelative(10.0f); lineTo(17.0f, 4.0f); horizontalLineToRelative(2.0f)
                verticalLineToRelative(16.0f); close()
            }
        }.build()
    }

    val DarkMode: ImageVector by lazy {
        ImageVector.Builder(name = "DarkMode", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 3.0f); curveToRelative(-4.97f, 0.0f, -9.0f, 4.03f, -9.0f, 9.0f); reflectiveCurveToRelative(4.03f, 9.0f, 9.0f, 9.0f)
                reflectiveCurveToRelative(9.0f, -4.03f, 9.0f, -9.0f); curveToRelative(0.0f, -0.46f, -0.04f, -0.92f, -0.1f, -1.36f); curveToRelative(-0.98f, 1.37f, -2.58f, 2.26f, -4.4f, 2.26f)
                curveToRelative(-2.98f, 0.0f, -5.4f, -2.42f, -5.4f, -5.4f); curveToRelative(0.0f, -1.81f, 0.89f, -3.42f, 2.26f, -4.4f); curveTo(12.92f, 3.04f, 12.46f, 3.0f, 12.0f, 3.0f)
                lineTo(12.0f, 3.0f); close()
            }
        }.build()
    }

    val DeleteOutline: ImageVector by lazy {
        ImageVector.Builder(name = "DeleteOutline", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 19.0f); curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); lineTo(18.0f, 7.0f); lineTo(6.0f, 7.0f)
                verticalLineToRelative(12.0f); close(); moveTo(8.0f, 9.0f)
                horizontalLineToRelative(8.0f); verticalLineToRelative(10.0f); lineTo(8.0f, 19.0f)
                lineTo(8.0f, 9.0f); close(); moveTo(15.5f, 4.0f)
                lineToRelative(-1.0f, -1.0f); horizontalLineToRelative(-5.0f); lineToRelative(-1.0f, 1.0f)
                lineTo(5.0f, 4.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(14.0f)
                lineTo(19.0f, 4.0f); close()
            }
        }.build()
    }

    val Directions: ImageVector by lazy {
        ImageVector.Builder(name = "Directions", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21.41f, 10.59f); lineToRelative(-7.99f, -8.0f); curveToRelative(-0.78f, -0.78f, -2.05f, -0.78f, -2.83f, 0.0f)
                lineToRelative(-8.01f, 8.0f); curveToRelative(-0.78f, 0.78f, -0.78f, 2.05f, 0.0f, 2.83f); lineToRelative(8.01f, 8.0f)
                curveToRelative(0.78f, 0.78f, 2.05f, 0.78f, 2.83f, 0.0f); lineToRelative(7.99f, -8.0f); curveTo(22.2f, 12.63f, 22.2f, 11.37f, 21.41f, 10.59f)
                close(); moveTo(13.5f, 14.5f); verticalLineTo(12.0f)
                horizontalLineTo(10.0f); verticalLineToRelative(3.0f); horizontalLineTo(8.0f)
                verticalLineToRelative(-4.0f); curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f); horizontalLineToRelative(4.5f)
                verticalLineTo(7.5f); lineTo(17.0f, 11.0f); lineTo(13.5f, 14.5f)
                close()
            }
        }.build()
    }

    val DirectionsCar: ImageVector by lazy {
        ImageVector.Builder(name = "DirectionsCar", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.92f, 6.01f); curveTo(18.72f, 5.42f, 18.16f, 5.0f, 17.5f, 5.0f); horizontalLineToRelative(-11.0f)
                curveToRelative(-0.66f, 0.0f, -1.21f, 0.42f, -1.42f, 1.01f); lineTo(3.0f, 12.0f); verticalLineToRelative(8.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f); horizontalLineToRelative(1.0f); curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-1.0f); horizontalLineToRelative(12.0f); verticalLineToRelative(1.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f); horizontalLineToRelative(1.0f); curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineToRelative(-8.0f); lineToRelative(-2.08f, -5.99f); close()
                moveTo(6.5f, 16.0f); curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f); reflectiveCurveTo(5.67f, 13.0f, 6.5f, 13.0f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f); reflectiveCurveTo(7.33f, 16.0f, 6.5f, 16.0f); close()
                moveTo(17.5f, 16.0f); curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f); reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f); reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f); close()
                moveTo(5.0f, 11.0f); lineToRelative(1.5f, -4.5f); horizontalLineToRelative(11.0f)
                lineTo(19.0f, 11.0f); lineTo(5.0f, 11.0f); close()
            }
        }.build()
    }

    val DirectionsWalk: ImageVector by lazy {
        ImageVector.Builder(name = "DirectionsWalk", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.5f, 5.5f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f); reflectiveCurveToRelative(-0.9f, -2.0f, -2.0f, -2.0f)
                reflectiveCurveToRelative(-2.0f, 0.9f, -2.0f, 2.0f); reflectiveCurveToRelative(0.9f, 2.0f, 2.0f, 2.0f); close()
                moveTo(9.8f, 8.9f); lineTo(7.0f, 23.0f); horizontalLineToRelative(2.1f)
                lineToRelative(1.8f, -8.0f); lineToRelative(2.1f, 2.0f); verticalLineToRelative(6.0f)
                horizontalLineToRelative(2.0f); verticalLineToRelative(-7.5f); lineToRelative(-2.1f, -2.0f)
                lineToRelative(0.6f, -3.0f); curveTo(14.8f, 12.0f, 16.8f, 13.0f, 19.0f, 13.0f); verticalLineToRelative(-2.0f)
                curveToRelative(-1.9f, 0.0f, -3.5f, -1.0f, -4.3f, -2.4f); lineToRelative(-1.0f, -1.6f); curveToRelative(-0.4f, -0.6f, -1.0f, -1.0f, -1.7f, -1.0f)
                curveToRelative(-0.3f, 0.0f, -0.5f, 0.1f, -0.8f, 0.1f); lineTo(6.0f, 8.3f); verticalLineTo(13.0f)
                horizontalLineToRelative(2.0f); verticalLineTo(9.6f); lineToRelative(1.8f, -0.7f)
            }
        }.build()
    }

    val Explore: ImageVector by lazy {
        ImageVector.Builder(name = "Explore", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 10.9f); curveToRelative(-0.61f, 0.0f, -1.1f, 0.49f, -1.1f, 1.1f); reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
                curveToRelative(0.61f, 0.0f, 1.1f, -0.49f, 1.1f, -1.1f); reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f); close()
                moveTo(12.0f, 2.0f); curveTo(6.48f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f); reflectiveCurveToRelative(4.48f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.48f, 10.0f, -10.0f); reflectiveCurveTo(17.52f, 2.0f, 12.0f, 2.0f); close()
                moveTo(14.19f, 14.19f); lineTo(6.0f, 18.0f); lineToRelative(3.81f, -8.19f)
                lineTo(18.0f, 6.0f); lineToRelative(-3.81f, 8.19f); close()
            }
        }.build()
    }

    val Flag: ImageVector by lazy {
        ImageVector.Builder(name = "Flag", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.4f, 6.0f); lineTo(14.0f, 4.0f); horizontalLineTo(5.0f)
                verticalLineToRelative(17.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(-7.0f)
                horizontalLineToRelative(5.6f); lineToRelative(0.4f, 2.0f); horizontalLineToRelative(7.0f)
                verticalLineTo(6.0f); close()
            }
        }.build()
    }

    val GpsFixed: ImageVector by lazy {
        ImageVector.Builder(name = "GpsFixed", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 8.0f); curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f); reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f); reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f); close()
                moveTo(20.94f, 11.0f); curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f); lineTo(13.0f, 1.0f)
                horizontalLineToRelative(-2.0f); verticalLineToRelative(2.06f); curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
                lineTo(1.0f, 11.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f); lineTo(11.0f, 23.0f); horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.06f); curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f); lineTo(23.0f, 13.0f)
                verticalLineToRelative(-2.0f); horizontalLineToRelative(-2.06f); close()
                moveTo(12.0f, 19.0f); curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f); reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f); reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f); close()
            }
        }.build()
    }

    val GpsOff: ImageVector by lazy {
        ImageVector.Builder(name = "GpsOff", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.94f, 11.0f); curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f); lineTo(13.0f, 1.0f)
                horizontalLineToRelative(-2.0f); verticalLineToRelative(2.06f); curveToRelative(-1.13f, 0.12f, -2.19f, 0.46f, -3.16f, 0.97f)
                lineToRelative(1.5f, 1.5f); curveTo(10.16f, 5.19f, 11.06f, 5.0f, 12.0f, 5.0f); curveToRelative(3.87f, 0.0f, 7.0f, 3.13f, 7.0f, 7.0f)
                curveToRelative(0.0f, 0.94f, -0.19f, 1.84f, -0.52f, 2.65f); lineToRelative(1.5f, 1.5f); curveToRelative(0.5f, -0.96f, 0.84f, -2.02f, 0.97f, -3.15f)
                lineTo(23.0f, 13.0f); verticalLineToRelative(-2.0f); horizontalLineToRelative(-2.06f)
                close(); moveTo(3.0f, 4.27f); lineToRelative(2.04f, 2.04f)
                curveTo(3.97f, 7.62f, 3.25f, 9.23f, 3.06f, 11.0f); lineTo(1.0f, 11.0f); verticalLineToRelative(2.0f)
                horizontalLineToRelative(2.06f); curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f); lineTo(11.0f, 23.0f)
                horizontalLineToRelative(2.0f); verticalLineToRelative(-2.06f); curveToRelative(1.77f, -0.2f, 3.38f, -0.91f, 4.69f, -1.98f)
                lineTo(19.73f, 21.0f); lineTo(21.0f, 19.73f); lineTo(4.27f, 3.0f)
                lineTo(3.0f, 4.27f); close(); moveTo(16.27f, 17.54f)
                curveTo(15.09f, 18.45f, 13.61f, 19.0f, 12.0f, 19.0f); curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f); curveToRelative(0.0f, -1.61f, 0.55f, -3.09f, 1.46f, -4.27f)
                lineToRelative(9.81f, 9.81f); close()
            }
        }.build()
    }

    val LightMode: ImageVector by lazy {
        ImageVector.Builder(name = "LightMode", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 7.0f); curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f); reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f)
                reflectiveCurveToRelative(5.0f, -2.24f, 5.0f, -5.0f); reflectiveCurveTo(14.76f, 7.0f, 12.0f, 7.0f); lineTo(12.0f, 7.0f)
                close(); moveTo(2.0f, 13.0f); lineToRelative(2.0f, 0.0f)
                curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f); reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f); lineToRelative(-2.0f, 0.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f); reflectiveCurveTo(1.45f, 13.0f, 2.0f, 13.0f); close()
                moveTo(20.0f, 13.0f); lineToRelative(2.0f, 0.0f); curveToRelative(0.55f, 0.0f, 1.0f, -0.45f, 1.0f, -1.0f)
                reflectiveCurveToRelative(-0.45f, -1.0f, -1.0f, -1.0f); lineToRelative(-2.0f, 0.0f); curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                reflectiveCurveTo(19.45f, 13.0f, 20.0f, 13.0f); close(); moveTo(11.0f, 2.0f)
                verticalLineToRelative(2.0f); curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f); reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                verticalLineTo(2.0f); curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f); reflectiveCurveTo(11.0f, 1.45f, 11.0f, 2.0f)
                close(); moveTo(11.0f, 20.0f); verticalLineToRelative(2.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f); reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f); verticalLineToRelative(-2.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f); curveTo(11.45f, 19.0f, 11.0f, 19.45f, 11.0f, 20.0f); close()
                moveTo(5.99f, 4.58f); curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0.0f); curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.41f)
                lineToRelative(1.06f, 1.06f); curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0.0f); reflectiveCurveToRelative(0.39f, -1.03f, 0.0f, -1.41f)
                lineTo(5.99f, 4.58f); close(); moveTo(18.36f, 16.95f)
                curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0.0f); curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.41f); lineToRelative(1.06f, 1.06f)
                curveToRelative(0.39f, 0.39f, 1.03f, 0.39f, 1.41f, 0.0f); curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0.0f, -1.41f); lineTo(18.36f, 16.95f)
                close(); moveTo(19.42f, 5.99f); curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0.0f, -1.41f)
                curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0.0f); lineToRelative(-1.06f, 1.06f); curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.41f)
                reflectiveCurveToRelative(1.03f, 0.39f, 1.41f, 0.0f); lineTo(19.42f, 5.99f); close()
                moveTo(7.05f, 18.36f); curveToRelative(0.39f, -0.39f, 0.39f, -1.03f, 0.0f, -1.41f); curveToRelative(-0.39f, -0.39f, -1.03f, -0.39f, -1.41f, 0.0f)
                lineToRelative(-1.06f, 1.06f); curveToRelative(-0.39f, 0.39f, -0.39f, 1.03f, 0.0f, 1.41f); reflectiveCurveToRelative(1.03f, 0.39f, 1.41f, 0.0f)
                lineTo(7.05f, 18.36f); close()
            }
        }.build()
    }

    val MyLocation: ImageVector by lazy {
        ImageVector.Builder(name = "MyLocation", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 8.0f); curveToRelative(-2.21f, 0.0f, -4.0f, 1.79f, -4.0f, 4.0f); reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f); reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f); close()
                moveTo(20.94f, 11.0f); curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f); lineTo(13.0f, 1.0f)
                horizontalLineToRelative(-2.0f); verticalLineToRelative(2.06f); curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11.0f)
                lineTo(1.0f, 11.0f); verticalLineToRelative(2.0f); horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f); lineTo(11.0f, 23.0f); horizontalLineToRelative(2.0f)
                verticalLineToRelative(-2.06f); curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f); lineTo(23.0f, 13.0f)
                verticalLineToRelative(-2.0f); horizontalLineToRelative(-2.06f); close()
                moveTo(12.0f, 19.0f); curveToRelative(-3.87f, 0.0f, -7.0f, -3.13f, -7.0f, -7.0f); reflectiveCurveToRelative(3.13f, -7.0f, 7.0f, -7.0f)
                reflectiveCurveToRelative(7.0f, 3.13f, 7.0f, 7.0f); reflectiveCurveToRelative(-3.13f, 7.0f, -7.0f, 7.0f); close()
            }
        }.build()
    }

    val OfflinePin: ImageVector by lazy {
        ImageVector.Builder(name = "OfflinePin", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.0f, 2.0f); curveTo(6.5f, 2.0f, 2.0f, 6.5f, 2.0f, 12.0f); reflectiveCurveToRelative(4.5f, 10.0f, 10.0f, 10.0f)
                reflectiveCurveToRelative(10.0f, -4.5f, 10.0f, -10.0f); reflectiveCurveTo(17.5f, 2.0f, 12.0f, 2.0f); close()
                moveTo(17.0f, 18.0f); horizontalLineTo(7.0f); verticalLineToRelative(-2.0f)
                horizontalLineToRelative(10.0f); verticalLineTo(18.0f); close()
                moveTo(10.3f, 14.0f); lineTo(7.0f, 10.7f); lineToRelative(1.4f, -1.4f)
                lineToRelative(1.9f, 1.9f); lineToRelative(5.3f, -5.3f); lineTo(17.0f, 7.3f)
                lineTo(10.3f, 14.0f); close()
            }
        }.build()
    }

    val Remove: ImageVector by lazy {
        ImageVector.Builder(name = "Remove", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.0f, 13.0f); horizontalLineTo(5.0f); verticalLineToRelative(-2.0f)
                horizontalLineToRelative(14.0f); verticalLineToRelative(2.0f); close()
            }
        }.build()
    }

    val RoundaboutRight: ImageVector by lazy {
        ImageVector.Builder(name = "RoundaboutRight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.92f, 8.0f); curveTo(13.44f, 5.16f, 10.97f, 3.0f, 8.0f, 3.0f); curveTo(4.69f, 3.0f, 2.0f, 5.69f, 2.0f, 9.0f)
                curveToRelative(0.0f, 2.97f, 2.16f, 5.44f, 5.0f, 5.92f); lineTo(7.0f, 21.0f); horizontalLineToRelative(2.0f)
                lineToRelative(0.0f, -6.09f); curveToRelative(0.0f, -0.98f, -0.71f, -1.8f, -1.67f, -1.97f); curveTo(5.44f, 12.63f, 4.0f, 10.98f, 4.0f, 9.0f)
                curveToRelative(0.0f, -2.21f, 1.79f, -4.0f, 4.0f, -4.0f); curveToRelative(1.98f, 0.0f, 3.63f, 1.44f, 3.94f, 3.33f); curveTo(12.11f, 9.29f, 12.93f, 10.0f, 13.91f, 10.0f)
                lineToRelative(4.26f, 0.0f); lineToRelative(-1.59f, 1.59f); lineTo(18.0f, 13.0f)
                lineToRelative(4.0f, -4.0f); lineToRelative(-4.0f, -4.0f); lineToRelative(-1.41f, 1.41f)
                lineTo(18.17f, 8.0f); lineTo(13.92f, 8.0f); close()
            }
        }.build()
    }

    val Straight: ImageVector by lazy {
        ImageVector.Builder(name = "Straight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.0f, 6.83f); lineToRelative(-1.59f, 1.58f); lineToRelative(-1.41f, -1.41f)
                lineToRelative(4.0f, -4.0f); lineToRelative(4.0f, 4.0f); lineToRelative(-1.41f, 1.41f)
                lineToRelative(-1.59f, -1.58f); lineToRelative(0.0f, 14.17f); lineToRelative(-2.0f, 0.0f)
                close()
            }
        }.build()
    }

    val TurnLeft: ImageVector by lazy {
        ImageVector.Builder(name = "TurnLeft", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.83f, 11.0f); lineToRelative(1.59f, 1.59f); lineTo(7.0f, 14.0f)
                lineToRelative(-4.0f, -4.0f); lineToRelative(4.0f, -4.0f); lineToRelative(1.41f, 1.41f)
                lineTo(6.83f, 9.0f); lineTo(15.0f, 9.0f); curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                verticalLineToRelative(9.0f); horizontalLineToRelative(-2.0f); verticalLineToRelative(-9.0f)
                lineTo(6.83f, 11.0f); close()
            }
        }.build()
    }

    val TurnRight: ImageVector by lazy {
        ImageVector.Builder(name = "TurnRight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17.17f, 11.0f); lineToRelative(-1.59f, 1.59f); lineTo(17.0f, 14.0f)
                lineToRelative(4.0f, -4.0f); lineToRelative(-4.0f, -4.0f); lineToRelative(-1.41f, 1.41f)
                lineTo(17.17f, 9.0f); lineTo(9.0f, 9.0f); curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
                verticalLineToRelative(9.0f); horizontalLineToRelative(2.0f); verticalLineToRelative(-9.0f)
                lineTo(17.17f, 11.0f); close()
            }
        }.build()
    }

    val TurnSharpLeft: ImageVector by lazy {
        ImageVector.Builder(name = "TurnSharpLeft", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.0f, 6.83f); lineTo(4.41f, 8.41f); lineTo(3.0f, 7.0f)
                lineToRelative(4.0f, -4.0f); lineToRelative(4.0f, 4.0f); lineTo(9.59f, 8.41f)
                lineTo(8.0f, 6.83f); verticalLineTo(13.0f); horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(-2.0f)
                verticalLineToRelative(-6.0f); horizontalLineTo(8.0f); curveToRelative(-1.1f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
                verticalLineTo(6.83f); close()
            }
        }.build()
    }

    val TurnSharpRight: ImageVector by lazy {
        ImageVector.Builder(name = "TurnSharpRight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.0f, 6.83f); lineToRelative(1.59f, 1.59f); lineTo(21.0f, 7.0f)
                lineToRelative(-4.0f, -4.0f); lineToRelative(-4.0f, 4.0f); lineToRelative(1.41f, 1.41f)
                lineTo(16.0f, 6.83f); verticalLineTo(13.0f); horizontalLineTo(8.0f)
                curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f); verticalLineToRelative(6.0f); horizontalLineToRelative(2.0f)
                verticalLineToRelative(-6.0f); horizontalLineToRelative(8.0f); curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(6.83f); close()
            }
        }.build()
    }

    val TurnSlightLeft: ImageVector by lazy {
        ImageVector.Builder(name = "TurnSlightLeft", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.66f, 6.0f); verticalLineTo(4.0f); horizontalLineTo(6.0f)
                verticalLineToRelative(5.66f); horizontalLineToRelative(2.0f); verticalLineTo(7.41f)
                lineToRelative(5.0f, 5.0f); verticalLineTo(20.0f); horizontalLineToRelative(2.0f)
                verticalLineToRelative(-7.58f); curveToRelative(0.0f, -0.53f, -0.21f, -1.04f, -0.59f, -1.41f); lineToRelative(-5.0f, -5.0f)
                horizontalLineTo(11.66f); close()
            }
        }.build()
    }

    val TurnSlightRight: ImageVector by lazy {
        ImageVector.Builder(name = "TurnSlightRight", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.34f, 6.0f); verticalLineTo(4.0f); horizontalLineTo(18.0f)
                verticalLineToRelative(5.66f); horizontalLineToRelative(-2.0f); verticalLineTo(7.41f)
                lineToRelative(-5.0f, 5.0f); verticalLineTo(20.0f); horizontalLineTo(9.0f)
                verticalLineToRelative(-7.58f); curveToRelative(0.0f, -0.53f, 0.21f, -1.04f, 0.59f, -1.41f); lineToRelative(5.0f, -5.0f)
                horizontalLineTo(12.34f); close()
            }
        }.build()
    }

    val UTurnLeft: ImageVector by lazy {
        ImageVector.Builder(name = "UTurnLeft", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18.0f, 9.0f); verticalLineToRelative(12.0f); horizontalLineToRelative(-2.0f)
                verticalLineTo(9.0f); curveToRelative(0.0f, -2.21f, -1.79f, -4.0f, -4.0f, -4.0f); reflectiveCurveTo(8.0f, 6.79f, 8.0f, 9.0f)
                verticalLineToRelative(4.17f); lineToRelative(1.59f, -1.59f); lineTo(11.0f, 13.0f)
                lineToRelative(-4.0f, 4.0f); lineToRelative(-4.0f, -4.0f); lineToRelative(1.41f, -1.41f)
                lineTo(6.0f, 13.17f); verticalLineTo(9.0f); curveToRelative(0.0f, -3.31f, 2.69f, -6.0f, 6.0f, -6.0f)
                reflectiveCurveTo(18.0f, 5.69f, 18.0f, 9.0f); close()
            }
        }.build()
    }

    val Work: ImageVector by lazy {
        ImageVector.Builder(name = "Work", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.0f, 6.0f); horizontalLineToRelative(-4.0f); lineTo(16.0f, 4.0f)
                curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f); horizontalLineToRelative(-4.0f); curveToRelative(-1.11f, 0.0f, -2.0f, 0.89f, -2.0f, 2.0f)
                verticalLineToRelative(2.0f); lineTo(4.0f, 6.0f); curveToRelative(-1.11f, 0.0f, -1.99f, 0.89f, -1.99f, 2.0f)
                lineTo(2.0f, 19.0f); curveToRelative(0.0f, 1.11f, 0.89f, 2.0f, 2.0f, 2.0f); horizontalLineToRelative(16.0f)
                curveToRelative(1.11f, 0.0f, 2.0f, -0.89f, 2.0f, -2.0f); lineTo(22.0f, 8.0f); curveToRelative(0.0f, -1.11f, -0.89f, -2.0f, -2.0f, -2.0f)
                close(); moveTo(14.0f, 6.0f); horizontalLineToRelative(-4.0f)
                lineTo(10.0f, 4.0f); horizontalLineToRelative(4.0f); verticalLineToRelative(2.0f)
                close()
            }
        }.build()
    }
}
