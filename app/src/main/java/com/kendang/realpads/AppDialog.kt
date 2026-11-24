package com.kendang.realpads

import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val DIALOG_MAX_WIDTH = 380.dp
private val DIALOG_EDGE_MARGIN = 16.dp      // jarak minimum kartu ke tepi layar (kiri & kanan sama)
val DIALOG_PAD_H = 24.dp                    // padding isi kiri = kanan (SIMETRIS); bar gulir di tengah jalur ini
private const val DIALOG_MAX_HEIGHT_FRACTION = 0.92f

/**
 * Dialog standar SEMUA dialog di app (menggantikan AlertDialog / Dialog buatan sendiri).
 *
 * - Lebar kartu menyesuaikan layar; padding isi selalu kiri = kanan (tidak ada lagi sisi yang lebih sempit).
 * - Tinggi kartu dibatasi ke tinggi layar (landscape pendek pun aman). Judul dan tombol bawah SELALU terlihat,
 *   hanya bagian isi di tengah yang digulir.
 * - Bar gulir muncul otomatis HANYA bila isi lebih panjang dari ruang yang ada; tipis, di tengah jalur kanan,
 *   bisa diseret, plus pudar halus di tepi atas/bawah isi.
 *
 * [scrollBody] = true (default): [content] digulir oleh dialog ini. JANGAN taruh verticalScroll / LazyColumn di dalamnya.
 * [scrollBody] = false: [content] tidak digulir dialog dan TIDAK diberi padding horizontal (mendapat tinggi
 *   terbatas) - untuk isi yang punya penggulir sendiri. Bagian biasa diberi Modifier.dialogPadding(); daftar
 *   memakai [DialogListRegion] (LazyColumn) dan isi panjang memakai [DialogScrollRegion]; keduanya memberi
 *   padding + bar gulir yang sama dengan dialog biasa (bisa diseret).
 * [dismissable] = false: tap di luar dan tombol back diabaikan (dialog yang wajib memilih tombol).
 * [buttons]: isi baris tombol bawah; pakai [DialogButton] (lebar dibagi rata).
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    dismissable: Boolean = true,
    scrollBody: Boolean = true,
    maxWidth: Dp = DIALOG_MAX_WIDTH,
    subtitle: String? = null,
    titleLeading: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val cfg = LocalConfiguration.current
    val cardWidth = minOf(maxWidth.value, cfg.screenWidthDp - DIALOG_EDGE_MARGIN.value * 2).dp
    val cardMaxHeight = (cfg.screenHeightDp * DIALOG_MAX_HEIGHT_FRACTION).dp

    Dialog(
        onDismissRequest = { if (dismissable) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable)
    ) {
        Column(
            modifier = Modifier
                .width(cardWidth)
                .heightIn(max = cardMaxHeight)
                .clip(RoundedCornerShape(28.dp))
                .background(PanelColor)
                .padding(top = 24.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H),
                verticalAlignment = Alignment.CenterVertically
            ) {
                titleLeading?.invoke()
                Text(
                    title,
                    color = PadTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                titleTrailing?.invoke()
            }
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H).padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            if (scrollBody) {
                DialogScrollBody(content)
            } else {
                // Tanpa padding horizontal: bagian biasa pakai Modifier.dialogPadding(); area gulir pakai
                // DialogListRegion / DialogScrollRegion (mengurus padding + bar gulir yang bisa diseret).
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                ) { content() }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { buttons() }
        }
    }
}

/** Tombol baris bawah dialog: lebar dibagi rata. Tanpa [container] = tombol netral (BATAL/TUTUP); aksi utama pakai warna tema. */
@Composable
fun RowScope.DialogButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    enabled: Boolean = true,
    container: Color = if (primary) ThemeState.accentColor else CtrlPanelColor,
    contentColor: Color = if (primary) ThemeState.accentTextColor else PadTextColor
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = contentColor)
    ) { Text(text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1) }
}

// ---------------------------------------------------------------------------------------------
// Penggulir (bar gulir SELALU berupa kotak 24dp yang menerima sentuhan di jalur padding kanan -> bisa diseret)
// ---------------------------------------------------------------------------------------------

