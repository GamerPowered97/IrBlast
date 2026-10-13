# IrBlast 📡

A modern, high-precision Android Universal Remote and Infrared Blaster application built for **Xiaomi Redmi Note 15 Pro 5G** and other Android devices with built-in IR hardware.

Built with **Jetpack Compose**, **Material 3**, and Android's native `ConsumerIrManager` API.

---

## 🚀 Features

- **Offline Database (100,000+ Codes):** Pre-bundled SQLite database containing verified IR codes across 500+ brands and 7 major appliance categories:
  - 📺 TV (Samsung, LG, Sony, TCL, Hisense, Philips, Panasonic, Xiaomi, etc.)
  - 📽️ Projector (Epson, BenQ, Optoma, ViewSonic, Acer, NEC, Sony, etc.)
  - ❄️ AC / Climate (Daikin, Mitsubishi, Gree, Midea, Panasonic, LG, etc.)
  - 🔊 Audio & Soundbars (Yamaha, Denon, Pioneer, Onkyo, Bose, Sonos, JBL, etc.)
  - 🌀 Fans & Ventilation (Dyson, Hunter, Minka, Lasko, etc.)
  - 📦 Set-Top Boxes & Satellite Receivers
  - 💿 Media Players / DVD / Blu-ray
- **Smart Code Finder (Auto-Scanner):**
  - **Smart Scan:** Pick your device type (e.g., *Projector*) and brand (e.g., *Epson*), tap *Start Scan*, and test signals sequentially.
  - **General Full Scan:** Brute-force cycle through the entire protocol address space with dynamic ETA and adjustable speed slider (200ms–1000ms).
  - **Pause / Resume:** Easily pause scans, test matching signals, and instantly save working remotes.
- **Protocol Support:**
  - **NEC & Extended NEC** (38 kHz, supporting extended subdevice addressing)
  - **Sony SIRC** (40 kHz, 12/15/20-bit frames with standard 3-repeat bursts)
  - **RC5 & RC6** (36 kHz Manchester bi-phase encoding)
  - **JVC** (38 kHz pulse-distance modulation)
  - **Pronto Hex** & **Raw Microsecond Timings**
- **Custom Signal Creator:** Input and blast arbitrary Pronto Hex codes or raw mark/space microsecond timings.
- **Tactile Material 3 UI:**
  - Haptic feedback on every button press with dynamic spring scale animations.
  - Dedicated D-Pad, dual rocker controls (Volume / Channel), color macro buttons, and an expandable numpad.
  - Edge-to-edge transparent system bars.
  - Custom adaptive launcher icon with Android 13+ themed icon support.
- **Theme Settings:**
  - **IrBlast Dark:** Deep OLED dark-room theme (default).
  - **System:** Synchronized with OS light/dark mode.
  - **Dynamic Color:** Material You wallpaper-based theming (Android 12+).

---

## 📦 Package Information

- **Package Name / Application ID:** `com.gamerpowered.irblast`
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 36)

---

## 📥 Installation

Download the latest `IrBlast-v1.0.0.apk` from the [Releases](https://github.com/GamerPowered97/IrBlast/releases) tab.

Install via ADB:
```bash
adb install -r IrBlast-v1.0.0.apk
```
Or transfer directly to your phone and open using the Xiaomi File Manager.

---

## 🛠️ Building from Source

```bash
git clone https://github.com/GamerPowered97/IrBlast.git
cd IrBlast
./gradlew assembleDebug
```
The compiled APK will be located in `app/build/outputs/apk/debug/`.

---

## 📄 License
This project is open-source under the MIT License.
