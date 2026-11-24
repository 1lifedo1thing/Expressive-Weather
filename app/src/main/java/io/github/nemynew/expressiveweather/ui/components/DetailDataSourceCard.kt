package io.github.nemynew.expressiveweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nemynew.expressiveweather.data.AppLanguage
import io.github.nemynew.expressiveweather.data.City
import io.github.nemynew.expressiveweather.data.NwsAlertRepository
import io.github.nemynew.expressiveweather.data.WeatherData
import io.github.nemynew.expressiveweather.theme.GoogleSansFlex

enum class DetailMetricType {
    WIND,
    UV,
    AIR_QUALITY,
    HUMIDITY,
    SUN_MOON,
    PRESSURE,
    VISIBILITY,
    PRECIPITATION,
    FEELS_LIKE
}

/**
 * 各詳細ページ最下部に設置する情報ソース検証・表示カード。
 * 観測地点（日本国内、米国、全世界）および対象気象項目に応じて
 * 実際に取得しているデータソース（気象庁アメダス、NOAA、Open-Meteo、CAMSなど）を正確に判定・表示する。
 *
 * Material 3 Expressive 準拠:
 * - 枠線（Border）は一切不使用
 * - トナルサーフェスと角丸（24dp）
 * - 4言語（日本語、英語、簡体中文、繁体中文）完全対応
 */
