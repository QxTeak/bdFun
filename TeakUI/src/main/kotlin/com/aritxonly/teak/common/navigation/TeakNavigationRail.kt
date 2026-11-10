package com.aritxonly.teak.common.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Wide-window navigation uses the same hoisted route and page backdrop as the bottom dock. */
@Composable
fun TeakNavigationRail(
    items: List<TeakFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (TeakFloatingTabItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    TeakGlassSurface(
        modifier = modifier.width(80.dp),
        shape = RoundedCornerShape(28.dp),
        recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning)
            .copy(geometry = GlassGeometrySpec(cornerRadius = 28.dp)),
    ) {
        Column(Modifier.selectableGroup().verticalScroll(rememberScrollState()).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            items.forEach { item ->
                val selected = item.key == selectedKey
                val color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                Column(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    .selectable(selected, enabled = item.enabled, role = Role.Tab,
                        onClick = { onItemSelected(item) }).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
                    Text(item.label, color = color, style = MiuixTheme.textStyles.footnote2)
                }
            }
        }
    }
}
