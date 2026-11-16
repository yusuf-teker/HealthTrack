package com.yusufteker.healthtrack.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.core.model.BadgeId
import com.yusufteker.healthtrack.core.model.SportType
import com.yusufteker.healthtrack.theme.HealthColors

/**
 * Minimalist geometric vector sport icons (24x24 viewport) matching the XML drawables in composeResources.
 * Uses clean single-weight strokes so icons stay crisp across workout cards, quick chips,
 * sport selector pills, and achievement popups in both light and dark themes.
 */
object SportIcons {

    val SELECTABLE_SPORTS: List<SportType> = listOf(
        SportType.TENNIS,
        SportType.FITNESS,
        SportType.RUNNING,
        SportType.ROWING,
        SportType.BASKETBALL,
        SportType.FOOTBALL,
        SportType.CYCLING,
        SportType.SWIMMING,
        SportType.WALKING,
        SportType.YOGA_PILATES,
        SportType.VOLLEYBALL,
        SportType.BOXING,
        SportType.HIIT,
        SportType.CALISTHENICS,
        SportType.DANCE
    )

    val TennisIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportTennis",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Tilted racket head oval
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11.5f, 5.8f)
                curveTo(13.65f, 3.65f, 16.76f, 3.26f, 18.4f, 4.9f)
                curveTo(20.04f, 6.54f, 19.65f, 9.65f, 17.5f, 11.8f)
                curveTo(15.35f, 13.95f, 12.24f, 14.34f, 10.6f, 12.7f)
                curveTo(8.96f, 11.06f, 9.35f, 7.95f, 11.5f, 5.8f)
                close()
            }
            // Subtle strings
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.15f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(11.6f, 10.4f); lineTo(16.6f, 5.4f)
                moveTo(13.4f, 12.2f); lineTo(18.0f, 7.6f)
                moveTo(12.2f, 6.5f); lineTo(16.8f, 11.1f)
                moveTo(14.2f, 4.9f); lineTo(18.2f, 8.9f)
            }
            // Racket handle
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(10.6f, 12.7f)
                lineTo(4.6f, 18.7f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.7f, 17.6f)
                lineTo(4.2f, 19.1f)
            }
            // Tennis ball
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f
            ) {
                moveTo(17.8f, 14.9f)
                curveTo(19.4f, 14.9f, 20.7f, 16.2f, 20.7f, 17.8f)
                curveTo(20.7f, 19.4f, 19.4f, 20.7f, 17.8f, 20.7f)
                curveTo(16.2f, 20.7f, 14.9f, 19.4f, 14.9f, 17.8f)
                curveTo(14.9f, 16.2f, 16.2f, 14.9f, 17.8f, 14.9f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.8f, 16.0f)
                curveTo(17.0f, 17.0f, 17.0f, 18.6f, 15.8f, 19.6f)
            }
        }.build()
    }

    val FitnessDumbbellIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportFitness",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(2.2f, 12f); lineTo(3.8f, 12f)
                moveTo(6.8f, 12f); lineTo(17.2f, 12f)
                moveTo(20.2f, 12f); lineTo(21.8f, 12f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.8f, 6.5f); lineTo(6.8f, 17.5f)
                moveTo(17.2f, 6.5f); lineTo(17.2f, 17.5f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.8f, 8.8f); lineTo(3.8f, 15.2f)
                moveTo(20.2f, 8.8f); lineTo(20.2f, 15.2f)
            }
        }.build()
    }

    val RunningAthleteIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportRunning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(15.6f, 3.4f)
                curveTo(16.6f, 3.4f, 17.4f, 4.2f, 17.4f, 5.2f)
                curveTo(17.4f, 6.2f, 16.6f, 7.0f, 15.6f, 7.0f)
                curveTo(14.6f, 7.0f, 13.8f, 6.2f, 13.8f, 5.2f)
                curveTo(13.8f, 4.2f, 14.6f, 3.4f, 15.6f, 3.4f)
                close()
            }
            // Torso & legs
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.6f, 8.4f)
                lineTo(11.4f, 13.6f)
                lineTo(15.2f, 15.8f)
                lineTo(14.0f, 20.4f)
                moveTo(11.4f, 13.6f)
                lineTo(8.6f, 17.4f)
                lineTo(5.4f, 18.2f)
            }
            // Arms
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.6f, 10.6f)
                lineTo(11.4f, 8.6f)
                lineTo(15.0f, 10.8f)
                lineTo(17.8f, 9.4f)
            }
            // Motion lines
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.0f, 9.6f); lineTo(6.0f, 9.6f)
                moveTo(2.2f, 13.2f); lineTo(5.8f, 13.2f)
            }
        }.build()
    }

    val RowingMachineIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportRowing",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Ergometer flywheel
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(5.6f, 11.6f)
                curveTo(7.31f, 11.6f, 8.7f, 12.99f, 8.7f, 14.7f)
                curveTo(8.7f, 16.41f, 7.31f, 17.8f, 5.6f, 17.8f)
                curveTo(3.89f, 17.8f, 2.5f, 16.41f, 2.5f, 14.7f)
                curveTo(2.5f, 12.99f, 3.89f, 11.6f, 5.6f, 11.6f)
                close()
            }
            // Monitor arm
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.8f, 11.8f)
                lineTo(9.0f, 6.6f)
                lineTo(11.2f, 6.2f)
            }
            // Monorail & support feet
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.4f, 16.0f); lineTo(21.0f, 16.0f)
                moveTo(4.4f, 17.7f); lineTo(3.6f, 19.2f)
                moveTo(20.2f, 16.0f); lineTo(21.0f, 19.2f)
            }
            // Sliding seat
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(13.6f, 13.6f); lineTo(17.0f, 13.6f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15.3f, 13.6f); lineTo(15.3f, 16.0f)
            }
            // Pull chain & handle
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8.2f, 12.8f); lineTo(12.8f, 10.5f)
                moveTo(12.8f, 9.0f); lineTo(12.8f, 11.9f)
            }
        }.build()
    }

    val BasketballHoopIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportBasketball",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Backboard rounded rect
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.8f, 3.6f)
                lineTo(19.2f, 3.6f)
                curveTo(20.1f, 3.6f, 20.8f, 4.3f, 20.8f, 5.2f)
                lineTo(20.8f, 12.4f)
                curveTo(20.8f, 13.3f, 20.1f, 14.0f, 19.2f, 14.0f)
                lineTo(4.8f, 14.0f)
                curveTo(3.9f, 14.0f, 3.2f, 13.3f, 3.2f, 12.4f)
                lineTo(3.2f, 5.2f)
                curveTo(3.2f, 4.3f, 3.9f, 3.6f, 4.8f, 3.6f)
                close()
            }
            // Inner square
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(9.0f, 7.6f)
                lineTo(15.0f, 7.6f)
                lineTo(15.0f, 11.8f)
                lineTo(9.0f, 11.8f)
                close()
            }
            // Rim
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.6f, 14.0f); lineTo(16.4f, 14.0f)
            }
            // Net silhouette + mesh
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.2f, 14.0f)
                lineTo(9.6f, 20.4f)
                lineTo(14.4f, 20.4f)
                lineTo(15.8f, 14.0f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12.0f, 14.0f); lineTo(12.0f, 20.4f)
                moveTo(8.8f, 17.0f); lineTo(15.0f, 19.2f)
                moveTo(15.2f, 17.0f); lineTo(9.0f, 19.2f)
            }
        }.build()
    }

    val FootballGoalIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportFootball",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Front goalposts & crossbar
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.2f, 18.2f)
                lineTo(3.2f, 6.8f)
                lineTo(20.8f, 6.8f)
                lineTo(20.8f, 18.2f)
            }
            // Back depth frame
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3.2f, 6.8f); lineTo(5.8f, 4.6f); lineTo(18.2f, 4.6f); lineTo(20.8f, 6.8f)
                moveTo(5.8f, 4.6f); lineTo(5.8f, 15.6f); lineTo(18.2f, 15.6f); lineTo(18.2f, 4.6f)
                moveTo(3.2f, 18.2f); lineTo(5.8f, 15.6f)
                moveTo(20.8f, 18.2f); lineTo(18.2f, 15.6f)
            }
            // Net mesh
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.15f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.8f, 9.8f); lineTo(18.2f, 9.8f)
                moveTo(5.8f, 12.7f); lineTo(18.2f, 12.7f)
                moveTo(9.8f, 4.6f); lineTo(9.8f, 15.6f)
                moveTo(14.2f, 4.6f); lineTo(14.2f, 15.6f)
            }
            // Small ball at goal line
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f
            ) {
                moveTo(12.0f, 16.4f)
                curveTo(13.22f, 16.4f, 14.2f, 17.38f, 14.2f, 18.6f)
                curveTo(14.2f, 19.82f, 13.22f, 20.8f, 12.0f, 20.8f)
                curveTo(10.78f, 20.8f, 9.8f, 19.82f, 9.8f, 18.6f)
                curveTo(9.8f, 17.38f, 10.78f, 16.4f, 12.0f, 16.4f)
                close()
            }
        }.build()
    }

    val CyclingBikeIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportCycling",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Rear wheel
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f
            ) {
                moveTo(5.8f, 12.2f)
                curveTo(7.79f, 12.2f, 9.4f, 13.81f, 9.4f, 15.8f)
                curveTo(9.4f, 17.79f, 7.79f, 19.4f, 5.8f, 19.4f)
                curveTo(3.81f, 19.4f, 2.2f, 17.79f, 2.2f, 15.8f)
                curveTo(2.2f, 13.81f, 3.81f, 12.2f, 5.8f, 12.2f)
                close()
            }
            // Front wheel
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f
            ) {
                moveTo(18.2f, 12.2f)
                curveTo(20.19f, 12.2f, 21.8f, 13.81f, 21.8f, 15.8f)
                curveTo(21.8f, 17.79f, 20.19f, 19.4f, 18.2f, 19.4f)
                curveTo(16.21f, 19.4f, 14.6f, 17.79f, 14.6f, 15.8f)
                curveTo(14.6f, 13.81f, 16.21f, 12.2f, 18.2f, 12.2f)
                close()
            }
            // Frame & handlebar
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.8f, 15.8f); lineTo(11.2f, 15.8f); lineTo(15.2f, 9.8f); lineTo(9.0f, 9.8f); close()
                moveTo(11.2f, 15.8f); lineTo(8.4f, 7.6f)
                moveTo(6.8f, 7.6f); lineTo(9.8f, 7.6f)
                moveTo(18.2f, 15.8f); lineTo(14.8f, 7.4f); lineTo(17.2f, 7.4f)
            }
        }.build()
    }

    val SwimmingWaveIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportSwimming",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Swimmer head
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(16.4f, 5.4f)
                curveTo(17.4f, 5.4f, 18.2f, 6.2f, 18.2f, 7.2f)
                curveTo(18.2f, 8.2f, 17.4f, 9.0f, 16.4f, 9.0f)
                curveTo(15.4f, 9.0f, 14.6f, 8.2f, 14.6f, 7.2f)
                curveTo(14.6f, 6.2f, 15.4f, 5.4f, 16.4f, 5.4f)
                close()
            }
            // Stroke arm
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.2f, 12.0f)
                lineTo(11.4f, 8.0f)
                lineTo(14.4f, 11.2f)
            }
            // Water waves
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.0f, 15.2f)
                curveTo(4.5f, 13.8f, 6.5f, 13.8f, 8.0f, 15.2f)
                curveTo(9.5f, 16.6f, 11.5f, 16.6f, 13.0f, 15.2f)
                curveTo(14.5f, 13.8f, 16.5f, 13.8f, 18.0f, 15.2f)
                curveTo(19.5f, 16.6f, 20.5f, 16.0f, 21.2f, 15.2f)
                moveTo(3.0f, 18.8f)
                curveTo(4.5f, 17.4f, 6.5f, 17.4f, 8.0f, 18.8f)
                curveTo(9.5f, 20.2f, 11.5f, 20.2f, 13.0f, 18.8f)
                curveTo(14.5f, 17.4f, 16.5f, 17.4f, 18.0f, 18.8f)
                curveTo(19.5f, 20.2f, 20.5f, 19.6f, 21.2f, 18.8f)
            }
        }.build()
    }

    val WalkingIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportWalking",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(13.2f, 3.6f)
                curveTo(14.2f, 3.6f, 15.0f, 4.4f, 15.0f, 5.4f)
                curveTo(15.0f, 6.4f, 14.2f, 7.2f, 13.2f, 7.2f)
                curveTo(12.2f, 7.2f, 11.4f, 6.4f, 11.4f, 5.4f)
                curveTo(11.4f, 4.4f, 12.2f, 3.6f, 13.2f, 3.6f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.6f, 8.8f); lineTo(11.2f, 14.0f); lineTo(14.8f, 20.2f)
                moveTo(11.2f, 14.0f); lineTo(8.4f, 20.2f)
                moveTo(8.5f, 12.4f); lineTo(12.4f, 9.2f); lineTo(15.6f, 12.4f)
            }
        }.build()
    }

    val YogaLotusIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportYoga",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(12.0f, 4.2f)
                curveTo(13.0f, 4.2f, 13.8f, 5.0f, 13.8f, 6.0f)
                curveTo(13.8f, 7.0f, 13.0f, 7.8f, 12.0f, 7.8f)
                curveTo(11.0f, 7.8f, 10.2f, 7.0f, 10.2f, 6.0f)
                curveTo(10.2f, 5.0f, 11.0f, 4.2f, 12.0f, 4.2f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.0f, 9.2f); lineTo(12.0f, 14.8f)
                moveTo(6.8f, 13.4f); lineTo(12.0f, 10.4f); lineTo(17.2f, 13.4f)
                moveTo(5.4f, 18.2f)
                curveTo(7.4f, 16.0f, 9.6f, 15.2f, 12.0f, 15.2f)
                curveTo(14.4f, 15.2f, 16.6f, 16.0f, 18.6f, 18.2f)
                lineTo(5.4f, 18.2f)
                close()
            }
        }.build()
    }

    val VolleyballIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportVolleyball",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer ball circle (center 12,12 radius 8.8)
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(12.0f, 3.2f)
                curveTo(16.86f, 3.2f, 20.8f, 7.14f, 20.8f, 12.0f)
                curveTo(20.8f, 16.86f, 16.86f, 20.8f, 12.0f, 20.8f)
                curveTo(7.14f, 20.8f, 3.2f, 16.86f, 3.2f, 12.0f)
                curveTo(3.2f, 7.14f, 7.14f, 3.2f, 12.0f, 3.2f)
                close()
            }
            // Three-panel curved seams
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12.0f, 12.0f)
                curveTo(12.0f, 8.2f, 10.4f, 5.2f, 8.0f, 4.2f)
                moveTo(12.0f, 12.0f)
                curveTo(15.2f, 13.8f, 17.8f, 14.0f, 20.2f, 14.6f)
                moveTo(12.0f, 12.0f)
                curveTo(8.8f, 14.0f, 6.4f, 16.4f, 5.8f, 18.2f)
                moveTo(15.6f, 4.0f)
                curveTo(16.0f, 7.2f, 15.0f, 10.2f, 12.0f, 12.0f)
                moveTo(4.0f, 10.2f)
                curveTo(7.0f, 9.6f, 9.8f, 10.2f, 12.0f, 12.0f)
                moveTo(13.2f, 20.6f)
                curveTo(14.0f, 17.6f, 13.6f, 14.6f, 12.0f, 12.0f)
            }
        }.build()
    }

    val BoxingGloveIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportBoxing",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Glove main body
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.2f, 15.6f)
                lineTo(6.0f, 15.6f)
                curveTo(4.4f, 15.6f, 3.6f, 14.4f, 3.6f, 12.8f)
                curveTo(3.6f, 11.4f, 4.6f, 10.4f, 6.0f, 10.4f)
                lineTo(6.6f, 10.4f)
                curveTo(6.2f, 8.6f, 6.2f, 6.6f, 7.6f, 5.0f)
                curveTo(9.0f, 3.6f, 11.4f, 3.4f, 14.2f, 3.8f)
                curveTo(17.8f, 4.2f, 20.2f, 6.4f, 20.2f, 9.8f)
                curveTo(20.2f, 12.8f, 18.6f, 15.0f, 16.8f, 16.0f)
                lineTo(8.2f, 16.0f)
                close()
            }
            // Wrist cuff
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.0f, 16.0f)
                lineTo(8.0f, 19.6f)
                curveTo(8.0f, 20.3f, 8.6f, 20.8f, 9.3f, 20.8f)
                lineTo(15.7f, 20.8f)
                curveTo(16.4f, 20.8f, 17.0f, 20.3f, 17.0f, 19.6f)
                lineTo(17.0f, 16.0f)
            }
            // Thumb separation crease
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6.6f, 10.4f)
                curveTo(8.2f, 10.4f, 9.2f, 11.6f, 9.2f, 13.0f)
                moveTo(10.6f, 18.4f)
                lineTo(14.4f, 18.4f)
            }
        }.build()
    }

    val HiitLightningIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportHiit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Dynamic lightning bolt
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13.4f, 2.8f)
                lineTo(5.4f, 13.2f)
                lineTo(11.4f, 13.2f)
                lineTo(10.4f, 21.2f)
                lineTo(18.6f, 10.6f)
                lineTo(12.6f, 10.6f)
                close()
            }
        }.build()
    }

    val CalisthenicsBarIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportCalisthenics",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Top horizontal pull-up bar
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3.2f, 5.2f)
                lineTo(20.8f, 5.2f)
            }
            // Hanging straps
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7.4f, 5.2f); lineTo(7.4f, 12.6f)
                moveTo(16.6f, 5.2f); lineTo(16.6f, 12.6f)
            }
            // Left gymnastic ring
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(7.4f, 12.6f)
                curveTo(9.11f, 12.6f, 10.5f, 13.99f, 10.5f, 15.7f)
                curveTo(10.5f, 17.41f, 9.11f, 18.8f, 7.4f, 18.8f)
                curveTo(5.69f, 18.8f, 4.3f, 17.41f, 4.3f, 15.7f)
                curveTo(4.3f, 13.99f, 5.69f, 12.6f, 7.4f, 12.6f)
                close()
            }
            // Right gymnastic ring
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(16.6f, 12.6f)
                curveTo(18.31f, 12.6f, 19.7f, 13.99f, 19.7f, 15.7f)
                curveTo(19.7f, 17.41f, 18.31f, 18.8f, 16.6f, 18.8f)
                curveTo(14.89f, 18.8f, 13.5f, 17.41f, 13.5f, 15.7f)
                curveTo(13.5f, 13.99f, 14.89f, 12.6f, 16.6f, 12.6f)
                close()
            }
        }.build()
    }

    val DanceSparkIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SportDance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f
            ) {
                moveTo(12.2f, 3.6f)
                curveTo(13.2f, 3.6f, 14.0f, 4.4f, 14.0f, 5.4f)
                curveTo(14.0f, 6.4f, 13.2f, 7.2f, 12.2f, 7.2f)
                curveTo(11.2f, 7.2f, 10.4f, 6.4f, 10.4f, 5.4f)
                curveTo(10.4f, 4.4f, 11.2f, 3.6f, 12.2f, 3.6f)
                close()
            }
            // Expressive raised arms & dynamic dance pose
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.4f, 6.6f)
                lineTo(9.2f, 9.8f)
                lineTo(14.2f, 9.6f)
                lineTo(18.6f, 5.8f)
                moveTo(11.8f, 9.8f)
                lineTo(11.4f, 14.8f)
                lineTo(7.4f, 20.2f)
                moveTo(11.4f, 14.8f)
                lineTo(16.2f, 17.4f)
                lineTo(15.2f, 20.6f)
            }
            // Rhythm wave accent
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18.8f, 11.2f); lineTo(20.6f, 10.0f)
                moveTo(19.2f, 14.0f); lineTo(21.2f, 14.0f)
            }
        }.build()
    }
}

