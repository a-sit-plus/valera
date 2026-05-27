/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material.icons.outlined

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Bluetooth: ImageVector
    get() {
        if (_bluetooth != null) {
            return _bluetooth!!
        }
        _bluetooth = materialIcon(name = "Outlined.Bluetooth") {
            materialPath {
                // Outer perimeter of the Bluetooth symbol
                moveTo(17.71f, 7.71f)
                lineTo(12.0f, 2.0f)
                horizontalLineToRelative(-1.0f)
                verticalLineToRelative(7.59f)
                lineTo(6.41f, 5.0f)
                lineTo(5.0f, 6.41f)
                lineTo(10.59f, 12.0f)
                lineTo(5.0f, 17.59f)
                lineTo(6.41f, 19.0f)
                lineTo(11.0f, 14.41f)
                verticalLineTo(22.0f)
                horizontalLineToRelative(1.0f)
                lineToRelative(5.71f, -5.71f)
                curveToRelative(1.56f, -1.56f, 1.56f, -4.09f, 0.0f, -5.65f)
                lineTo(17.71f, 7.71f)
                close()
                // Inner upper triangle (creates hollow outlined appearance)
                moveTo(13.0f, 5.83f)
                lineToRelative(1.88f, 1.88f)
                lineTo(13.0f, 9.59f)
                verticalLineTo(5.83f)
                close()
                // Inner lower triangle
                moveTo(14.88f, 16.29f)
                lineTo(13.0f, 18.17f)
                verticalLineToRelative(-3.76f)
                lineTo(14.88f, 16.29f)
                close()
            }
        }
        return _bluetooth!!
    }

private var _bluetooth: ImageVector? = null