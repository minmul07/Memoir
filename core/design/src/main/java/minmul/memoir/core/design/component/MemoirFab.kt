package minmul.memoir.core.design.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme

@Composable
fun MemoirFab(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val containerColor = if (enabled) {
        FloatingActionButtonDefaults.containerColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContainerAlpha)
    }
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.semantics {
            if (!enabled) {
                disabled()
            }
        },
        containerColor = containerColor,
        contentColor = if (enabled) {
            contentColorFor(containerColor)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledContentAlpha)
        },
        elevation = if (enabled) {
            FloatingActionButtonDefaults.elevation()
        } else {
            FloatingActionButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                hoveredElevation = 0.dp,
                focusedElevation = 0.dp,
            )
        },
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
        )
    }
}

private const val DisabledContainerAlpha = 0.12f
private const val DisabledContentAlpha = 0.38f

@Preview(showBackground = true)
@Composable
private fun MemoirFabPreview() {
    MemoirTheme {
        MemoirFab(
            onClick = {},
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.action_add_to_queue),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemoirFabDisabledPreview() {
    MemoirTheme {
        MemoirFab(
            onClick = {},
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.action_add_to_queue),
            enabled = false,
        )
    }
}