fun SportType.iconVector(): ImageVector = when (this) {
    SportType.TENNIS -> SportIcons.TennisIcon
    SportType.FITNESS -> SportIcons.FitnessDumbbellIcon
    SportType.RUNNING -> SportIcons.RunningAthleteIcon
    SportType.ROWING -> SportIcons.RowingMachineIcon
    SportType.BASKETBALL -> SportIcons.BasketballHoopIcon
    SportType.FOOTBALL -> SportIcons.FootballGoalIcon
    SportType.CYCLING -> SportIcons.CyclingBikeIcon
    SportType.SWIMMING -> SportIcons.SwimmingWaveIcon
    SportType.WALKING -> SportIcons.WalkingIcon
    SportType.YOGA_PILATES -> SportIcons.YogaLotusIcon
    SportType.VOLLEYBALL -> SportIcons.VolleyballIcon
    SportType.BOXING -> SportIcons.BoxingGloveIcon
    SportType.HIIT -> SportIcons.HiitLightningIcon
    SportType.CALISTHENICS -> SportIcons.CalisthenicsBarIcon
    SportType.DANCE -> SportIcons.DanceSparkIcon
    SportType.OTHER -> SportIcons.RunningAthleteIcon
}

fun SportType.accentColor(): Color = when (this) {
    SportType.TENNIS -> Color(0xFF10B981)
    SportType.FITNESS -> Color(0xFFF59E0B)
    SportType.RUNNING -> Color(0xFFFB7185)
    SportType.ROWING -> Color(0xFF38BDF8)
    SportType.BASKETBALL -> Color(0xFFF97316)
    SportType.FOOTBALL -> Color(0xFF22C55E)
    SportType.CYCLING -> HealthColors.Primary
    SportType.SWIMMING -> HealthColors.NutrientVitD
    SportType.WALKING -> HealthColors.NutrientFiber
    SportType.YOGA_PILATES -> HealthColors.SecondaryAccent
    SportType.VOLLEYBALL -> Color(0xFFEC4899)
    SportType.BOXING -> Color(0xFFEF4444)
    SportType.HIIT -> Color(0xFFFACC15)
    SportType.CALISTHENICS -> Color(0xFF6366F1)
    SportType.DANCE -> Color(0xFFF43F5E)
    SportType.OTHER -> HealthColors.Tertiary
}

fun BadgeId.sportTypeOrNull(): SportType? = toSportTypeOrNull()
