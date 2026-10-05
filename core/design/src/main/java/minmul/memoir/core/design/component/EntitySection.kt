package minmul.memoir.core.design.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import minmul.memoir.core.design.R
import minmul.memoir.core.design.theme.MemoirTheme
import minmul.memoir.core.design.theme.MemoirTypography

enum class EntityKind {
    Time,
    Period,
    Location,
    Account,
    Phone,
}

data class EntityItem(
    val kind: EntityKind?,
    val name: String,
    val value: String,
)

@Composable
fun EntitySection(
    entities: List<EntityItem>,
    modifier: Modifier = Modifier,
) {
    if (entities.isEmpty()) return
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            entities.forEachIndexed { index, entity ->
                key(entity.kind, entity.name, entity.value, index) {
                    EntityRow(entity = entity)
                    if (index < entities.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun EntityRow(
    entity: EntityItem,
    modifier: Modifier = Modifier,
) {
    val kind = entity.kind
    val description = if (kind == null) {
        stringResource(
            R.string.entity_card_content_description_plain,
            entity.name,
            entity.value,
        )
    } else {
        stringResource(
            R.string.entity_card_content_description,
            stringResource(kind.labelRes),
            entity.name,
            entity.value,
        )
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = entity.name,
            style = MemoirTypography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entity.value,
            style = MemoirTypography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val EntityKind.labelRes: Int
    get() = when (this) {
        EntityKind.Time -> R.string.entity_kind_time
        EntityKind.Period -> R.string.entity_kind_period
        EntityKind.Location -> R.string.entity_kind_location
        EntityKind.Account -> R.string.entity_kind_account
        EntityKind.Phone -> R.string.entity_kind_phone
    }


@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EntitySectionSinglePreview() {
    MemoirTheme {
        EntitySection(
            entities = listOf(
                EntityItem(
                    kind = EntityKind.Time,
                    name = stringResource(R.string.preview_entity_time_name),
                    value = stringResource(R.string.preview_entity_time_value),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EntitySectionListPreview() {
    MemoirTheme {
        EntitySection(
            entities = listOf(
                EntityItem(
                    kind = EntityKind.Time,
                    name = stringResource(R.string.preview_entity_time_name),
                    value = stringResource(R.string.preview_entity_time_value),
                ),
                EntityItem(
                    kind = EntityKind.Period,
                    name = stringResource(R.string.preview_entity_period_name),
                    value = stringResource(R.string.preview_entity_period_value),
                ),
                EntityItem(
                    kind = EntityKind.Location,
                    name = stringResource(R.string.preview_entity_location_name),
                    value = stringResource(R.string.preview_entity_location_value),
                ),
                EntityItem(
                    kind = EntityKind.Account,
                    name = stringResource(R.string.preview_entity_account_name),
                    value = stringResource(R.string.preview_entity_account_value),
                ),
                EntityItem(
                    kind = EntityKind.Phone,
                    name = stringResource(R.string.preview_entity_phone_name),
                    value = stringResource(R.string.preview_entity_phone_value),
                ),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
