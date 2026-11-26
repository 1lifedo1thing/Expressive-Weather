package io.github.nemynew.expressiveweather.theme

import androidx.compose.ui.graphics.Color
import io.github.nemynew.expressiveweather.data.AppLanguage

/**
 * アプリのテーマカラー・プリセット定義。
 * 天候連動（リアルタイム自動調和）、システム動的カラー（Material You）、
 * および厳選された 6 種の独自プリセット（固定カラー）を提供。
 */
enum class ThemePreset(
    val id: String,
    val jaName: String,
    val enName: String,
    val koName: String,
    val zhHansName: String,
    val zhHantName: String,
    val esName: String,
    val frName: String,
    val ptName: String,
    val ruName: String,
    val jaDesc: String,
    val enDesc: String,
    val koDesc: String,
    val zhHansDesc: String,
    val zhHantDesc: String,
    val esDesc: String,
    val frDesc: String,
    val ptDesc: String,
    val ruDesc: String,
    val previewPrimary: Color,
    val previewContainer: Color,
    val previewTertiary: Color
) {
    WEATHER(
        id = "weather",
        jaName = "天候連動 (自動)",
        enName = "Weather Adaptive",
        koName = "날씨 연동 (자동)",
        zhHansName = "天气动态联动 (自动)",
        zhHantName = "天氣動態聯動 (自動)",
        esName = "Adaptable al clima",
        frName = "Adaptatif météo",
        ptName = "Adaptável ao clima",
        ruName = "Адаптивная (по погоде)",
        jaDesc = "天候や時間帯に応じてアプリ全体の色調がリアルタイム変化",
        enDesc = "Dynamically adjusts to real-time weather & time of day",
        koDesc = "실시간 날씨와 시간대에 맞춰 앱 전체 색조가 자동 변화",
        zhHansDesc = "根据实时天气与时段自动调整界面色调",
        zhHantDesc = "根據即時天氣與時段自動調整介面色調",
        esDesc = "Se ajusta dinámicamente al clima y la hora del día en tiempo real",
        frDesc = "S'adapte en temps réel à la météo et au moment de la journée",
        ptDesc = "Ajusta-se dinamicamente ao clima e à hora do dia em tempo real",
        ruDesc = "Динамически подстраивается под погоду и время суток в реальном времени",
        previewPrimary = Color(0xFF006492),
        previewContainer = Color(0xFFC9E6FF),
        previewTertiary = Color(0xFFBA5500)
    ),
    SYSTEM(
        id = "system",
        jaName = "システム動的カラー",
        enName = "Dynamic System (Material You)",
        koName = "시스템 다이나믹 컬러",
        zhHansName = "系统动态取色 (Material You)",
        zhHantName = "系統動態取色 (Material You)",
        esName = "Sistema dinámico (Material You)",
        frName = "Couleurs système (Material You)",
        ptName = "Sistema dinâmico (Material You)",
        ruName = "Системные цвета (Material You)",
        jaDesc = "端末の壁紙と Material You パレットに連動",
        enDesc = "Matches device wallpaper palette via Material You",
        koDesc = "기기 배경화면과 Material You 팔레트에 연동",
        zhHansDesc = "取色自设备壁纸与 Material You 动态调色盘",
        zhHantDesc = "取色自設備桌布與 Material You 動態調色盤",
        esDesc = "Combina con la paleta del fondo de pantalla mediante Material You",
        frDesc = "Reprend la palette du fond d'écran via Material You",
        ptDesc = "Combina com a paleta do papel de parede via Material You",
        ruDesc = "Подбирает палитру под обои устройства через Material You",
        previewPrimary = Color(0xFF0B57D0),
        previewContainer = Color(0xFFD3E3FD),
        previewTertiary = Color(0xFF725572)
    ),
    SKY_OCEAN(
        id = "sky_ocean",
        jaName = "スカイ・オーシャン",
        enName = "Sky Ocean",
        koName = "스카이 오션",
        zhHansName = "晴空碧蓝",
        zhHantName = "晴空碧藍",
        esName = "Cielo y Océano",
        frName = "Ciel & Océan",
        ptName = "Céu e Oceano",
        ruName = "Небо и океан",
        jaDesc = "澄み渡る青空と清涼なアクアブルーに固定",
        enDesc = "Crisp azure sky and ocean aqua tones",
        koDesc = "맑게 갠 푸른 하늘과 청량한 아쿠아 블루 톤으로 고정",
        zhHansDesc = "清澈蔚蓝天空与海洋水蓝色",
        zhHantDesc = "清澈蔚藍天空與海洋水藍色",
        esDesc = "Tonos azul cielo despejado y aguamarina oceánico",
        frDesc = "Teintes bleu azur limpide et eau d'océan",
        ptDesc = "Tons de céu azul límpido e azul-oceano",
        ruDesc = "Чистые оттенки лазурного неба и морской волны",
        previewPrimary = Color(0xFF006492),
        previewContainer = Color(0xFFC9E6FF),
        previewTertiary = Color(0xFF006874)
    ),
    EMERALD_FOREST(
        id = "emerald",
        jaName = "エメラルド・フォレスト",
        enName = "Emerald Forest",
        koName = "에메랄드 포레스트",
        zhHansName = "翡翠森林",
        zhHantName = "翡翠森林",
        esName = "Bosque Esmeralda",
        frName = "Forêt Émeraude",
        ptName = "Floresta Esmeralda",
        ruName = "Изумрудный лес",
        jaDesc = "瑞々しい新緑と大自然のミントグリーンに固定",
        enDesc = "Fresh foliage and vibrant emerald green",
        koDesc = "싱그러운 신록과 대자연의 민트 그린 톤으로 고정",
        zhHansDesc = "生机盎然的新绿与清新薄荷绿",
        zhHantDesc = "生機盎然的新綠與清新薄荷綠",
        esDesc = "Follaje fresco y verde esmeralda vibrante",
        frDesc = "Feuillage frais et vert émeraude éclatant",
        ptDesc = "Folhagem fresca e verde-esmeralda vibrante",
        ruDesc = "Свежая зелень и насыщенные изумрудные оттенки",
        previewPrimary = Color(0xFF006C4C),
        previewContainer = Color(0xFF89F8C6),
        previewTertiary = Color(0xFF3D6473)
    ),
    SUNSET_AMBER(
        id = "sunset",
        jaName = "サンセット・アンバー",
        enName = "Sunset Amber",
        koName = "선셋 앰버",
        zhHansName = "暮色琥珀",
        zhHantName = "暮色琥珀",
        esName = "Ámbar Atardecer",
        frName = "Ambre Crépuscule",
        ptName = "Âmbar Pôr do Sol",
        ruName = "Закатный янтарь",
        jaDesc = "夕暮れの温もりとゴールデンアンバーに固定",
        enDesc = "Warm twilight glow and golden amber",
        koDesc = "노을의 따뜻함과 골든 앰버 톤으로 고정",
        zhHansDesc = "日落余晖的温暖金琥珀色",
        zhHantDesc = "日落餘暉的溫暖金琥珀色",
        esDesc = "Brillo cálido del atardecer y ámbar dorado",
        frDesc = "Lueur chaleureuse du couchant et ambre doré",
        ptDesc = "Brilho acolhedor do pôr do sol e âmbar dourado",
        ruDesc = "Тёплое сияние заката и золотистый янтарь",
        previewPrimary = Color(0xFF8E4F00),
        previewContainer = Color(0xFFFFDCC2),
        previewTertiary = Color(0xFF5B6334)
    ),
    LAVENDER_TWILIGHT(
        id = "lavender",
        jaName = "ラベンダー・トワイライト",
        enName = "Lavender Twilight",
        koName = "라벤더 트와일라잇",
        zhHansName = "薰衣草暮光",
        zhHantName = "薰衣草暮光",
        esName = "Lavanda Crepúsculo",
        frName = "Lavande Crépusculaire",
        ptName = "Lavanda Crepúsculo",
        ruName = "Лавандовые сумерки",
        jaDesc = "幻想的な黄昏と深みのあるバイオレットに固定",
        enDesc = "Mystic dusk and elegant violet lavender",
        koDesc = "몽환적인 황혼과 깊이 있는 바이올렛 톤으로 고정",
        zhHansDesc = "梦幻暮光与深邃优雅紫罗兰",
        zhHantDesc = "夢幻暮光與深邃優雅紫羅蘭",
        esDesc = "Crepúsculo místico y elegante lavanda violeta",
        frDesc = "Crépuscule mystique et violet lavande élégant",
        ptDesc = "Crepúsculo místico e lavanda violeta elegante",
        ruDesc = "Таинственные сумерки и благородный фиолетово-лавандовый",
        previewPrimary = Color(0xFF6750A4),
        previewContainer = Color(0xFFEADDFF),
        previewTertiary = Color(0xFF7D5260)
    ),
    SAKURA_BLOSSOM(
        id = "sakura",
        jaName = "サクラ・ブロッサム",
        enName = "Sakura Blossom",
        koName = "사쿠라 블로섬",
        zhHansName = "樱花绯粉",
        zhHantName = "櫻花緋粉",
        esName = "Flor de Sakura",
        frName = "Fleur de Sakura",
        ptName = "Flor de Sakura",
        ruName = "Цветение сакуры",
        jaDesc = "華やかで優しいローズ・チェリーピンクに固定",
        enDesc = "Graceful sakura rose and delicate pink",
        koDesc = "화사하고 부드러운 벚꽃 로즈 핑크 톤으로 고정",
        zhHansDesc = "温柔优雅的樱花玫瑰与柔粉色",
        zhHantDesc = "溫柔優雅的櫻花玫瑰與柔粉色",
        esDesc = "Rosa sakura delicado y tonos cerezo suaves",
        frDesc = "Rose sakura délicat et teintes fleur de cerisier",
        ptDesc = "Rosa sakura gracioso e tons delicados de cerejeira",
        ruDesc = "Нежный розовый и утончённые оттенки цветущей сакуры",
        previewPrimary = Color(0xFF9C4146),
        previewContainer = Color(0xFFFFDAD9),
        previewTertiary = Color(0xFF755A2F)
    ),
    MIDNIGHT_SLATE(
        id = "slate",
        jaName = "ミッドナイト・スレート",
        enName = "Midnight Slate",
        koName = "미드나잇 슬레이트",
        zhHansName = "夜幕岩灰",
        zhHantName = "夜幕岩灰",
        esName = "Pizarra Medianoche",
        frName = "Ardoise de Minuit",
        ptName = "Ardósia Meia-Noite",
        ruName = "Полуночный сланец",
        jaDesc = "洗練されたミニマルなスレートグレーに固定",
        enDesc = "Refined minimal cool slate gray",
        koDesc = "세련되고 미니멀한 슬레이트 그레이 톤으로 고정",
        zhHansDesc = "洗练简约的极简板岩深灰",
        zhHantDesc = "洗練簡約的極簡板岩深灰",
        esDesc = "Gris pizarra frío, refinado y minimalista",
        frDesc = "Gris ardoise frais, épuré et minimaliste",
        ptDesc = "Cinza-ardósia frio, refinado e minimalista",
        ruDesc = "Сдержанный минималистичный холодный сланцево-серый",
        previewPrimary = Color(0xFF4F616E),
        previewContainer = Color(0xFFD2E5F5),
        previewTertiary = Color(0xFF655B6F)
    );

    val isFixed: Boolean
        get() = this != WEATHER && this != SYSTEM

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaName
        AppLanguage.KOREAN -> koName
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansName
        AppLanguage.TRADITIONAL_CHINESE -> zhHantName
        AppLanguage.ENGLISH -> enName
        AppLanguage.SPANISH -> esName
        AppLanguage.FRENCH -> frName
        AppLanguage.PORTUGUESE -> ptName
        AppLanguage.RUSSIAN -> ruName
    }

    fun description(language: AppLanguage): String = when (language) {
        AppLanguage.JAPANESE -> jaDesc
        AppLanguage.KOREAN -> koDesc
        AppLanguage.SIMPLIFIED_CHINESE -> zhHansDesc
        AppLanguage.TRADITIONAL_CHINESE -> zhHantDesc
        AppLanguage.ENGLISH -> enDesc
        AppLanguage.SPANISH -> esDesc
        AppLanguage.FRENCH -> frDesc
        AppLanguage.PORTUGUESE -> ptDesc
        AppLanguage.RUSSIAN -> ruDesc
    }

    companion object {
        fun fromId(id: String?): ThemePreset =
            entries.find { it.id == id } ?: WEATHER
    }
}
