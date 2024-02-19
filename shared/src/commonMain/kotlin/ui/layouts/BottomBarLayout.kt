package ui.layouts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout


/*
 @Deprecated: Replacable by using the Scaffold Parameter "bottomBar"
 */
//@Composable
//fun BottomBarLayout(
//    modifier: Modifier = Modifier,
//    bottomBar: @Composable () -> Unit,
//    content: @Composable () -> Unit,
//) {
//    Layout(
//        modifier = modifier,
//        content = {
//            content()
//            bottomBar()
//        }
//    ) { measurables, constraints ->
//        // Measure the composable
//        val bottomPlaceable = measurables[1].measure(constraints.copy(
//            minHeight = 0,
//            maxHeight = constraints.maxHeight / 2,
//        ))
//        val topPlaceable = measurables[0].measure(constraints.copy(
//            minHeight = 0,
//            maxHeight = constraints.maxHeight - bottomPlaceable.height,
//        ))
//
//        // Set the size of the layout as big as it can
//        layout(constraints.maxWidth, constraints.maxHeight) {
//            // Track the y co-ord we have placed children up to
//            bottomPlaceable.placeRelative(x = 0, y = constraints.maxHeight - bottomPlaceable.height)
//            topPlaceable.placeRelative(x = 0, y = 0)
//        }
//    }
//}