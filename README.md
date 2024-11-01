# ExifEraser
[![License](https://img.shields.io/github/license/Tommy-Geenexus/exif-eraser)](https://mit-license.org/)
![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/Tommy-Geenexus/exif-eraser/total)
[![Crowdin](https://badges.crowdin.net/exif-eraser/localized.svg)](https://crowdin.com/project/exif-eraser)<p>
![GitHub Release](https://img.shields.io/github/v/release/Tommy-Geenexus/exif-eraser)
![GitHub Downloads (all assets, latest release)](https://img.shields.io/github/downloads/Tommy-Geenexus/exif-eraser/latest/total)<p>
[![Assemble](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/assemble.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/assemble.yml)
[![Unit Tests](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/tests_unit.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/tests_unit.yml)
[![Instrumentation Tests](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/tests_instrumented.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/tests_instrumented.yml)
[![Detekt](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/detekt.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/detekt.yml)
[![Ktlint](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/ktlint.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/ktlint.yml)
[![Spotless](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/spotless.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/spotless.yml)
[![Crowdin Upload](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/crowdin_upload.yml/badge.svg)](https://github.com/Tommy-Geenexus/exif-eraser/actions/workflows/crowdin_upload.yml)

## Description
ExifEraser is a modern, permissionless image metadata erasing application for Android 6.0+.

It relies on [exif-interface-extended](https://github.com/Tommy-Geenexus/exif-interface-extended) under the hood.<p>

## Currently Supported Image Formates
- **JPEG**: Images will be saved excluding the embedded *ICC Profile, EXIF, Photoshop Image Resources* and *XMP/ExtendedXMP* metadata
- **PNG**: Images will be saved excluding the embedded *ICC Profile, EXIF* and *XMP* metadata
- **WebP**: Images will be saved excluding the embedded *ICC Profile, EXIF* and *XMP* metadata

## Localization 
- [Help translate this application into the language of your preference](https://crowdin.com/project/exif-eraser/invite?h=7814ed7d3215d8221577cc5b8aa661ee2190921)

## Download
<a href='https://github.com/Tommy-Geenexus/exif-eraser/releases/latest'><img alt='Get it on GitHub' height='80' src='https://s1.ax1x.com/2023/01/12/pSu1a36.png'/></a>
<a href='https://play.google.com/store/apps/details?id=com.none.tom.exiferaser'><img alt='Get it on Google Play' height='80' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png'/></a>
<a href="https://accrescent.app/app/com.none.tom.exiferaser"><img alt="Get it on Accrescent" height='80' src="https://accrescent.app/badges/get-it-on.png"></a>
<a href="https://apt.izzysoft.de/fdroid/index/apk/com.none.tom.exiferaser"><img alt="Get it on IzzyOnDroid" height='80' src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"></a>

You can [verify](https://developer.android.com/tools/apksigner#usage-verify) the signing certificate on the APK matches this SHA256 fingerprint:

```D9:10:86:7C:E2:CF:3D:CD:37:DB:51:85:3E:BE:F8:63:3F:D8:3A:14:2B:26:42:27:4E:D0:68:15:2F:F1:22:01```
