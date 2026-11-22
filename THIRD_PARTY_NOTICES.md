# Third-Party Notices & Attributions

Expressive Weather incorporates third-party open-source software, fonts, icons, and public weather/geospatial datasets. This document lists the applicable licenses and attributions.

---

## 1. Bundled Fonts

### Google Sans Flex
- **File**: `app/src/main/res/font/google_sans_flex.ttf`
- **Copyright**: Copyright 2015 Google LLC. All Rights Reserved.
- **License**: SIL Open Font License, Version 1.1 (OFL-1.1)

```text
SIL OPEN FONT LICENSE Version 1.1 - 26 February 2007

PREAMBLE
The goals of the Open Font License (OFL) are to stimulate worldwide
development of collaborative font projects, to support the font creation
efforts of academic and linguistic communities, and to provide a free and
open framework in which fonts may be shared and improved in partnership
with others.

The OFL allows the licensed fonts to be used, studied, modified and
redistributed freely as long as they are not sold by themselves. The
fonts, including any derivative works, can be bundled, embedded,
redistributed and/or sold with any software provided that any reserved
names are not used by derivative works. The fonts and derivatives,
however, cannot be released under any other type of license. The
requirement for fonts to remain under this license does not apply
to any document created using the fonts or their derivatives.

DEFINITIONS
"Font Software" refers to the set of files released by the Copyright
Holder(s) under this license and clearly marked as such. This may
include source files, build scripts and documentation.

"Reserved Font Name" refers to any names specified as such after the
copyright statement(s).

"Original Version" refers to the collection of Font Software components as
distributed by the Copyright Holder(s).

"Modified Version" refers to any derivative made by adding to, deleting,
or substituting -- in part or in whole -- any of the components of the
Original Version, by changing formats or by porting the Font Software to a
new environment.

"Author" refers to any designer, engineer, programmer, technical
writer or other person who contributed to the Font Software.

PERMISSION & CONDITIONS
Permission is hereby granted, free of charge, to any person obtaining
a copy of the Font Software, to use, study, copy, merge, embed, modify,
redistribute, and sell modified and unmodified copies of the Font
Software, subject to the following conditions:

1) Neither the Font Software nor any of its individual components,
in Original or Modified Versions, may be sold by itself.

2) Original or Modified Versions of the Font Software may be bundled,
redistributed and/or sold with any software, provided that each copy
contains the above copyright notice and this license. These can be
included either as stand-alone text files, human-readable headers or
in the appropriate machine-readable metadata fields within text or
binary files as long as those fields can be easily viewed by the user.

3) No Modified Version of the Font Software may use the Reserved Font
Name(s) unless explicit written permission is granted by the Copyright
Holder. This restriction only applies to the primary font name as
presented to the users.

4) The name(s) of the Copyright Holder(s) or the Author(s) of the Font
Software shall not be used to endorse or promote products derived from
this Font Software without specific prior written permission.

5) The Font Software, modified or unmodified, in part or in whole,
must be distributed entirely under this license, and must not be
distributed under any other license. The requirement for fonts to
remain under this license does not apply to any document created
using the Font Software.

TERMINATION
This license becomes null and void if any of the above conditions are
not met.

DISCLAIMER
THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT
OF COPYRIGHT, PATENT, TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL THE
COPYRIGHT HOLDER BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
OF THE USE OR INABILITY TO USE THE FONT SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE FONT SOFTWARE.
```

---

## 2. Bundled Icons

### Microsoft Fluent System Icons
- **Files**: `app/src/main/res/drawable/ic_fluent_*.xml`
- **Copyright**: Copyright (c) Microsoft Corporation.
- **License**: MIT License (https://github.com/microsoft/fluentui-system-icons/blob/main/LICENSE)

---

## 3. Open-Source Libraries

| Library | License |
| :--- | :--- |
| AndroidX (Core, Activity, Lifecycle, Room, WorkManager, Navigation3, Compose) | Apache License 2.0 |
| Kotlin Standard Library, Coroutines, `kotlinx.serialization` | Apache License 2.0 |
| OkHttp (Square, Inc.) | Apache License 2.0 |
| Haze (Chris Banes) | Apache License 2.0 |
| JUnit 4 (Test dependency) | Eclipse Public License 1.0 |

### Proprietary SDK Notice
- **Google Play Services Maps SDK (`com.google.android.gms:play-services-maps`)**: Governed by the [Google APIs Terms of Service](https://developers.google.com/terms) and [Google Maps Platform Terms of Service](https://cloud.google.com/maps-platform/terms).

---

## 4. Weather, Radar & Geospatial Data Sources

- **Open-Meteo**: Weather data by [Open-Meteo.com](https://open-meteo.com/) (Licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)). Note: Open-Meteo's free public tier is for non-commercial use only.
- **Japan Meteorological Agency (JMA)**: 出典：気象庁（[https://www.jma.go.jp/](https://www.jma.go.jp/)）. 本アプリは気象庁公式のアプリではありません。気象庁のデータを加工して表示しています。
- **Geospatial Information Authority of Japan (GSI)**: 出典：国土地理院（地理院タイル・ハザードマップポータルサイト [https://maps.gsi.go.jp/development/ichiran.html](https://maps.gsi.go.jp/development/ichiran.html)）.
- **NOAA / National Weather Service (NWS)**: Data provided by [NOAA / NWS](https://www.weather.gov/) (Public Domain).
- **Iowa Environmental Mesonet (IEM)**: US NEXRAD / MRMS / HRRR radar tiles hosted by [Iowa Environmental Mesonet, Iowa State University](https://mesonet.agron.iastate.edu/).
- **MeteoAlarm (EUMETNET)**: European weather warnings provided by [MeteoAlarm](https://meteoalarm.org/).
- **World Meteorological Organization (WMO)**: Official weather warnings via [WMO Severe Weather Information Centre](https://severeweather.wmo.int/).
- **GDACS**: Global disaster alerts provided by [GDACS](https://www.gdacs.org/) (United Nations & European Commission).
- **RainViewer**: Global radar tiles provided by [RainViewer](https://www.rainviewer.com/). Note: Free public API is for non-commercial use.
- **Photon / OpenStreetMap**: Geocoding by [Photon (komoot)](https://photon.komoot.io/), Data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors ([ODbL](https://opendatacommons.org/licenses/odbl/)).
- **Google Maps**: Map data © Google.
