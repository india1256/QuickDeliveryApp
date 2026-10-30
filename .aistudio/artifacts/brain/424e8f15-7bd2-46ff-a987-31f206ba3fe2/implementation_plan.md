# Quick Delivery Brand Alignment, Single Duty Control & Clean Rider Menu

## Summary of Changes Implemented

### 1. Complete Porter Branding Removal & Brand Palette Alignment
- **Branding**: Replaced all visible instances of "Porter", "Porter Partner", "PORTER PARTNER", "Porter Guidelines", "Porter Negative Balance Simulator", "1800-PORTER-CARE", and "Qucky" with **Quick Delivery** / **Quick Delivery Partner**.
- **Palette**: Applied official brand colors:
  - **Teal**: `#078B8D`
  - **Orange**: `#FF7A1A`
  - **Tagline**: `“FAST. SIMPLE. SECURE.”`
- **Logo**: Incorporated `QuickDeliveryOfficialLogo` across both Customer and Rider app headers and navigation drawers.
- **Support**: Configured support contacts to `1800-QUICK-CARE / 1800-258-258`.
- **Platform Sync**: Updated `metadata.json` to `"name": "Quick Delivery"`, and `rider/src/main/res/values/values.xml` to `"app_name": "Quick Delivery Partner App"`.

### 2. Single Online/Offline Duty Control
- **Top Header Toggle**: Retained the top bar switch (`rider_top_duty_switch`) as the **sole, authoritative Online/Offline control** on the Rider screen.
- **Removed Duplicate Toggles**:
  - Removed duplicate switch from the **Earnings / Duty card** on the main Rider screen; replaced with a read-only live status indicator pill (`rider_online_status_pill`).
  - Removed duplicate switch and click-toggle from the **Rider Side Navigation Drawer** (`drawer_duty_status_strip`); replaced with a read-only live status readout.
- **Shared State**: All screens read directly from `currentRider?.isOnline == true`. Toggling the top header switch updates duty state everywhere across the app without extra controls.

### 3. Streamlined Rider Drawer Menu
- Retained all functional menu options:
  1. **Profile**: `My Profile & Partner Details`
  2. **KYC**: `Scan KYC Documents` (Direct camera document scanner)
  3. **Earnings**: `Earnings & Passbook` (Instant cashout & passbook card)
  4. **History**: `Trip & Delivery History` (Past completed orders & receipts)
  5. **Vehicle**: `Vehicle & Document Management` (Vehicle details & specs)
  6. **Safety/SOS**: `Safety & Emergency SOS` (112 emergency hotline & support alert)
  7. **Support**: `Help Desk & Partner Support` (1800-QUICK-CARE helpline)
  8. **Guidelines**: `Quick Delivery Guidelines & SOP`
  9. **Sign Out**: `Sign Out / Switch Account`
- **Demo / Simulator Removal**: Completely removed the negative balance simulation panel (`-₹160`, `-₹210`, `+₹500` demo buttons) from the drawer.
