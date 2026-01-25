# 🔍 PlayerAuctions v2.5.1 - Auction GUI Search Fix & Pagination Accuracy

**Release Date**: January 26, 2026  
**Build**: PlayerAuctions-2.5.1-Modern.jar  
**Status**: ✅ PRODUCTION READY  

---

## 📌 Summary

Version **2.5.1** adalah patch release yang fokus memperbaiki **bug kritis double-purchase** (item bisa dibeli berkali-kali), fitur **Search di menu `/ah`** agar benar-benar berjalan, dan memastikan **pagination / total page** tetap akurat saat pemain sedang melakukan pencarian.

Patch ini mencakup fix kritis untuk mencegah eksploitasi ekonomi dan perbaikan UX untuk fitur search yang sudah ada.

---

## 🐛 Issues Fixed

### 1) Double Purchase Exploit (CRITICAL FIX) 🔴

**Symptoms**:
- Multiple players dapat membeli item auction yang sama secara bersamaan
- Seller menerima uang berkali-kali untuk satu item yang sama
- Buyer menerima item duplikat atau kehilangan uang tanpa mendapat item
- Exploitable: spam-click atau multiple buyers dapat menguras ekonomi server

**Root Cause (technical)**:
- Auction status di-update ke FINISHED **SETELAH** money/item transfer selesai
- Race condition memungkinkan multiple buyers lolos pengecekan `status == ACTIVE` secara bersamaan
- Tidak ada mekanisme atomic reservation untuk mencegah concurrent purchases
- Flow lama:
  1. Cek ACTIVE ✓
  2. Withdraw uang buyer
  3. Deposit uang seller
  4. Beri item ke buyer
  5. Update status FINISHED ← **TOO LATE!**

**Fix (Atomic Reservation Pattern)**:
- Implementasi **optimistic locking** dengan atomic reservation:
  1. Cek auction ACTIVE ✓
  2. **Reserve auction DULU** dengan mark status FINISHED (optimistic lock)
  3. Jika reservation gagal → auction sudah dibeli, return false
  4. Withdraw uang buyer (gagal → rollback ke ACTIVE)
  5. Deposit uang seller (gagal → refund buyer + rollback ACTIVE)
  6. Beri item di main thread (thread-safe inventory ops)
  7. Log transaction dan notify seller
- Comprehensive error handling dengan rollback logic
- Inventory operations dipindah ke main thread untuk thread safety
- Better logging untuk debugging dan monitoring

**Result**: 
- ✅ Auction **hanya bisa dibeli sekali** bahkan dalam kondisi high concurrency
- ✅ Failed purchase menampilkan pesan error yang jelas
- ✅ Automatic rollback jika ada step yang gagal
- ✅ Tidak ada orphaned money atau duplicate items
- ✅ Exploit prevention untuk ekonomi server

---

### 2) Search button hanya menutup GUI
**Symptoms**:
- Player klik tombol **Search** di `/ah`
- GUI menutup
- Tidak ada hasil pencarian / seakan-akan search tidak bekerja

**Root Cause (technical)**:
- Event click pada GUI memang memanggil `player.closeInventory()` ketika start search.
- Walaupun `SearchManager` tersedia untuk intercept chat, urutan eksekusi dan timing close inventory membuat interaksi terasa seperti “fitur tidak jalan”.

**Fix**:
- Saat tombol Search diklik, plugin sekarang:
  1. **Memulai Search Session terlebih dahulu** melalui `SearchManager.startSearchSession(...)`
  2. Baru **menutup inventory di next tick** (scheduler) untuk menjaga event click tetap stabil dan sesi search pasti aktif.

Hasil: setelah klik Search, player bisa langsung ketik query di chat dan GUI akan terbuka kembali dengan hasil filter.

---

### 3) Pagination salah saat mode search
**Symptoms**:
- Saat search aktif, total pages / navigasi Next/Prev terasa tidak konsisten
- Page count dihitung memakai total aktif global, bukan jumlah hasil search

**Root Cause**:
- GUI menggunakan `getTotalActiveAuctionCount()` (count global) untuk menghitung total pages.

**Fix**:
- Ditambahkan API count terfilter end-to-end:
  - `AuctionStorage#countActiveAuctions(category, sortOrder, searchQuery)`
  - Implementasi di `JsonAuctionStorage` dan `SQLiteAuctionStorage`
  - `AuctionService#getActiveAuctionCount(...)`
  - `MainAuctionGui` menggunakan count terfilter saat `searchQuery` aktif

Hasil: totalPages sekarang sesuai hasil search, navigasi next/prev tidak menipu.

---

## 🔧 Internal Changes (Developer Notes)

- **Atomic Reservation Pattern**: Implemented optimistic locking for auction purchases to prevent race conditions
- **Thread Safety**: Moved all inventory operations to main thread using Bukkit scheduler
- **Rollback Logic**: Added comprehensive rollback mechanism for failed transactions
- **Error Handling**: Enhanced error logging and user feedback for purchase failures
- Added storage-level method untuk menghitung auction aktif dengan filter yang sama seperti `findActive/findActiveAuctions`
- Perbaikan flow click Search untuk menghindari race/timing issues
- Output build modern kini mengikuti versi `2.5.1`

---

## ✅ Compatibility

- **Minecraft**: 1.19 - 1.21 (Modern)
- **Server**: Paper / Paper forks
- **Java**: 21
- **Economy**: Vault (required)

---

## 📦 Installation / Update

1. Stop server
2. Replace jar lama dengan: `PlayerAuctions-2.5.1-Modern.jar`
3. Start server

No config migration required.

---

## 🧪 Quick Test Checklist (In-Game)

**Basic Functionality:**
- `/ah` opens normally
- Click **Search** → muncul instruksi "enter search query"
- Ketik query (contoh: `diamond`) → GUI terbuka kembali dan item terfilter
- `cancel/exit` saat search → kembali ke GUI normal
- Next/Prev page saat search → page count konsisten

**Critical: Double Purchase Prevention:**
- ✅ Single player rapid-click pada item → hanya 1 purchase berhasil
- ✅ Two players click same item simultaneously → only one succeeds
- ✅ Failed purchase shows error message "Purchase failed › Item may be sold"
- ✅ Seller hanya receive payment sekali untuk satu item
- ✅ No duplicate items spawned
- ✅ Balance checking tetap akurat setelah failed purchase

