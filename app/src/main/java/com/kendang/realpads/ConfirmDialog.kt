package com.kendang.realpads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// Dialog konfirmasi Ya/Tidak generik - dipakai di titik-titik yang butuh
// "jeda sebentar" sebelum aksi yang gak bisa langsung dibatalin (nyimpen
// rekaman ke file, export preset ke file), biar gak berasa tiba-tiba/gak
// sengaja kepencet. Gak dibikin spesifik per fitur, satu composable dipakai
// ulang di semua tempat yang butuh pola ini.
//
// FIX WARNA: dulu background dialog ini pakai CtrlPanelColor (0xFF1C1C1E) -
// hampir sama dengan BgColor app (0xFF111113) di belakangnya, jadi dialognya
// kelihatan "nyatu" sama layar dan susah dibedain batasnya. Sekarang pakai
// PanelColor (0xFF3C3C3F), PERSIS warna container dialog lain di app ini
// (MusicLibraryDialog, OnlinePresetDialog, ColorPickerDialog, PadVolumeDialog
// - semuanya AlertDialog(containerColor = PanelColor)), dan tombol kedua
// (BUANG/TIDAK/BATAL) pakai CtrlPanelColor + PadTextColor persis kayak tombol
// "BATAL" di dialog-dialog itu. Tombol aksi utama (SIMPAN/YA/EXPORT) tetap
// warna aksen tema user (ThemeState.accentColor), sama kayak elemen "aktif"
// lain di app.
//
// Lebar dibatasi (gak lebih dari 340dp) biar gak kepanjangan buat pesan pendek.
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "YA",
    dismissLabel: String = "TIDAK",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Dialog ini MEMBLOKIR (dismissable = false): tap di luar dialog & tombol back sengaja diabaikan.
    // Kalau tidak, satu tap nyasar di luar dialog langsung memicu BUANG dan rekaman yang barusan jadi hilang.
    // User WAJIB memilih tombol secara eksplisit (SIMPAN/BUANG, dst).
    AppDialog(
        title = title,
        onDismiss = onDismiss,
        dismissable = false,
        buttons = {
            DialogButton(dismissLabel, onDismiss)
            DialogButton(confirmLabel, onConfirm, primary = true)
        }
    ) {
        Text(message, color = TextDim, fontSize = 13.sp, lineHeight = 18.sp)
    }
}
