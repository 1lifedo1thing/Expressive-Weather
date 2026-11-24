package io.github.nemynew.expressiveweather.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex
import io.github.nemynew.expressiveweather.ui.haptics.ExpressiveHapticPattern
import io.github.nemynew.expressiveweather.ui.haptics.LocalExpressiveHaptics

/**
 * 言語選択画面（LanguageSelectionScreen）:
 * - 添付デザイン通りの円形バックボタン＋「言語を変更」ヘッダー
 * - 「現在の言語」独立した角丸カード（RoundedCornerShape 26.dp）
 * - 「設定可能な言語」Android 15 / Material 3 Expressive Connected Card Group
 * - 設定可能な言語はその言語自身で表記され、翻訳されない（日本語、English、简体中文、繁體中文）
 * - 全画面 Edge-to-Edge 対応
 */
@Composable
fun LanguageSelectionScreen(
    currentLanguage: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    useWindowInsets: Boolean = true
) {
    val colorScheme = MaterialTheme.colorScheme
    val strings = currentLanguage.strings
    val expressHaptics = LocalExpressiveHaptics.current

    val preferredOrder = remember {
        listOf(
            AppLanguage.ENGLISH,
            AppLanguage.SPANISH,
            AppLanguage.FRENCH,
            AppLanguage.PORTUGUESE,
            AppLanguage.KOREAN,
            AppLanguage.SIMPLIFIED_CHINESE,
            AppLanguage.TRADITIONAL_CHINESE,
            AppLanguage.JAPANESE
        )
    }
    // 現在選択中以外の設定可能な言語一覧（モックアップに準拠した並び順）
    val availableLanguages = remember(currentLanguage) {
        preferredOrder.filter { it != currentLanguage }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .let { if (useWindowInsets) it.statusBarsPadding() else it }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp)
                .let { if (useWindowInsets) it.navigationBarsPadding() else it }
        ) {
            // ================= 1. トップバー: 円形戻るボタン + タイトル「言語を変更」 =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 円形バックボタン（タップ領域を確保しつつソフトな背景）
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(),
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(18.dp))

                Text(
                    text = strings.changeLanguageTitle,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colorScheme.onSurface
                )
            }

            // ================= 2. 「現在の言語」セクション =================
            Text(
                text = strings.currentLanguageSection,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // 現在選択されている言語のカード
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = colorScheme.surfaceContainer,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = currentLanguage.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ================= 3. 「設定可能な言語」セクション =================
            Text(
                text = strings.availableLanguagesSection,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            // 設定可能な言語一覧（Connected Cards: 上端・中間・下端で角丸が連続するグループ）
            Android15CardGroup {
                availableLanguages.forEachIndexed { index, lang ->
                    val position = when {
                        availableLanguages.size == 1 -> ConnectedCardPosition.Single
                        index == 0 -> ConnectedCardPosition.Top
                        index == availableLanguages.lastIndex -> ConnectedCardPosition.Bottom
                        else -> ConnectedCardPosition.Middle
                    }
                    val shape = position.toShape()

                    Surface(
                        shape = shape,
                        color = colorScheme.surfaceContainer,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = {
                                    expressHaptics.play(ExpressiveHapticPattern.SWITCH_ON)
                                    onSelectLanguage(lang)
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 20.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 18.sp,
                                color = colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