/** Padding kiri/kanan standar isi dialog (untuk bagian biasa di dialog dengan scrollBody = false). */
fun Modifier.dialogPadding(): Modifier = this.padding(horizontal = DIALOG_PAD_H)

@Composable
private fun ColumnScope.DialogScrollBody(content: @Composable ColumnScope.() -> Unit) {
    val state = rememberScrollState()
    DialogScrollRegionImpl(state, Modifier.weight(1f, fill = false).fillMaxWidth(), content)
}

/**
 * Isi panjang yang digulir (untuk scrollBody = false), lebar penuh dialog: isi diberi padding 24dp kiri-kanan,
 * bar gulir tipis di tengah jalur kanan (bisa diseret) + pudar di tepi atas/bawah.
 * Beri tinggi lewat [modifier] (mis. Modifier.fillMaxSize() di dalam Box bertinggi tetap).
 */
@Composable
fun DialogScrollRegion(
    state: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = DialogScrollRegionImpl(state, modifier.fillMaxWidth(), content)

@Composable
private fun DialogScrollRegionImpl(
    state: ScrollState,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    var viewH by remember { mutableIntStateOf(0) }
    Box(modifier = modifier.onSizeChanged { viewH = it.height }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state)
                .padding(horizontal = DIALOG_PAD_H)
        ) { content() }
        EdgeFades(state.canScrollBackward, state.canScrollForward)
        DialogScrollbar(state, viewH)
    }
}

/**
 * Daftar (LazyColumn) di dialog dengan scrollBody = false. Pakai: DialogListRegion(listState, Modifier.heightIn(max = ..)) {
 * ...LazyColumn(state = listState, ...) }. Padding kiri-kanan 24dp + bar gulir yang bisa diseret + pudar tepi.
 */
@Composable
fun DialogListRegion(
    state: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // derivedStateOf: hanya berubah saat BENAR-BENAR pindah ujung, supaya isi daftar tidak ikut recompose tiap frame gulir.
    val canBack by remember(state) { derivedStateOf { state.canScrollBackward } }
    val canForward by remember(state) { derivedStateOf { state.canScrollForward } }
    Box(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = DIALOG_PAD_H)) { content() }
        EdgeFades(canBack, canForward)
        LazyDialogScrollbar(state)
    }
}

@Composable
private fun BoxScope.EdgeFades(canBack: Boolean, canForward: Boolean) {
    if (canBack) {
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(16.dp)
                .background(Brush.verticalGradient(listOf(PanelColor, Color.Transparent)))
        )
    }
    if (canForward) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(16.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, PanelColor)))
        )
    }
}

private fun thumbHeight(trackH: Float, viewH: Int, max: Int, minPx: Float): Float =
    (trackH * viewH / (viewH + max)).coerceAtLeast(minPx).coerceAtMost(trackH)

private val THUMB_COLOR = Color.White.copy(alpha = 0.38f)

/** Bar gulir 3dp tepat di TENGAH jalur padding kanan, jarak 6dp dari atas/bawah, bisa diseret. */
@Composable
private fun BoxScope.DialogScrollbar(state: ScrollState, viewH: Int) {
    val max = state.maxValue
    if (viewH <= 0 || max <= 0 || max == Int.MAX_VALUE) return
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(DIALOG_PAD_H)
            .fillMaxHeight()
            .pointerInput(max, viewH) {
                detectVerticalDragGestures { change, dy ->
                    change.consume()
                    val trackH = size.height - 12.dp.toPx()
                    val travel = (trackH - thumbHeight(trackH, size.height, state.maxValue, 36.dp.toPx())).coerceAtLeast(1f)
                    state.dispatchRawDelta(dy * state.maxValue / travel)
                }
            }
            .drawBehind {
                val inset = 6.dp.toPx()
                val trackH = size.height - inset * 2
                val thumbH = thumbHeight(trackH, size.height.toInt(), state.maxValue, 36.dp.toPx())
                val frac = state.value.toFloat() / state.maxValue.coerceAtLeast(1)
                val w = 3.dp.toPx()
                drawRoundRect(
                    color = THUMB_COLOR,
                    topLeft = Offset((size.width - w) / 2f, inset + (trackH - thumbH) * frac),
                    size = Size(w, thumbH),
                    cornerRadius = CornerRadius(w / 2f)
                )
            }
    )
}

