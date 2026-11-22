package com.example.axs_player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.axs_player.ui.theme.BackgroundGray
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.TextBlack
import com.example.axs_player.ui.theme.TextMuted
import com.example.axs_player.ui.theme.OldStandardTT

@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    fillAvailable: Boolean = false
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(value) {
        if (fieldValue.text != value) fieldValue = fieldValue.copy(text = value, selection = TextRange(value.length))
    }
    val selection = fieldValue.selection
    val hasSelection = selection.start != selection.end

    androidx.compose.foundation.layout.Column(
        modifier = modifier.clickable(
            onClick = { focusRequester.requestFocus() },
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        )
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { fieldValue = it; onValueChange(it.text) },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillAvailable) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier.heightIn(min = 140.dp, max = 420.dp))
                .then(if (showBorder) Modifier.border(2.dp, BackgroundGray) else Modifier)
                .focusRequester(focusRequester)
                .padding(16.dp),
            textStyle = LocalTextStyle.current.copy(color = TextMuted, fontSize = 14.sp),
            cursorBrush = SolidColor(TextBlack)
        )
        if (hasSelection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PageBackground)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MarkdownAction("B") { applyMarker("**", fieldValue, onValueChange) { fieldValue = it } }
                MarkdownAction("I") { applyMarker("*", fieldValue, onValueChange) { fieldValue = it } }
                MarkdownAction("2nd") { applyMarker("~~", fieldValue, onValueChange) { fieldValue = it } }
            }
        }
    }
}

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextBlack,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    linkColor: Color = Color(0xFF0033FF),
    onTextClick: (() -> Unit)? = null
) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    ClickableText(
        text = markdownAnnotatedString(text, linkColor),
        modifier = modifier,
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = fontWeight, textAlign = textAlign ?: TextAlign.Start),
        maxLines = maxLines,
        overflow = overflow,
        onClick = { position ->
            markdownAnnotatedString(text, linkColor)
                .getStringAnnotations("URL", position, position)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
                ?: onTextClick?.invoke()
        }
    )
}

private fun markdownAnnotatedString(text: String, linkColor: Color): AnnotatedString = buildStringAnnotated(text, linkColor)

private fun buildStringAnnotated(text: String, linkColor: Color): AnnotatedString = androidx.compose.ui.text.buildAnnotatedString {
    val pattern = Regex("https?://[^\\s]+|www\\.[^\\s]+|\\*\\*([^*]+)\\*\\*|\\*([^*]+)\\*|~~([^~]+)~~")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        if (match.value.startsWith("http://") || match.value.startsWith("https://") || match.value.startsWith("www.")) {
            val rawUrl = match.value.trimEnd('.', ',', '!', '?', ')', ']')
            pushStringAnnotation("URL", if (rawUrl.startsWith("www.")) "https://$rawUrl" else rawUrl)
            withStyle(SpanStyle(color = linkColor, fontStyle = FontStyle.Italic)) { append(rawUrl) }
            pop()
            cursor = match.range.first + rawUrl.length
            return@forEach
        }
        val value = match.groups[1]?.value ?: match.groups[2]?.value ?: match.groups[3]?.value.orEmpty()
        val style = when {
            match.groups[1] != null -> SpanStyle(fontWeight = FontWeight.Bold)
            match.groups[2] != null -> SpanStyle(fontStyle = FontStyle.Italic)
            else -> SpanStyle(fontFamily = OldStandardTT)
        }
        withStyle(style) { append(value) }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

@Composable
private fun MarkdownAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = TextBlack,
        fontSize = 14.sp,
        modifier = Modifier
            .border(1.dp, BackgroundGray)
            .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() })
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

private fun applyMarker(
    marker: String,
    value: TextFieldValue,
    onTextChange: (String) -> Unit,
    onFieldChange: (TextFieldValue) -> Unit
) {
    val start = value.selection.min
    val end = value.selection.max
    if (start == end) return
    val selected = value.text.substring(start, end)
    val replacement = "$marker$selected$marker"
    val text = value.text.replaceRange(start, end, replacement)
    onFieldChange(value.copy(text = text, selection = TextRange(start + replacement.length)))
    onTextChange(text)
}
