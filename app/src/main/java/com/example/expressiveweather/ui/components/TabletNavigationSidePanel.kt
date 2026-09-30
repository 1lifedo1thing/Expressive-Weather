package com.example.expressiveweather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.data.AppLanguage
import com.example.expressiveweather.data.City
import com.example.expressiveweather.data.LocalAppLanguage
import com.example.expressiveweather.data.LocalAppStrings
import com.example.expressiveweather.data.localizedName

/**
 * タブレットおよび大画面デバイス専用の折りたたみサイドレール (幅 76dp)
 * 常時表示され、メイン画面のサイズを変更することなく安定した幅を維持します。
 * 全配色が端末の MaterialTheme.colorScheme に完全連動します。
 */
@Composable
fun TabletNavigationRail(
    onExpandClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val railBgColor = colorScheme.surfaceContainer
    val buttonBgColor = colorScheme.surfaceContainerHighest
    val iconColor = colorScheme.onSurface

    Surface(
        modifier = modifier
            .width(76.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        color = railBgColor,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部: ハンバーガーボタン (≡)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = buttonBgColor,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onExpandClick
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Expand side panel",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 下部: 設定ボタン (⚙️)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = buttonBgColor,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettingsClick
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * タブレットおよび大画面デバイス専用の展開ナビゲーションドロワー (幅 320dp)
 * GPU レイヤーによるスムーズなスライドオーバーレイで出現します。
 * 全配色が端末の MaterialTheme.colorScheme に完全連動します。
 */
@Composable
fun TabletNavigationDrawer(
    onCollapseClick: () -> Unit,
    currentCity: City,
    defaultCity: City,
    savedCities: List<City>,
    searchResults: List<City>,
    isSearching: Boolean,
    onCitySelected: (City, Boolean) -> Unit,
    onRemoveSavedCity: (City) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }

    val drawerBgColor = colorScheme.surfaceContainer
    val cardItemBgColor = colorScheme.secondaryContainer
    val cardItemTextColor = colorScheme.onSecondaryContainer
    val actionButtonBgColor = colorScheme.surfaceContainerHighest
    val actionButtonIconColor = colorScheme.onSurface
    val titleTextColor = colorScheme.onSurface
    val sectionHeaderColor = colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        color = drawerBgColor,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // 1. 上部ヘッダー: 「≡ 都市」
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onCollapseClick)
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Collapse side panel",
                        tint = actionButtonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "都市"
                            AppLanguage.SIMPLIFIED_CHINESE -> "城市"
                            AppLanguage.TRADITIONAL_CHINESE -> "城市"
                            AppLanguage.ENGLISH -> "Cities"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleTextColor,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. ピル型検索バー: 「🔍 都市名を検索」
                Surface(
                    shape = CircleShape,
                    color = cardItemBgColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = cardItemTextColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = strings.searchCityPlaceholder,
                                    style = TextStyle(
                                        fontSize = 14.5.sp,
                                        color = cardItemTextColor.copy(alpha = 0.65f),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    onSearchQueryChange(it)
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.5.sp,
                                    color = cardItemTextColor,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(cardItemTextColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    onSearchQueryChange("")
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Clear",
                                    tint = cardItemTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = cardItemTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 検索中の場合は検索結果を表示
                if (searchQuery.isNotBlank()) {
                    Text(
                        text = when (language) {
                            AppLanguage.JAPANESE -> "検索結果"
                            AppLanguage.SIMPLIFIED_CHINESE -> "搜索结果"
                            AppLanguage.TRADITIONAL_CHINESE -> "搜尋結果"
                            AppLanguage.ENGLISH -> "Search Results"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = sectionHeaderColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(searchResults, key = { it.id }) { resultCity ->
                            CityPillCard(
                                city = resultCity,
                                isSelected = resultCity.id == currentCity.id,
                                isDefault = false,
                                onClick = {
                                    onCitySelected(resultCity, true)
                                    searchQuery = ""
                                    onSearchQueryChange("")
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                } else {
                    // 3. 現在地カード: 「📍 現在地(東京都) 日本」
                    val isDefaultSelected = currentCity.id == defaultCity.id || currentCity.id == "current"
                    CityPillCard(
                        city = defaultCity,
                        isSelected = isDefaultSelected,
                        isDefault = true,
                        onClick = {
                            onCitySelected(defaultCity, false)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. セクション見出し: 「登録された地名」
                    Text(
                        text = strings.savedLocations,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = sectionHeaderColor,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                    )

                    // 5. 登録地名リスト
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 60.dp) // 右下設定ボタン用の余白
                    ) {
                        items(savedCities, key = { it.id }) { savedCity ->
                            val isSelected = savedCity.id == currentCity.id
                            CityPillCard(
                                city = savedCity,
                                isSelected = isSelected,
                                isDefault = false,
                                onClick = { onCitySelected(savedCity, false) },
                                onDelete = { onRemoveSavedCity(savedCity) }
                            )
                        }
                    }
                }
            }

            // 6. 右下: 浮動設定ボタン (⚙️)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = actionButtonBgColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSettingsClick
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = actionButtonIconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * テーマカラーに完全連動するピル型/角丸都市カード
 */
@Composable
private fun CityPillCard(
    city: City,
    isSelected: Boolean,
    isDefault: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val colorScheme = MaterialTheme.colorScheme

    val cityName = if (isDefault) {
        val resolvedName = city.localizedName(language)
        if (resolvedName.isNotEmpty() && !resolvedName.equals(strings.currentLocation, ignoreCase = true)) {
            "${strings.currentLocation}($resolvedName)"
        } else {
            strings.currentLocation
        }
    } else {
        city.localizedName(language)
    }

    val countryName = city.country

    // 選択時はPrimaryContainer、通常時はSecondaryContainerでテーマに完全調和
    val containerColor = if (isSelected) colorScheme.primaryContainer else colorScheme.secondaryContainer
    val contentTextColor = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSecondaryContainer

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // アイコン
            Icon(
                imageVector = if (isDefault) Icons.Rounded.MyLocation else Icons.Rounded.LocationCity,
                contentDescription = null,
                tint = contentTextColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cityName,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (countryName.isNotEmpty()) {
                    Text(
                        text = countryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = contentTextColor.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onDelete != null && !isDefault) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete",
                        tint = contentTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
