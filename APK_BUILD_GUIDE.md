# 📱 Quick Delivery — Zero-Laptop Mobile Cloud APK Build Guide (Customer, Rider & Admin)

Agar aapke paas **laptop nahi hai** aur aap **sirf mobile phone** se kaam kar rahe hain, toh is project me **GitHub Actions Automatic Cloud APK Builder (`.github/workflows/build-three-apks.yml`)** pehle se configure kar diya gaya hai.

Jab bhi code aapke GitHub repository (`https://github.com/india1256/QuickDelivery`) par push hota hai, **GitHub ka Cloud Server khud automatic teeno (3) Android `.apk` files banata hai** aur unhe direct download ke liye ready kar deta hai!

---

## 🚀 Mobile Se 3 APKs Kaise Banayein Aur Download Karein (Step-by-Step)

### Step 1: AI Studio se Code GitHub par Push karein (Mobile Browser se)
1. Apne mobile browser me **AI Studio** ke top-right corner me **Settings (⚙️) / GitHub icon** par tap karein.
2. **"Push to GitHub"** select karein aur apna repository URL **`https://github.com/india1256/QuickDelivery.git`** (branch: `main`) choose karke sign-in/authorize karein.
3. Jaise hi code GitHub par push hoga, GitHub Actions automatic chalu ho jayega!

### Step 2: (Optional) Mobile se Manual Cloud Build Chalu Karna
Agar aap kabhi bhi mobile se dubara fresh APK build trigger karna chahein:
1. Mobile Chrome me yeh link kholein:
   👉 **[https://github.com/india1256/QuickDelivery/actions](https://github.com/india1256/QuickDelivery/actions)**
2. **"Build 3 Quick Delivery APKs (Customer, Rider, Admin)"** workflow par tap karein.
3. **"Run workflow"** button dabayein -> `all` select karein -> green **"Run workflow"** button par tap karein.
4. Cloud build lagbhag **4 se 6 minute** me teeno APKs taiyaar kar dega.

### Step 3: Mobile me Direct 1-Tap `.apk` Download Karein (Bina ZIP Extract Kiye!)
Cloud build pura hote hi GitHub automatic ek **Release (`latest-apks`)** publish kar deta hai jahan se aap sidha `.apk` download kar sakte hain:
1. Apne mobile browser me yeh link kholein:
   👉 **[https://github.com/india1256/QuickDelivery/releases/tag/latest-apks](https://github.com/india1256/QuickDelivery/releases/tag/latest-apks)**
   *(Ya **[https://github.com/india1256/QuickDelivery/releases](https://github.com/india1256/QuickDelivery/releases)** par jaayein)*
2. Niche **Assets** section me aapko charo `.apk` files milengi — jis par tap karenge woh sidha mobile me download aur install ho jayegi:

| APK File Name | App Name on Phone | Package ID | Role / Interface |
| :--- | :--- | :--- | :--- |
| **`QuickDelivery-Customer.apk`** | `Quick Delivery` | `com.aistudio.quickdelivery.ltkeqf` | 🛍️ **Customer App** (Order placement, live map tracking, order history) |
| **`QuickDelivery-Rider-Partner.apk`** | `QD Rider Partner` | `com.aistudio.quickdelivery.ltkeqf.rider` | 🛵 **Rider Partner App** (Accept orders, GPS navigation, delivery proof photo, wallet) |
| **`QuickDelivery-Admin-Enterprise.apk`** | `QD Admin Console` | `com.aistudio.quickdelivery.ltkeqf.admin` | 🛡️ **Admin Enterprise App** (Live order status management, rider dispatch, analytics) |
| **`QuickDelivery-AllInOne-Hub.apk`** | `Quick Delivery` | `com.aistudio.quickdelivery.ltkeqf.hub` | 🌐 **All-in-One 3-Tab Hub** (Customer + Rider + Admin switcher in one app) |

> 💡 **Sabse Badi Suvidha:** Kyunki Customer, Rider, aur Admin APKs ke **Package IDs alag-alag hain**, aap **apne ek hi Android phone me teeno APKs ek saath install** kar sakte hain! Teeno apps ek hi live **Firebase Firestore Cloud Database** se connected hain—Customer app me order lagate hi Rider aur Admin app me turant live dikhega.

---

## 📦 Alternative: GitHub Actions Artifacts se Download Karna
Agar aap GitHub Actions run page se download karna chahein:
1. **[https://github.com/india1256/QuickDelivery/actions](https://github.com/india1256/QuickDelivery/actions)** par sabse upar wale green tick (✅) wale build par tap karein.
2. Page ke sabse niche **Artifacts** section me jaayein:
   - `QuickDelivery-Customer-APK`
   - `QuickDelivery-Rider-Partner-APK`
   - `QuickDelivery-Admin-Enterprise-APK`
   - `QuickDelivery-All-APKs-Bundle`

---

## 🛠️ Technical Details of `.github/workflows/build-three-apks.yml`

- **Cloud Runner:** `ubuntu-latest` with JDK 21 (Temurin), Android SDK, and Gradle `9.3.1`.
- **Automatic Signing:** Uses `debug.keystore.base64` (or generates a standard Android debug key if missing) so every generated `.apk` is ready to install on any Android phone immediately.
- **Role Locking via Gradle Properties:**
  - Customer APK: `gradle :app:assembleDebug -PappRole=CUSTOMER`
  - Rider Partner APK: `gradle :app:assembleDebug -PappRole=RIDER -PappIdSuffix=.rider`
  - Admin Enterprise APK: `gradle :app:assembleDebug -PappRole=ADMIN -PappIdSuffix=.admin`
  - All-in-One Hub APK: `gradle :app:assembleDebug -PappRole=UNIFIED -PappIdSuffix=.hub`
