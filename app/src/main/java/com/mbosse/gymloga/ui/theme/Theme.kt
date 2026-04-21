package com.mbosse.gymloga.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Bg = Color(0xFF111114)
val Surface = Color(0xFF1C1C22)
val SurfaceHi = Color(0xFF26262E)
val Border = Color(0xFF333340)
val Text = Color(0xFFE2E0DB)
val TextDim = Color(0xFF7A7870)
val Accent = Color(0xFFC9983A)
val Red = Color(0xFFB8483A)
val Green = Color(0xFF4D8F5F)
val Blue = Color(0xFF5080AA)

val GymTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
        color = Text
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 2.sp,
        color = Accent
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.sp,
        color = TextDim
    )
)
