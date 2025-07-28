package com.example.it140p_spark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    main: String,
    sub: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    leadingContent: (@Composable (() -> Unit))? = null,
    trailingContent: (@Composable (() -> Unit))? = null,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    ListItem(
        headlineContent = { Text(text = main, style = style) },
        supportingContent = if (sub != null) { { Text(text = sub, style = MaterialTheme.typography.bodySmall) } } else null,
        leadingContent = leadingContent ?: (
                if (icon != null) {
                    {
                        Icon(
                            imageVector = icon,
                            contentDescription = main,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
                ),
        trailingContent = trailingContent,
        modifier = modifier.clickable(onClick = onClick)
    )
}