/** Bar gulir untuk LazyColumn: posisi/ukuran dari rata-rata tinggi item yang tampak (perkiraan); diseret = loncat ke item. */
@Composable
private fun BoxScope.LazyDialogScrollbar(state: LazyListState) {
    val scrollable by remember(state) { derivedStateOf { state.canScrollForward || state.canScrollBackward } }
    val scope = rememberCoroutineScope()
    if (!scrollable) return

    // Mengembalikan (tinggiThumb, perkiraanTotalPx, tinggiViewport) atau null bila belum ada item.
    fun metrics(trackH: Float, minThumb: Float): Triple<Float, Float, Float>? {
        val info = state.layoutInfo
        val visible = info.visibleItemsInfo
        if (visible.isEmpty() || info.totalItemsCount <= 0) return null
        val avg = visible.sumOf { it.size }.toFloat() / visible.size
        val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
        val total = (avg * info.totalItemsCount).coerceAtLeast(viewport + 1f)
        val thumb = (trackH * viewport / total).coerceAtLeast(minThumb).coerceAtMost(trackH)
        return Triple(thumb, total, viewport)
    }

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .width(DIALOG_PAD_H)
            .fillMaxHeight()
            .pointerInput(state) {
                // PENTING: jangan geser dengan selisih piksel (dispatchRawDelta) - di daftar ribuan lagu satu geseran besar
                // memaksa LazyColumn mengukur SEMUA baris yang dilewati dalam satu frame -> lag lalu force close.
                // Sebagai gantinya: posisi jari di jalur -> nomor item, lalu LONCAT langsung (scrollToItem).
                val inset = 6.dp.toPx()
                val minThumb = 36.dp.toPx()
                var jumpJob: Job? = null
                fun jump(y: Float) {
                    val trackH = size.height - inset * 2
                    val info = state.layoutInfo
                    val m = metrics(trackH, minThumb) ?: return
                    val avg = info.visibleItemsInfo.sumOf { it.size }.toFloat() / info.visibleItemsInfo.size
                    val travel = (trackH - m.first).coerceAtLeast(1f)
                    val frac = ((y - inset - m.first / 2f) / travel).coerceIn(0f, 1f)
                    val targetPx = frac * (m.second - m.third).coerceAtLeast(0f)
                    val index = (targetPx / avg.coerceAtLeast(1f)).toInt().coerceIn(0, info.totalItemsCount - 1)
                    val offset = (targetPx - index * avg).toInt().coerceAtLeast(0)
                    // Batalkan lompatan sebelumnya yang belum selesai, supaya tidak menumpuk saat jari bergerak cepat.
                    jumpJob?.cancel()
                    jumpJob = scope.launch { state.scrollToItem(index, offset) }
                }
                detectVerticalDragGestures(
                    onDragStart = { jump(it.y) },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        jump(change.position.y)
                    }
                )
            }
            .drawBehind {
                val inset = 6.dp.toPx()
                val trackH = size.height - inset * 2
                val m = metrics(trackH, 36.dp.toPx()) ?: return@drawBehind
                val info = state.layoutInfo
                val avg = info.visibleItemsInfo.sumOf { it.size }.toFloat() / info.visibleItemsInfo.size
                val posPx = state.firstVisibleItemIndex * avg + state.firstVisibleItemScrollOffset
                val frac = (posPx / (m.second - m.third).coerceAtLeast(1f)).coerceIn(0f, 1f)
                val w = 3.dp.toPx()
                drawRoundRect(
                    color = THUMB_COLOR,
                    topLeft = Offset((size.width - w) / 2f, inset + (trackH - m.first) * frac),
                    size = Size(w, m.first),
                    cornerRadius = CornerRadius(w / 2f)
                )
            }
    )
}
