package com.example.expressiveweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expressiveweather.theme.ExpressiveWeatherTheme

/**
 * キキクル警戒レベルカード（レベル5〜レベル2）プレビュー
 */
@Preview(showBackground = true, name = "キキクル警戒レベルカード一覧")
@Composable
fun KikikuruAlertCardsPreview() {
    ExpressiveWeatherTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "キキクル警戒レベルカード",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            // レベル5 災害切迫
            KikikuruAlertCard(
                level = 5,
                title = "警戒レベル5 災害切迫",
                description = "何らかの災害がすでに発生している可能性が極めて高い状況です。命の危険が迫っているため直ちに身の安全を確保してください。"
            )

            // レベル4 危険
            KikikuruAlertCard(
                level = 4,
                title = "警戒レベル4 危険",
                description = "危険な場所からの避難が必要とされる警戒レベル4に相当します。自治体からの避難指示の発令に留意するとともに、自ら避難の判断をしてください。"
            )

            // レベル3 警戒
            KikikuruAlertCard(
                level = 3,
                title = "警戒レベル3 警戒",
                description = "高齢者等は危険な場所からの避難が必要とされる警戒レベル3に相当します。普段の行動を見合わせ始めたり、避難の準備をしてください。"
            )

            // レベル2 注意
            KikikuruAlertCard(
                level = 2,
                title = "警戒レベル2 注意",
                description = "避難行動の確認が必要とされる警戒レベル2に相当します。ハザードマップ等により災害が想定されている区域や避難先、避難経路を確認してください。"
            )
        }
    }
}

/**
 * 警報・注意報カード（国内警報・国外警報・注意報）プレビュー
 */
@Preview(showBackground = true, name = "警報・注意報カード一覧")
@Composable
fun WeatherWarningCardsPreview() {
    ExpressiveWeatherTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "気象警報・注意報カード",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            // 警報 日本国内用 (紫系)
            WeatherWarningCard(
                type = WeatherWarningType.DOMESTIC_WARNING,
                title = "大雨警報・洪水警報",
                description = "大雨および河川の増水・氾濫に警戒してください。土砂災害警戒区域や低い土地にお住まいの方は周囲の状況に十分注意してください。"
            )

            // 警報 日本国外用 (赤系)
            WeatherWarningCard(
                type = WeatherWarningType.FOREIGN_WARNING,
                title = "Severe Weather Warning",
                description = "Flash flooding and dangerous conditions are expected. Avoid travel and take necessary precautions immediately."
            )

            // 注意報 (黄系)
            WeatherWarningCard(
                type = WeatherWarningType.ADVISORY,
                title = "雷注意報・強風注意報",
                description = "急な強い雨や落雷、突風に注意してください。外出時は最新の気象情報を確認してください。"
            )
        }
    }
}

/**
 * 全デザインカード統合プレビュー
 */
@Preview(showBackground = true, name = "気象警報・キキクル警戒レベル全カード")
@Composable
fun AllWeatherAlertCardsPreview() {
    ExpressiveWeatherTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "◆ キキクル警戒レベルカード",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            KikikuruAlertCard(
                level = 5,
                title = "警戒レベル5 災害切迫",
                description = "何らかの災害がすでに発生している可能性が極めて高い状況です。命の危険が迫っているため直ちに身の安全を確保してください。"
            )

            KikikuruAlertCard(
                level = 4,
                title = "警戒レベル4 危険",
                description = "危険な場所からの避難が必要とされる警戒レベル4に相当します。自治体からの避難指示の発令に留意するとともに、自ら避難の判断をしてください。"
            )

            KikikuruAlertCard(
                level = 3,
                title = "警戒レベル3 警戒",
                description = "高齢者等は危険な場所からの避難が必要とされる警戒レベル3に相当します。普段の行動を見合わせ始めたり、避難の準備をしてください。"
            )

            KikikuruAlertCard(
                level = 2,
                title = "警戒レベル2 注意",
                description = "避難行動の確認が必要とされる警戒レベル2に相当します。ハザードマップ等により避難先や経路を確認してください。"
            )

            Text(
                text = "◆ 警報・注意報カード",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.padding(top = 10.dp)
            )

            WeatherWarningCard(
                type = WeatherWarningType.DOMESTIC_WARNING,
                title = "大雨警報・洪水警報",
                description = "大雨および河川の増水・氾濫に警戒してください。"
            )

            WeatherWarningCard(
                type = WeatherWarningType.FOREIGN_WARNING,
                title = "Severe Weather Warning",
                description = "Flash flooding and dangerous conditions are expected."
            )

            WeatherWarningCard(
                type = WeatherWarningType.ADVISORY,
                title = "雷注意報・強風注意報",
                description = "急な強い雨や落雷、突風に注意してください。"
            )
        }
    }
}
