package minmul.memoir.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import minmul.memoir.core.design.R

@OptIn(ExperimentalTextApi::class)
private val NotoSansKr = FontFamily(
    Font(
        resId = R.font.noto_sans_kr_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.noto_sans_kr_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.noto_sans_kr_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

val Typography = Typography(
    displayLarge = textStyle(FontWeight.Bold, 57, 64),
    displayMedium = textStyle(FontWeight.Bold, 45, 52),
    displaySmall = textStyle(FontWeight.Bold, 36, 44),
    headlineLarge = textStyle(FontWeight.Bold, 32, 40),
    headlineMedium = textStyle(FontWeight.Bold, 28, 36),
    headlineSmall = textStyle(FontWeight.Bold, 24, 32),
    titleLarge = textStyle(FontWeight.Bold, 22, 28),
    titleMedium = textStyle(FontWeight.Medium, 16, 24),
    titleSmall = textStyle(FontWeight.Medium, 14, 20),
    bodyLarge = textStyle(FontWeight.Normal, 16, 24),
    bodyMedium = textStyle(FontWeight.Normal, 14, 20),
    bodySmall = textStyle(FontWeight.Normal, 12, 16),
    labelLarge = textStyle(FontWeight.Medium, 14, 20),
    labelMedium = textStyle(FontWeight.Medium, 12, 16),
    labelSmall = textStyle(FontWeight.Medium, 11, 16),
)

private fun textStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
) = TextStyle(
    fontFamily = NotoSansKr,
    fontWeight = fontWeight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)