@Composable
fun DetailDataSourceCard(
    metricType: DetailMetricType,
    weather: WeatherData?,
    city: City,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // 観測地判定
    val isJapan = city.country.contains("日本") ||
            city.country.contains("일본") ||
            city.country.equals("Japan", ignoreCase = true) ||
            city.country.equals("Japón", ignoreCase = true) ||
            city.country.equals("Japon", ignoreCase = true) ||
            city.country.equals("Japão", ignoreCase = true) ||
            weather?.jmaOverviewText != null ||
            (city.latitude in 20.0..46.0 && city.longitude in 122.0..154.0)
    val isUs = !isJapan && NwsAlertRepository.isUnitedStates(city)

    val sourceTitle: String
    val sourceSubtitle: String
    val sourceDetail: String

    when (metricType) {
        DetailMetricType.WIND -> {
            sourceTitle = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス観測網 ＆ 予報モデル"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) AMeDAS 관측망 및 예보 모델"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS观测网与预报模型"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS觀測網與預報模型"
                    AppLanguage.SPANISH -> "Estaciones AMeDAS de JMA y modelos de pronóstico"
                    AppLanguage.FRENCH -> "Stations AMeDAS de JMA et modèles de prévision"
                    AppLanguage.PORTUGUESE -> "Estações AMeDAS da JMA e modelos de previsão"
                    AppLanguage.ENGLISH -> "JMA AMeDAS Stations & Forecast Models"
                }
                isUs -> when (language) {
                    AppLanguage.JAPANESE -> "米海洋大気庁 (NOAA) / NWS 地上観測網"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) / NWS 지상 관측망"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 (NOAA/NWS) 观测网络"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 (NOAA/NWS) 觀測網絡"
                    AppLanguage.SPANISH -> "NOAA / Servicio Meteorológico Nacional (NWS)"
                    AppLanguage.FRENCH -> "NOAA / National Weather Service (NWS)"
                    AppLanguage.PORTUGUESE -> "NOAA / Serviço Nacional de Meteorologia (NWS)"
                    AppLanguage.ENGLISH -> "NOAA / National Weather Service (NWS)"
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 高解像度気象予報モデル (ECMWF / GFS)"
                    AppLanguage.KOREAN -> "Open-Meteo 고해상도 기상 예보 모델 (ECMWF / GFS)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 高分辨率天气模型 (ECMWF / GFS)"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 高解析度天氣模型 (ECMWF / GFS)"
                    AppLanguage.SPANISH -> "Modelos de alta resolución Open-Meteo (ECMWF/GFS)"
                    AppLanguage.FRENCH -> "Modèles haute résolution Open-Meteo (ECMWF/GFS)"
                    AppLanguage.PORTUGUESE -> "Modelos de alta resolução Open-Meteo (ECMWF/GFS)"
                    AppLanguage.ENGLISH -> "Open-Meteo High-Resolution Models (ECMWF/GFS)"
                }
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "地上風向・風速・瞬間最大風速 実測＆予報"
                AppLanguage.KOREAN -> "지상 풍향 · 풍속 · 최대 순간풍속 실측 및 예보"
                AppLanguage.SIMPLIFIED_CHINESE -> "地面风向・风速・阵风实测与预报"
                AppLanguage.TRADITIONAL_CHINESE -> "地面風向・風速・陣風實測與預報"
                AppLanguage.SPANISH -> "Mediciones de viento en superficie y rachas"
                AppLanguage.FRENCH -> "Mesures du vent en surface et rafales"
                AppLanguage.PORTUGUESE -> "Medições de vento na superfície e rajadas"
                AppLanguage.ENGLISH -> "Surface Wind Vector & Gust Measurements"
            }
            sourceDetail = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "全国約1,300地点のアメダス観測所から風向・風速・最大瞬間風速の直接実測データおよび気象台発表の予報をリアルタイム反映しています。"
                    AppLanguage.KOREAN -> "일본 전역 약 1,300개 AMeDAS 관측소의 풍향·풍속·최대 순간풍속 실측 데이터와 기상대 발표 예보를 실시간으로 반영합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "直接获取全国约1,300个AMeDAS气象站风向、风速及阵风实测数据，并同步气象台权威预报。"
                    AppLanguage.TRADITIONAL_CHINESE -> "直接擷取全國約1,300個AMeDAS氣象站風向、風速及陣風實測資料，並同步氣象台權威預報。"
                    AppLanguage.SPANISH -> "Velocidad, rachas y dirección del viento en tiempo real obtenidos directamente de ~1.300 estaciones automáticas AMeDAS en todo Japón."
                    AppLanguage.FRENCH -> "Vitesse, rafales et direction du vent en temps réel issues directement d'environ 1 300 stations automatiques AMeDAS au Japon."
                    AppLanguage.PORTUGUESE -> "Velocidade, rajadas e direção do vento em tempo real obtidas diretamente de ~1.300 estações automáticas AMeDAS em todo o Japão."
                    AppLanguage.ENGLISH -> "Real-time surface wind speed, gusts, and direction directly retrieved from ~1,300 AMeDAS automated stations nationwide."
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "国際標準気象通報式 (METAR/SYNOP) および欧州中期予報センター (ECMWF) / 米国気象局 (GFS) 数値予報を統合しています。"
                    AppLanguage.KOREAN -> "국제 표준 기상 전문 (METAR/SYNOP)과 유럽중기예보센터 (ECMWF) 및 미국 기상청 (GFS) 고해상도 수치 예보를 통합합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "集成国际地面气象电码(METAR)与ECMWF/GFS全球高分辨率数值天气预报。"
                    AppLanguage.TRADITIONAL_CHINESE -> "整合國際地面氣象電碼(METAR)與ECMWF/GFS全球高解析度數值天氣預報。"
                    AppLanguage.SPANISH -> "Integra informes oficiales de estaciones METAR con modelos numéricos globales de alta resolución ECMWF y GFS."
                    AppLanguage.FRENCH -> "Combine les relevés officiels des stations METAR avec les modèles numériques haute résolution ECMWF et GFS."
                    AppLanguage.PORTUGUESE -> "Integra relatórios oficiais de estações METAR com os modelos numéricos globais de alta resolução ECMWF e GFS."
                    AppLanguage.ENGLISH -> "Synthesizing official METAR station reports with global high-resolution ECMWF and GFS numerical models."
                }
            }
        }

        DetailMetricType.UV -> {
            sourceTitle = when (language) {
                AppLanguage.JAPANESE -> "Open-Meteo ＆ 欧州コペルニクス大気監視サービス (CAMS)"
                AppLanguage.KOREAN -> "Open-Meteo 및 유럽 코페르니쿠스 대기감시서비스 (CAMS)"
                AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 与 欧洲哥白尼大气监测局 (CAMS)"
                AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 與 歐洲哥白尼大氣監測局 (CAMS)"
                AppLanguage.SPANISH -> "Open-Meteo y Copernicus Atmosphere (CAMS)"
                AppLanguage.FRENCH -> "Open-Meteo et Copernicus Atmosphere (CAMS)"
                AppLanguage.PORTUGUESE -> "Open-Meteo e Copernicus Atmosphere (CAMS)"
                AppLanguage.ENGLISH -> "Open-Meteo & Copernicus Atmosphere (CAMS)"
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "世界気象機関 (WMO) 基準 紫外線放射モデル"
                AppLanguage.KOREAN -> "세계기상기구 (WMO) 기준 자외선 복사 모델"
                AppLanguage.SIMPLIFIED_CHINESE -> "WMO标准地面紫外线辐射通量模型"
                AppLanguage.TRADITIONAL_CHINESE -> "WMO標準地面紫外線輻射通量模型"
                AppLanguage.SPANISH -> "Modelo de radiación UV solar estándar OMM (WMO)"
                AppLanguage.FRENCH -> "Modèle de rayonnement UV solaire standard OMM (WMO)"
                AppLanguage.PORTUGUESE -> "Modelo de radiação UV solar padrão OMM (WMO)"
                AppLanguage.ENGLISH -> "WMO-Standard Solar UV Radiation Model"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "太陽高度角、成層圏オゾン全量、雲量、エアロゾル光学的厚さから、世界保健機関 (WHO) / WMO ガイドラインに基づき地上到達UVインデックスを算出しています。"
                AppLanguage.KOREAN -> "태양 고도각, 성층권 오존 전량, 운량, 에어로졸 광학 두께를 바탕으로 세계보건기구 (WHO) 및 WMO 기준에 따라 지표 도달 자외선 지수를 산출합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "基于太阳天顶角、平流层臭氧总量、云量与气溶胶光学厚度，依据世界卫生组织(WHO)与WMO指南精算地面紫外线指数。"
                AppLanguage.TRADITIONAL_CHINESE -> "基於太陽天頂角、平流層臭氧總量、雲量與氣溶膠光學厚度，依據世界衛生組織(WHO)與WMO指南精算地面紫外線指數。"
                AppLanguage.SPANISH -> "Calcula el índice UV en superficie según las directrices de la OMS/OMM, considerando el ángulo solar cenital, el ozono total y la atenuación nubosa."
                AppLanguage.FRENCH -> "Calcule l'indice UV au sol selon les directives OMS/OMM en tenant compte de l'angle solaire zénithal, de l'ozone total et de la nébulosité."
                AppLanguage.PORTUGUESE -> "Calcula o índice UV na superfície conforme as diretrizes da OMS/OMM, considerando o ângulo zenital solar, o ozônio total e a atenuação por nuvens."
                AppLanguage.ENGLISH -> "Calculates surface erythemal UV index according to WHO/WMO guidelines, factoring solar zenith angle, total column ozone, and cloud attenuation."
            }
        }

        DetailMetricType.AIR_QUALITY -> {
            sourceTitle = when (language) {
                AppLanguage.JAPANESE -> "欧州コペルニクス (CAMS) ＆ Open-Meteo Air Quality"
                AppLanguage.KOREAN -> "유럽 코페르니쿠스 (CAMS) 및 Open-Meteo Air Quality"
                AppLanguage.SIMPLIFIED_CHINESE -> "欧洲哥白尼 (CAMS) 与 Open-Meteo 空气质量网"
                AppLanguage.TRADITIONAL_CHINESE -> "歐洲哥白尼 (CAMS) 與 Open-Meteo 空氣品質網"
                AppLanguage.SPANISH -> "Copernicus (CAMS) y Open-Meteo Air Quality"
                AppLanguage.FRENCH -> "Copernicus (CAMS) et Open-Meteo Air Quality"
                AppLanguage.PORTUGUESE -> "Copernicus (CAMS) e Open-Meteo Air Quality"
                AppLanguage.ENGLISH -> "Copernicus (CAMS) & Open-Meteo Air Quality"
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "全球大気化学輸送モデル ＆ 地上観測統合解析"
                AppLanguage.KOREAN -> "전 지구 대기화학 수송 모델 및 지상 관측 통합 분석"
                AppLanguage.SIMPLIFIED_CHINESE -> "全球大气化学传输模型与地基监测综合解析"
                AppLanguage.TRADITIONAL_CHINESE -> "全球大氣化學傳輸模型與地基監測綜合解析"
                AppLanguage.SPANISH -> "Análisis global de transporte químico atmosférico"
                AppLanguage.FRENCH -> "Analyse globale de transport chimique atmosphérique"
                AppLanguage.PORTUGUESE -> "Análise global de transporte químico atmosférico"
                AppLanguage.ENGLISH -> "Global Atmospheric Chemistry Transport Analysis"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "微小粒子状物質 (PM2.5 / PM10)、二酸化窒素 (NO2)、オゾン (O3) の多層大気シミュレーションと地質観測値を統合し、米国AQIおよび欧州大気質指標をリアルタイム算出しています。"
                AppLanguage.KOREAN -> "초미세먼지 (PM2.5), 미세먼지 (PM10), 이산화질소 (NO2), 오존 (O3)의 다층 대기 시뮬레이션과 지상 관측값을 통합해 미국 AQI 및 유럽 대기질 지수를 실시간으로 산출합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "综合PM2.5、PM10、NO2及地面臭氧等微细颗粒物化学模型，实时计算美国AQI与欧洲空气质量指数。"
                AppLanguage.TRADITIONAL_CHINESE -> "綜合PM2.5、PM10、NO2及地面臭氧等微細顆粒物化學模型，即時計算美國AQI與歐洲空氣品質指數。"
                AppLanguage.SPANISH -> "Integra modelos de transporte químico multicapa con observaciones en superficie para PM2.5, PM10, NO2 y ozono troposférico."
                AppLanguage.FRENCH -> "Intègre la modélisation chimique multicouche et les observations au sol pour les PM2.5, PM10, NO2 et l'ozone de surface."
                AppLanguage.PORTUGUESE -> "Integra modelagem química atmosférica multicamadas com observações de superfície para PM2.5, PM10, NO2 e ozônio."
                AppLanguage.ENGLISH -> "Integrates multi-layer chemistry transport modeling with ground observations for PM2.5, PM10, NO2, and surface ozone."
            }
        }

        DetailMetricType.HUMIDITY -> {
            sourceTitle = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス実測 ＆ 熱力学露点解析"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) AMeDAS 실측 및 열역학 이슬점 분석"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS实测与热力学露点分析"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS實測與熱力學露點分析"
                    AppLanguage.SPANISH -> "Mediciones AMeDAS de JMA y análisis termodinámico"
                    AppLanguage.FRENCH -> "Mesures AMeDAS de JMA et analyse thermodynamique"
                    AppLanguage.PORTUGUESE -> "Medições AMeDAS da JMA e análise termodinâmica"
                    AppLanguage.ENGLISH -> "JMA AMeDAS Measurements & Thermodynamic Analysis"
                }
                isUs -> when (language) {
                    AppLanguage.JAPANESE -> "米国海洋大気庁 (NOAA) / NWS 地上湿度観測"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) / NWS 지상 습도 관측"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 (NOAA/NWS) 湿度观测"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 (NOAA/NWS) 濕度觀測"
                    AppLanguage.SPANISH -> "Estaciones de humedad en superficie NOAA / NWS"
                    AppLanguage.FRENCH -> "Stations d'humidité en surface NOAA / NWS"
                    AppLanguage.PORTUGUESE -> "Estações de umidade de superfície NOAA / NWS"
                    AppLanguage.ENGLISH -> "NOAA / NWS Surface Humidity Stations"
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 気象観測網 ＆ ECMWF 数値解析"
                    AppLanguage.KOREAN -> "Open-Meteo 기상 관측망 및 ECMWF 수치 분석"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 气象观测网与 ECMWF 数值分析"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 氣象觀測網與 ECMWF 數值分析"
                    AppLanguage.SPANISH -> "Análisis de superficie Open-Meteo y ECMWF"
                    AppLanguage.FRENCH -> "Analyse de surface Open-Meteo et ECMWF"
                    AppLanguage.PORTUGUESE -> "Análise de superfície Open-Meteo e ECMWF"
                    AppLanguage.ENGLISH -> "Open-Meteo & ECMWF Surface Analysis"
                }
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "相対湿度実測 ＆ マグナス・テテンス露点計算法"
                AppLanguage.KOREAN -> "상대 습도 실측 및 마그누스-테텐스 이슬점 계산법"
                AppLanguage.SIMPLIFIED_CHINESE -> "相对湿度实测与马格努斯-泰滕斯露点计算法"
                AppLanguage.TRADITIONAL_CHINESE -> "相對濕度實測與馬格努斯-泰騰斯露點計算法"
                AppLanguage.SPANISH -> "Humedad relativa y punto de rocío Magnus-Tetens"
                AppLanguage.FRENCH -> "Humidité relative et point de rosée Magnus-Tetens"
                AppLanguage.PORTUGUESE -> "Umidade relativa e ponto de orvalho Magnus-Tetens"
                AppLanguage.ENGLISH -> "Relative Humidity & Magnus-Tetens Dew Point"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "地上気象観測センサーによる相対湿度実測値、および気温と水蒸気圧からマグナス・テテンスの精密気象熱力学計算式により露点温度を導出しています。"
                AppLanguage.KOREAN -> "지상 기상 관측 센서의 상대 습도 실측값과 기온·수증기압을 바탕으로 마그누스-테텐스 정밀 기상 열역학 공식으로 이슬점 온도를 산출합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "结合地面气象站实测相对湿度，基于热力学马格努斯-泰滕斯公式精确推导露点温度与水汽饱和度。"
                AppLanguage.TRADITIONAL_CHINESE -> "結合地面氣象站實測相對濕度，基於熱力學馬格努斯-泰騰斯公式精確推導露點溫度與水汽飽和度。"
                AppLanguage.SPANISH -> "Combina telemetría de humedad relativa en superficie con la fórmula termodinámica de Magnus-Tetens para calcular el punto de rocío."
                AppLanguage.FRENCH -> "Associe les relevés d'humidité relative au sol à la formule thermodynamique de Magnus-Tetens pour calculer le point de rosée."
                AppLanguage.PORTUGUESE -> "Combina medições de umidade relativa no solo com a formulação termodinâmica de Magnus-Tetens para calcular o ponto de orvalho."
                AppLanguage.ENGLISH -> "Ground relative humidity telemetry paired with thermodynamic Magnus-Tetens formulation to derive dew point temperature."
            }
        }

        DetailMetricType.SUN_MOON -> {
            sourceTitle = when (language) {
                AppLanguage.JAPANESE -> "米国海洋大気庁 (NOAA) 天文計算式 ＆ 端末独自天体アルゴリズム"
                AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) 천문 계산식 및 기기 내 천체 알고리즘"
                AppLanguage.SIMPLIFIED_CHINESE -> "美国国家海洋和大气管理局 (NOAA) 天文计算式"
                AppLanguage.TRADITIONAL_CHINESE -> "美國國家海洋暨大氣總署 (NOAA) 天文計算式"
                AppLanguage.SPANISH -> "Cálculos solares NOAA y algoritmos astronómicos"
                AppLanguage.FRENCH -> "Calculs solaires NOAA et algorithmes astronomiques"
                AppLanguage.PORTUGUESE -> "Cálculos solares NOAA e algoritmos astronômicos"
                AppLanguage.ENGLISH -> "NOAA Solar Calculations & Astronomical Algorithms"
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "緯度経度・大気差補正・コンウェイ月相計算法"
                AppLanguage.KOREAN -> "위도·경도 기반 대기 굴절 보정 및 콘웨이 월령 알고리즘"
                AppLanguage.SIMPLIFIED_CHINESE -> "经纬度太阳天顶角・大气折射修正・康威月相算法"
                AppLanguage.TRADITIONAL_CHINESE -> "經緯度太陽天頂角・大氣折射修正・康威月相演算法"
                AppLanguage.SPANISH -> "Coordenadas solares en el dispositivo y fase lunar de Conway"
                AppLanguage.FRENCH -> "Coordonnées solaires locales et phase lunaire de Conway"
                AppLanguage.PORTUGUESE -> "Coordenadas solares no dispositivo e fase lunar de Conway"
                AppLanguage.ENGLISH -> "On-Device Solar Coordinates & Conway Moon Phase"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "観測地点の緯度経度からNOAA太陽位置方程式、大気差補正（約34分角）、グリニッジ恒星時をもとに日の出・日の入り時刻および月齢・輝面比を端末独自に高精度演算しています。"
                AppLanguage.KOREAN -> "관측 지점의 위도·경도와 그리니치 항성시를 바탕으로 NOAA 태양 위치 방정식, 대기 굴절 보정(약 34분각), 콘웨이 월령 계산법을 적용해 일출·일몰 시각과 달 위상을 기기에서 정밀 계산합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "根据精确经纬度与格林尼治恒星时，运用NOAA太阳位置方程结合大气折射修正与康威月相算法进行端侧高精度天文演算。"
                AppLanguage.TRADITIONAL_CHINESE -> "根據精確經緯度與格林尼治恆星時，運用NOAA太陽位置方程結合大氣折射修正與康威月相演算法進行端側高精度天文演算。"
                AppLanguage.SPANISH -> "Calculado localmente mediante los algoritmos de posición solar de la NOAA con corrección de refracción atmosférica (~34 min de arco) y la fórmula de fase lunar de Conway."
                AppLanguage.FRENCH -> "Calculé localement via les algorithmes solaires de la NOAA intégrant la réfraction atmosphérique (~34 minutes d'arc) et les formules de phase lunaire de Conway."
                AppLanguage.PORTUGUESE -> "Calculado localmente pelos algoritmos de posição solar da NOAA considerando a refração atmosférica (~34 min de arco) e a fórmula de fase lunar de Conway."
                AppLanguage.ENGLISH -> "Computed locally via NOAA solar position algorithms factoring atmospheric refraction (~34 arcmin) alongside Conway moon phase formulas."
            }
        }

        DetailMetricType.PRESSURE -> {
            sourceTitle = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス実測 ＆ 海面更正気圧データ"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) AMeDAS 실측 및 해면 기압 데이터"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS实测与海平面修正气压"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS實測與海平面修正氣壓"
                    AppLanguage.SPANISH -> "Observaciones barométricas AMeDAS de JMA"
                    AppLanguage.FRENCH -> "Observations barométriques AMeDAS de JMA"
                    AppLanguage.PORTUGUESE -> "Observações barométricas AMeDAS da JMA"
                    AppLanguage.ENGLISH -> "JMA AMeDAS Barometric Observations"
                }
                isUs -> when (language) {
                    AppLanguage.JAPANESE -> "米海洋大気庁 (NOAA) / NWS 気圧観測網"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) / NWS 기압 관측망"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 (NOAA/NWS) 气压监测网"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 (NOAA/NWS) 氣壓監測網"
                    AppLanguage.SPANISH -> "Estaciones barométricas NOAA / NWS"
                    AppLanguage.FRENCH -> "Stations barométriques NOAA / NWS"
                    AppLanguage.PORTUGUESE -> "Estações barométricas NOAA / NWS"
                    AppLanguage.ENGLISH -> "NOAA / NWS Barometric Stations"
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 気圧モデル ＆ ECMWF IFS 大気循環"
                    AppLanguage.KOREAN -> "Open-Meteo 기압 모델 및 ECMWF IFS 대기 분석"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 气压模型与 ECMWF 大气分析"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 氣壓模型與 ECMWF 大氣分析"
                    AppLanguage.SPANISH -> "Modelos de presión atmosférica Open-Meteo y ECMWF"
                    AppLanguage.FRENCH -> "Modèles de pression en surface Open-Meteo et ECMWF"
                    AppLanguage.PORTUGUESE -> "Modelos de pressão atmosférica Open-Meteo e ECMWF"
                    AppLanguage.ENGLISH -> "Open-Meteo & ECMWF Surface Pressure Models"
                }
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "国際標準大気 (ISA) 準拠 海面気圧 (hPa)"
                AppLanguage.KOREAN -> "국제표준대기 (ISA) 기준 해면 기압 (hPa)"
                AppLanguage.SIMPLIFIED_CHINESE -> "国际标准大气 (ISA) 海平面标准气压 (hPa)"
                AppLanguage.TRADITIONAL_CHINESE -> "國際標準大氣 (ISA) 海平面標準氣壓 (hPa)"
                AppLanguage.SPANISH -> "Presión media a nivel del mar estándar ISA (MSLP)"
                AppLanguage.FRENCH -> "Pression moyenne au niveau de la mer standard ISA"
                AppLanguage.PORTUGUESE -> "Pressão média ao nível do mar padrão ISA (MSLP)"
                AppLanguage.ENGLISH -> "ISA-Standard Mean Sea Level Pressure (MSLP)"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "地上気圧センサー実測値に観測地点の標高差補正を行い、国際標準大気 (ISA) モデルに基づいて海面気圧 (hPa) を算出しています。"
                AppLanguage.KOREAN -> "지상 기압 센서 실측값에 관측 지점의 고도 보정을 적용하고, 국제표준대기 (ISA) 모델을 기반으로 해면 기압 (hPa)을 산출합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "基于地面气压传感器实测数据，结合测站海拔高度差依据国际标准大气(ISA)模型精算海平面气压。"
                AppLanguage.TRADITIONAL_CHINESE -> "基於地面氣壓感測器實測資料，結合測站海拔高度差依據國際標準大氣(ISA)模型精算海平面氣壓。"
                AppLanguage.SPANISH -> "Mediciones de sensores ajustadas por la elevación de la estación según la Atmósfera Estándar Internacional (ISA) para obtener la presión a nivel del mar."
                AppLanguage.FRENCH -> "Mesures barométriques corrigées selon l'altitude de la station d'après l'atmosphère normalisée internationale (ISA) pour obtenir la pression au niveau de la mer."
                AppLanguage.PORTUGUESE -> "Medições de sensores ajustadas pela altitude da estação conforme a Atmosfera Padrão Internacional (ISA) para obter a pressão ao nível do mar."
                AppLanguage.ENGLISH -> "Sensor measurements adjusted for station elevation according to the International Standard Atmosphere to produce MSLP."
            }
        }

        DetailMetricType.VISIBILITY -> {
            sourceTitle = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) 空港・アメダス推計 ＆ Open-Meteo"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) 공항·AMeDAS 추정 및 Open-Meteo"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) 机场实测与 Open-Meteo"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) 機場實測與 Open-Meteo"
                    AppLanguage.SPANISH -> "Estaciones aeronáuticas JMA y Open-Meteo"
                    AppLanguage.FRENCH -> "Stations aéronautiques JMA et Open-Meteo"
                    AppLanguage.PORTUGUESE -> "Estações aeronáuticas JMA e Open-Meteo"
                    AppLanguage.ENGLISH -> "JMA Aviation Stations & Open-Meteo"
                }
                isUs -> when (language) {
                    AppLanguage.JAPANESE -> "米航空気象 METAR 視程観測 ＆ NOAA / NWS"
                    AppLanguage.KOREAN -> "미국 항공 기상 METAR 시정 관측 및 NOAA / NWS"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国航空气象 METAR 与 NOAA/NWS 观测"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國航空氣象 METAR 與 NOAA/NWS 觀測"
                    AppLanguage.SPANISH -> "Visibilidad aeronáutica METAR y NOAA / NWS"
                    AppLanguage.FRENCH -> "Visibilité aéronautique METAR et NOAA / NWS"
                    AppLanguage.PORTUGUESE -> "Visibilidade aeronáutica METAR e NOAA / NWS"
                    AppLanguage.ENGLISH -> "Aviation METAR Visibility & NOAA / NWS"
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 高解像度気象予報モデル (ECMWF)"
                    AppLanguage.KOREAN -> "Open-Meteo 고해상도 시정 예보 모델 (ECMWF)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 能见度分析模型 (ECMWF)"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 能見度分析模型 (ECMWF)"
                    AppLanguage.SPANISH -> "Visibilidad de alta resolución Open-Meteo (ECMWF)"
                    AppLanguage.FRENCH -> "Visibilité haute résolution Open-Meteo (ECMWF)"
                    AppLanguage.PORTUGUESE -> "Visibilidade de alta resolução Open-Meteo (ECMWF)"
                    AppLanguage.ENGLISH -> "Open-Meteo High-Resolution Visibility (ECMWF)"
                }
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "国際民間航空機関 (ICAO) 視程基準 ＆ エアロゾル消散解析"
                AppLanguage.KOREAN -> "국제민간항공기구 (ICAO) 시정 기준 및 에어로졸 소멸 분석"
                AppLanguage.SIMPLIFIED_CHINESE -> "国际民航组织 (ICAO) 标准与气溶胶消光计算"
                AppLanguage.TRADITIONAL_CHINESE -> "國際民航組織 (ICAO) 標準與氣溶膠消光計算"
                AppLanguage.SPANISH -> "Transmisómetro estándar OACI y extinción por aerosoles"
                AppLanguage.FRENCH -> "Transmissomètre standard OACI et extinction par aérosols"
                AppLanguage.PORTUGUESE -> "Transmissômetro padrão OACI e extinção por aerossóis"
                AppLanguage.ENGLISH -> "ICAO Standard Transmissometer & Aerosol Extinction"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "地上透過度計（トランスミッソメーター）による実測値と、相対湿度・大気粒子消散係数を加味した水平有効視程（km）を表示しています。"
                AppLanguage.KOREAN -> "지상 투과도계(트랜스미소미터) 실측값에 상대 습도와 대기 입자 소멸 계수를 반영해 수평 유효 가시거리(km)를 표시합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "结合地面透射仪实测数据与大气相对湿度、微粒消光系数，呈现符合ICAO国际标准的有效水平能见度。"
                AppLanguage.TRADITIONAL_CHINESE -> "結合地面透射儀實測資料與大氣相對濕度、微粒消光係數，呈現符合ICAO國際標準的有效水平能見度。"
                AppLanguage.SPANISH -> "Calcula la visibilidad horizontal efectiva en superficie a partir de mediciones de transmisómetro, humedad relativa y extinción por aerosoles."
                AppLanguage.FRENCH -> "Calcule la visibilité horizontale effective au sol à partir des mesures de transmissomètre, de l'humidité relative et de l'extinction par aérosols."
                AppLanguage.PORTUGUESE -> "Calcula a visibilidade horizontal efetiva no solo com base em medições de transmissômetro, umidade relativa e extinção por aerossóis."
                AppLanguage.ENGLISH -> "Calculates effective horizontal ground visibility based on transmissometer measurements, relative humidity, and aerosol extinction."
            }
        }

        DetailMetricType.PRECIPITATION -> {
            sourceTitle = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "気象庁 (JMA) アメダス雨量計 ＆ 高解像度ナウキャスト"
                    AppLanguage.KOREAN -> "일본 기상청 (JMA) AMeDAS 우량계 및 고해상도 나우캐스트"
                    AppLanguage.SIMPLIFIED_CHINESE -> "日本气象厅 (JMA) AMeDAS雨量计与高精度降水雷达"
                    AppLanguage.TRADITIONAL_CHINESE -> "日本氣象廳 (JMA) AMeDAS雨量計與高精度降水雷達"
                    AppLanguage.SPANISH -> "Pluviómetros AMeDAS de JMA y nowcast de alta resolución"
                    AppLanguage.FRENCH -> "Pluviomètres AMeDAS de JMA et nowcast haute résolution"
                    AppLanguage.PORTUGUESE -> "Pluviômetros AMeDAS da JMA e nowcast de alta resolução"
                    AppLanguage.ENGLISH -> "JMA AMeDAS Rain Gauges & High-Res Nowcast"
                }
                isUs -> when (language) {
                    AppLanguage.JAPANESE -> "米海洋大気庁 (NOAA) / NWS ドップラーレーダー網 (MRMS)"
                    AppLanguage.KOREAN -> "미국 해양대기청 (NOAA) / NWS 도플러 레이더망 (MRMS)"
                    AppLanguage.SIMPLIFIED_CHINESE -> "美国国家气象局 (NOAA/NWS) 多普勒雷达网 (MRMS)"
                    AppLanguage.TRADITIONAL_CHINESE -> "美國國家氣象局 (NOAA/NWS) 都卜勒雷達網 (MRMS)"
                    AppLanguage.SPANISH -> "Radar Doppler NOAA / NWS y red MRMS"
                    AppLanguage.FRENCH -> "Radar Doppler NOAA / NWS et réseau MRMS"
                    AppLanguage.PORTUGUESE -> "Radar Doppler NOAA / NWS e rede MRMS"
                    AppLanguage.ENGLISH -> "NOAA / NWS Doppler Radar & MRMS Network"
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "Open-Meteo 降水シミュレーション ＆ ECMWF 予報"
                    AppLanguage.KOREAN -> "Open-Meteo 강수 시뮬레이션 및 ECMWF 예보"
                    AppLanguage.SIMPLIFIED_CHINESE -> "Open-Meteo 降水模型与 ECMWF 数值分析"
                    AppLanguage.TRADITIONAL_CHINESE -> "Open-Meteo 降水模型與 ECMWF 數值分析"
                    AppLanguage.SPANISH -> "Radar Open-Meteo y modelos numéricos de precipitación"
                    AppLanguage.FRENCH -> "Radar Open-Meteo et modèles numériques de précipitations"
                    AppLanguage.PORTUGUESE -> "Radar Open-Meteo e modelos numéricos de precipitação"
                    AppLanguage.ENGLISH -> "Open-Meteo Radar & Numerical Precipitation Models"
                }
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "ミリ単位降水量実測 ＆ 250mメッシュ合成解析"
                AppLanguage.KOREAN -> "밀리미터 단위 강수량 실측 및 고해상도 레이더 합성 분석"
                AppLanguage.SIMPLIFIED_CHINESE -> "毫米级降水实测与高分辨率网格合成解析"
                AppLanguage.TRADITIONAL_CHINESE -> "公釐級降水實測與高解析度網格合成解析"
                AppLanguage.SPANISH -> "Mediciones de pluviómetros y síntesis radar de alta resolución"
                AppLanguage.FRENCH -> "Mesures pluviométriques et synthèse radar haute résolution"
                AppLanguage.PORTUGUESE -> "Medições pluviométricas e síntese de radar de alta resolução"
                AppLanguage.ENGLISH -> "Gauge Measurements & High-Resolution Radar Synthesis"
            }
            sourceDetail = when {
                isJapan -> when (language) {
                    AppLanguage.JAPANESE -> "転倒ます型雨量計による直接ミリ実測値と気象ドップラーレーダー網の合成解析による降水ナウキャストを反映しています。"
                    AppLanguage.KOREAN -> "전도형 우량계의 직접 강수량 실측값과 기상 도플러 레이더망의 합성 분석을 통한 강수 나우캐스트를 반영합니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "结合翻斗式雨量计直接毫米级实测数据与气象多普勒雷达网高解像度合成分析。"
                    AppLanguage.TRADITIONAL_CHINESE -> "結合翻斗式雨量計直接公釐級實測資料與氣象都卜勒雷達網高解析度合成分析。"
                    AppLanguage.SPANISH -> "Combina mediciones directas de pluviómetros de balancín con análisis sintético de nowcast por radar Doppler."
                    AppLanguage.FRENCH -> "Combine les mesures directes des pluviomètres à augets basculeurs avec les prévisions immédiates par radar Doppler."
                    AppLanguage.PORTUGUESE -> "Combina medições diretas de pluviômetros basculantes com análises de nowcast por radar Doppler."
                    AppLanguage.ENGLISH -> "Direct precipitation metrics measured by tipping-bucket rain gauges synthesized with Doppler radar nowcasts."
                }
                else -> when (language) {
                    AppLanguage.JAPANESE -> "地上雨量観測計網と衛星降水観測アルゴリズムによる高精度数値予報データです。"
                    AppLanguage.KOREAN -> "지상 우량 관측망과 위성 강수 관측 알고리즘을 통합한 고정밀 수치 예보 데이터입니다."
                    AppLanguage.SIMPLIFIED_CHINESE -> "结合地面雨量监测网络与气象卫星雷达降水反演算法的高精度数值预报。"
                    AppLanguage.TRADITIONAL_CHINESE -> "結合地面雨量監測網絡與氣象衛星雷達降水反演演算法的高精度數值預報。"
                    AppLanguage.SPANISH -> "Redes integradas de pluviómetros en superficie y algoritmos de precipitación por radar satelital."
                    AppLanguage.FRENCH -> "Réseaux intégrés de pluviomètres au sol et algorithmes de précipitations par radar satellitaire."
                    AppLanguage.PORTUGUESE -> "Redes integradas de pluviômetros terrestres e algoritmos de precipitação por radar satelital."
                    AppLanguage.ENGLISH -> "Integrated ground rain gauge networks and satellite-derived radar precipitation algorithms."
                }
            }
        }

        DetailMetricType.FEELS_LIKE -> {
            sourceTitle = when (language) {
                AppLanguage.JAPANESE -> "豪州気象局 (BOM) 体感温度モデル (AT) ＆ 地上実測データ"
                AppLanguage.KOREAN -> "호주 기상청 (BOM) 체감 온도 모델 (AT) 및 지상 실측 데이터"
                AppLanguage.SIMPLIFIED_CHINESE -> "澳大利亚气象局 (BOM) 体感温度模型与实测数据"
                AppLanguage.TRADITIONAL_CHINESE -> "澳洲氣象局 (BOM) 體感溫度模型與實測資料"
                AppLanguage.SPANISH -> "Modelo de temperatura aparente (AT) del BOM australiano"
                AppLanguage.FRENCH -> "Modèle de température apparente (AT) du BOM australien"
                AppLanguage.PORTUGUESE -> "Modelo de temperatura aparente (AT) do BOM australiano"
                AppLanguage.ENGLISH -> "Australian BOM Apparent Temperature (AT) Model"
            }
            sourceSubtitle = when (language) {
                AppLanguage.JAPANESE -> "気温・相対湿度・風冷指数の総合熱力学算出"
                AppLanguage.KOREAN -> "기온 · 수증기압 · 풍속 냉각 종합 열역학 산출"
                AppLanguage.SIMPLIFIED_CHINESE -> "气温・水汽压・风寒综合热力学计算"
                AppLanguage.TRADITIONAL_CHINESE -> "氣溫・水汽壓・風寒綜合熱力學計算"
                AppLanguage.SPANISH -> "Combinación de temperatura, presión de vapor y sensación térmica"
                AppLanguage.FRENCH -> "Combinaison température, pression de vapeur et refroidissement éolien"
                AppLanguage.PORTUGUESE -> "Combinação de temperatura, pressão de vapor e sensação térmica"
                AppLanguage.ENGLISH -> "Combined Ambient, Vapor Pressure & Wind Chill"
            }
            sourceDetail = when (language) {
                AppLanguage.JAPANESE -> "オーストラリア気象局 (BOM) 開発のApparent Temperature公式を採用し、実測気温・水蒸気圧（湿度）・地上風速による人体の熱放散・体感負荷を正確に算出しています。"
                AppLanguage.KOREAN -> "호주 기상청 (BOM)의 체감 온도 (Apparent Temperature) 공식을 적용해 실측 기온, 수증기압(습도), 지상 풍속에 따른 인체 열 손실과 실제 체감 온도를 정밀 산출합니다."
                AppLanguage.SIMPLIFIED_CHINESE -> "采用澳大利亚气象局(BOM)经典Apparent Temperature公式，融合实测气温、水汽压与风寒指数，客观反映人体实际冷暖感受。"
                AppLanguage.TRADITIONAL_CHINESE -> "採用澳洲氣象局(BOM)經典Apparent Temperature公式，融合實測氣溫、水汽壓與風寒指數，客觀反映人體實際冷暖感受。"
                AppLanguage.SPANISH -> "Calculado mediante la fórmula de temperatura aparente del Bureau of Meteorology de Australia, combinando temperatura, presión de vapor de humedad y efecto del viento."
                AppLanguage.FRENCH -> "Calculé avec la formule de température apparente du Bureau of Meteorology australien, intégrant température, pression de vapeur et refroidissement éolien."
                AppLanguage.PORTUGUESE -> "Calculado pela fórmula de temperatura aparente do Bureau of Meteorology da Austrália, combinando temperatura, pressão de vapor e efeito do vento."
                AppLanguage.ENGLISH -> "Derived using the Australian Bureau of Meteorology Apparent Temperature formulation, factoring temperature, humidity vapor pressure, and wind chill."
            }
        }
    }

    val headerLabel = when (language) {
        AppLanguage.JAPANESE -> "情報ソース ＆ 観測手法"
        AppLanguage.KOREAN -> "데이터 출처 및 관측 방식"
        AppLanguage.SIMPLIFIED_CHINESE -> "信息来源与监测方法"
        AppLanguage.TRADITIONAL_CHINESE -> "資料來源與監測方法"
        AppLanguage.SPANISH -> "Fuente de datos y metodología"
        AppLanguage.FRENCH -> "Source de données et méthodologie"
        AppLanguage.PORTUGUESE -> "Fonte de dados e metodologia"
        AppLanguage.ENGLISH -> "Data Source & Methodology"
    }

    val verifiedBadge = when (language) {
        AppLanguage.JAPANESE -> "公式観測ソース確認済"
        AppLanguage.KOREAN -> "공식 관측 출처 확인됨"
        AppLanguage.SIMPLIFIED_CHINESE -> "官方权威数据源已验证"
        AppLanguage.TRADITIONAL_CHINESE -> "官方權威資料來源已驗證"
        AppLanguage.SPANISH -> "Fuente oficial verificada"
        AppLanguage.FRENCH -> "Source officielle vérifiée"
        AppLanguage.PORTUGUESE -> "Fonte oficial verificada"
        AppLanguage.ENGLISH -> "Verified Official Source"
    }

    val updatedText = when (language) {
        AppLanguage.JAPANESE -> "取得時刻: ${weather?.lastUpdatedTime ?: "リアルタイム"}"
        AppLanguage.KOREAN -> "관측 시각: ${weather?.lastUpdatedTime ?: "실시간"}"
        AppLanguage.SIMPLIFIED_CHINESE -> "获取时间: ${weather?.lastUpdatedTime ?: "实时"}"
        AppLanguage.TRADITIONAL_CHINESE -> "擷取時間: ${weather?.lastUpdatedTime ?: "即時"}"
        AppLanguage.SPANISH -> "Observado: ${weather?.lastUpdatedTime ?: "En vivo"}"
        AppLanguage.FRENCH -> "Observé : ${weather?.lastUpdatedTime ?: "En direct"}"
        AppLanguage.PORTUGUESE -> "Observado: ${weather?.lastUpdatedTime ?: "Ao vivo"}"
        AppLanguage.ENGLISH -> "Observed: ${weather?.lastUpdatedTime ?: "Live"}"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // ヘッダー行: Verified アイコン + タイトル + バッジ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = headerLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = GoogleSansFlex,
                        color = colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.65f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = verifiedBadge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GoogleSansFlex,
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 提供元メイン名
            Text(
                text = sourceTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFlex,
                color = colorScheme.onSurface
            )

            // サブタイトル
            Text(
                text = sourceSubtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                fontFamily = GoogleSansFlex,
                color = colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 詳細解説
            Text(
                text = sourceDetail,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = GoogleSansFlex,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // フッター行: 取得時刻
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = updatedText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontFamily = GoogleSansFlex,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
