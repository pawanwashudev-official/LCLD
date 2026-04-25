# 🛡️ LCLD: Locate & Control Lost Device

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Open Source](https://img.shields.io/badge/Open%20Source-%E2%9D%A4-red.svg)](https://github.com/pawanwashudev-official/LCLD)

**LCLD (Locate & Control Lost Device)** is the ultimate privacy-focused, offline-first security solution for your Android device. Unlike standard "Find My Device" apps that rely heavily on proprietary services, LCLD puts **absolute control** back into your hands.

Developed by **Pawan Washudev** ([@pawanwashudev-official](https://github.com/pawanwashudev-official))

---

## 🚀 Why LCLD is Better Than Default FMD Apps

Standard FMD apps are often tied to a single ecosystem, require an active internet connection, and may compromise your privacy. **LCLD breaks these barriers.**

| Feature | Default FMD Apps | **LCLD** |
| :--- | :---: | :---: |
| **Offline Control** | ❌ (Requires Data/WiFi) | ✅ **Full Control via SMS** |
| **Privacy** | ⚠️ Tracked by Big Tech | ✅ **100% Private / Self-Hostable** |
| **Remote Camera** | ❌ Usually Not Available | ✅ **Capture Photos Remotely** |
| **No Google Services** | ❌ Hard Dependency | ✅ **Works on de-Googled ROMs** |
| **Open Source** | ❌ Proprietary | ✅ **Auditable & Secure** |

---

## ✨ Key Features

### 📍 Precision Locating
Get the exact GPS coordinates of your device instantly. Even if GPS is off, LCLD can attempt to toggle it remotely or use cell tower data.

### 🔊 Remote Ringer
Misplaced your phone in the house? Trigger a loud, piercing ring even if the device is on **Silent** or **Do Not Disturb** mode.

### 📸 Remote Camera Snap
Take a front or back camera photo remotely to see who has your device or where it is located. The photo is sent back to you securely.

### 🔒 Advanced Locking & Messaging
Lock your device with a new PIN and display a custom message/contact number on the screen to help the finder return it.

### 🧹 Secure Wipe (Factory Reset)
If all else fails, protect your sensitive data by triggering a full factory reset remotely to ensure your privacy remains intact.

### 📡 Multiple Control Channels
- **SMS Control:** Send commands from any trusted phone without needing the internet.
- **Web Server:** Use the [FMD Server](https://server.fmd-foss.org) for a modern web interface.
- **Notification Control:** Control via notification replies from other apps.

---

## 🛠️ Getting Started

### 1. Installation
Download the latest APK from the [Releases](https://github.com/pawanwashudev-official/LCLD/releases) page and install it on your Android device.

### 2. Configuration
- **Set a Secure PIN:** This PIN is required for all SMS commands.
- **Trusted Contacts:** Add phone numbers that are authorized to control your device.
- **Permissions:** Grant the necessary permissions (Location, Camera, SMS, Device Admin) for full functionality.

### 3. Remote Control
- **Via SMS:** Send `fmd <your_pin> locate` to your lost phone.
- **Via Web:** Log in to your connected FMD server to track your device in real-time.

---

## 🔒 Security & Privacy
LCLD is built with a **Security-First** mindset. 
- **No Tracking:** We don't track your location. Your data is only shared with the channels *you* configure.
- **Encrypted Communication:** Server communication is secured via industry-standard encryption.
- **Self-Hostable:** For maximum privacy, you can host your own [FMD Server](https://gitlab.com/fmd-foss/fmd-server).

---

## ❤️ Credits & Foundation
LCLD is a rebranded and enhanced version of the **Find My Device (FMD)** app by **Nulide**. We are immensely grateful for their high-quality open-source foundation.

- **Original Project:** [FMD Android](https://gitlab.com/fmd-foss/fmd-android)
- **Original Developer:** [Nulide](https://fmd-foss.org)

---

## 🤝 Contributing
Contributions are welcome! Feel free to open issues or submit pull requests to make LCLD even better.

**Developed with ❤️ by Pawan Washudev**
