package io.github.nemynew.expressiveweather.ui.components

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.LocalAppLanguage
import io.github.nemynew.expressiveweather.data.LocalAppStrings
import io.github.nemynew.expressiveweather.data.localizedName

/**
 * タブレットおよび大画面デバイス専用の折りたたみサイドレール (幅 76dp)
 * 常時表示され、メイン画面のサイズを変更することなく安定した幅を維持します。
 * 全配色が端末の MaterialTheme.colorScheme に完全連動します。
 */
@Composable
fun TabletNavigationRail(
    onExpandClick: () -> Unit,
    onAddLocationClick: () -> Unit = onExpandClick,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val railBgColor = colorScheme.surfaceContainer
    val buttonBgColor = colorScheme.surfaceContainerHighest
    val iconColor = colorScheme.onSurface
    val strings = LocalAppStrings.current

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
            // 上部: ハンバーガーボタン (≡) + 地名検索・追加ボタン (📍+)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = buttonBgColor,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
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

                // 2つ目のアイコン：地名追加・検索ボタン (📍+)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = buttonBgColor,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onAddLocationClick
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AddLocationAlt,
                            contentDescription = strings.addLocation,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 下部: 設定ボタン (⚙️)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = buttonBgColor,
                modifier = Modifier
                    .size(48.dp)
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
    recentSearches: List<City> = emptyList(),
    searchResults: List<City>,
    isSearching: Boolean,
    onCitySelected: (City, Boolean) -> Unit,
    onSaveCity: (City) -> Unit,
    onRemoveSavedCity: (City) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    initialFocusSearch: Boolean = false,
    onInitialFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val language = LocalAppLanguage.current
    val strings = LocalAppStrings.current
    val focusManager = LocalFocusManager.current
    val colorScheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(initialFocusSearch) {
        if (initialFocusSearch) {
            searchFocusRequester.requestFocus()
            onInitialFocusHandled()
        }
    }

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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
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
                        items(searchResults, key = { "search_${it.id}_${it.latitude}_${it.longitude}" }) { resultCity ->
                            val isResultSaved = savedCities.any {
                                it.id == resultCity.id || (it.latitude == resultCity.latitude && it.longitude == resultCity.longitude)
                            }
                            CityPillCard(
                                city = resultCity,
                                isSelected = resultCity.id == currentCity.id,
                                isDefault = false,
                                isSaved = isResultSaved,
                                onSaveToggle = {
                                    if (isResultSaved) {
                                        onRemoveSavedCity(resultCity)
                                    } else {
                                        onSaveCity(resultCity)
                                    }
                                },
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
                    // 通常表示 (現在地 + 現在表示中の未保存都市 + 登録された地名 + 最近の検索)
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 60.dp) // 右下設定ボタン用の余白
                    ) {
                        // 1. 現在地カード: 「📍 現在地(東京都) 日本」
                        item(key = "default_location") {
                            val isDefaultSelected = currentCity.id == defaultCity.id ||
                                    (currentCity.latitude == defaultCity.latitude && currentCity.longitude == defaultCity.longitude) ||
                                    currentCity.id == "current"
                            CityPillCard(
                                city = defaultCity,
                                isSelected = isDefaultSelected,
                                isDefault = true,
                                onClick = {
                                    onCitySelected(defaultCity, false)
                                }
                            )
                        }

                        // 2. 現在表示中の都市が未保存の場合、ここに明示表示して登録可能にする
                        val isCurrentDefault = currentCity.id == defaultCity.id ||
                                (currentCity.latitude == defaultCity.latitude && currentCity.longitude == defaultCity.longitude) ||
                                currentCity.id == "current"
                        val isCurrentSaved = savedCities.any {
                            it.id == currentCity.id || (it.latitude == currentCity.latitude && it.longitude == currentCity.longitude)
                        }
                        if (!isCurrentDefault && !isCurrentSaved) {
                            item(key = "currently_viewing_unsaved") {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = strings.currentlyViewing,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = sectionHeaderColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    CityPillCard(
                                        city = currentCity,
                                        isSelected = true,
                                        isDefault = false,
                                        isSaved = false,
                                        onSaveToggle = { onSaveCity(currentCity) },
                                        onClick = {}
                                    )
                                }
                            }
                        }

                        // 3. セクション見出し: 「登録された地名」 + 「＋」地名追加ボタン
                        item(key = "saved_locations_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.savedLocations,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = sectionHeaderColor
                                )

                                // 地名を追加ボタン (＋)
                                Surface(
                                    shape = CircleShape,
                                    color = actionButtonBgColor,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { searchFocusRequester.requestFocus() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = strings.addLocation,
                                            tint = actionButtonIconColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 4. 登録地名リスト (空の場合は「＋ 地名を追加」誘導カード)
                        if (savedCities.isEmpty()) {
                            item(key = "no_saved_locations") {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable { searchFocusRequester.requestFocus() }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddLocationAlt,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = strings.addLocation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            items(savedCities, key = { "saved_${it.id}_${it.latitude}_${it.longitude}" }) { savedCity ->
                                val isSelected = savedCity.id == currentCity.id ||
                                        (savedCity.latitude == currentCity.latitude && savedCity.longitude == currentCity.longitude)
                                CityPillCard(
                                    city = savedCity,
                                    isSelected = isSelected,
                                    isDefault = false,
                                    onClick = { onCitySelected(savedCity, false) },
                                    onDelete = { onRemoveSavedCity(savedCity) }
                                )
                            }
                        }

                        // 5. 最近検索した項目 (Recent Searches)
                        if (recentSearches.isNotEmpty()) {
                            item(key = "recent_searches_header") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = strings.recentSearches,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = sectionHeaderColor,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                                )
                            }
                            items(recentSearches.take(3), key = { "recent_${it.id}" }) { recentCity ->
                                val isRecentSaved = savedCities.any {
                                    it.id == recentCity.id || (it.latitude == recentCity.latitude && it.longitude == recentCity.longitude)
                                }
                                val isSelected = recentCity.id == currentCity.id ||
                                        (recentCity.latitude == currentCity.latitude && recentCity.longitude == currentCity.longitude)
                                CityPillCard(
                                    city = recentCity,
                                    isSelected = isSelected,
                                    isDefault = false,
                                    isSaved = isRecentSaved,
                                    onSaveToggle = {
                                        if (isRecentSaved) {
                                            onRemoveSavedCity(recentCity)
                                        } else {
                                            onSaveCity(recentCity)
                                        }
                                    },
                                    onClick = { onCitySelected(recentCity, false) }
                                )
                            }
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
    isSaved: Boolean = false,
    onSaveToggle: (() -> Unit)? = null,
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

            // 右側アクション: 登録/解除トグル (+ / ✓)
            if (onSaveToggle != null) {
                Surface(
                    shape = CircleShape,
                    color = if (isSaved) colorScheme.surfaceContainerHighest else colorScheme.primary,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onSaveToggle)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = if (isSaved) strings.removeSaved else strings.addLocation,
                            tint = if (isSaved) contentTextColor else colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (onDelete != null && !isDefault) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = strings.removeSaved,
                        tint = contentTextColor.copy(alpha = 0.65f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            } else if (isSelected && !isDefault) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = strings.selected,
                    tint = contentTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
