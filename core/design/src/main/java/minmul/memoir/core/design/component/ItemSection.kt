package minmul.memoir.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun ItemSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
                .semantics { heading() },
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = shape,
                )
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface),
            content = content,
        )
    }
}

@Composable
fun ToggleItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        ItemRow(
            title = title,
            description = description,
            trailing = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            },
        )
        HorizontalDivider()
    }
}

@Composable
fun CheckboxItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Column(
        modifier = modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        ),
    ) {
        ItemRow(
            title = title,
            description = description,
            trailing = {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            },
        )
        HorizontalDivider()
    }
}

@Composable
fun NavigationItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        ItemRow(
            title = title,
            description = description,
            trailing = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        HorizontalDivider()
    }
}

@Composable
fun DialogItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    value: String? = null,
) {
    Column(
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
    ) {
        ItemRow(
            title = title,
            description = description,
            trailing = value?.let { selected ->
                {
                    Text(
                        text = selected,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        HorizontalDivider()
    }
}

@Composable
fun SliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    Column(modifier = modifier) {
        ItemRow(
            title = title,
            description = description,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider()
    }
}

@Composable
private fun ItemRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        modifier = modifier,
        supportingContent = description?.let { text ->
            { Text(text) }
        },
        trailingContent = trailing,
    ) {
        Text(title)
    }
}

@Preview(showBackground = true)
@Composable
private fun ToggleItemPreview() {
    var checked by remember { mutableStateOf(true) }
    MemoirTheme {
        ToggleItem(
            title = stringResource(R.string.preview_title),
            description = stringResource(R.string.preview_description),
            checked = checked,
            onCheckedChange = { checked = it },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckboxItemPreview() {
    var checked by remember { mutableStateOf(false) }
    MemoirTheme {
        CheckboxItem(
            title = stringResource(R.string.preview_title),
            description = stringResource(R.string.preview_description),
            checked = checked,
            onCheckedChange = { checked = it },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NavigationItemPreview() {
    MemoirTheme {
        NavigationItem(
            title = stringResource(R.string.preview_title),
            description = stringResource(R.string.preview_description),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DialogItemPreview() {
    MemoirTheme {
        DialogItem(
            title = stringResource(R.string.preview_title),
            description = stringResource(R.string.preview_description),
            value = stringResource(R.string.preview_value),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SliderItemPreview() {
    var value by remember { mutableFloatStateOf(0.5f) }
    MemoirTheme {
        SliderItem(
            title = stringResource(R.string.preview_title),
            description = stringResource(R.string.preview_description),
            value = value,
            onValueChange = { value = it },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemSectionPreview() {
    var toggled by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(false) }
    var slider by remember { mutableFloatStateOf(0.5f) }
    MemoirTheme {
        ItemSection(title = stringResource(R.string.preview_section_title)) {
            ToggleItem(
                title = stringResource(R.string.preview_title),
                description = stringResource(R.string.preview_description),
                checked = toggled,
                onCheckedChange = { toggled = it },
            )
            CheckboxItem(
                title = stringResource(R.string.preview_title),
                checked = checked,
                onCheckedChange = { checked = it },
            )
            NavigationItem(
                title = stringResource(R.string.preview_title),
                onClick = {},
            )
            DialogItem(
                title = stringResource(R.string.preview_title),
                value = stringResource(R.string.preview_value),
                onClick = {},
            )
            SliderItem(
                title = stringResource(R.string.preview_title),
                value = slider,
                onValueChange = { slider = it },
            )
        }
    }
}
