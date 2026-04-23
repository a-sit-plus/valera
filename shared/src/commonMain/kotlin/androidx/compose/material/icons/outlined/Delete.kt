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

public val Icons.Outlined.Delete: ImageVector
    get() {
        if (_delete != null) {
            return _delete!!
        }
        _delete = materialIcon(name = "Outlined.Delete") {
            materialPath {
                moveTo(16.0f, 9.0f)
                verticalLineToRelative(10.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(9.0f)
                horizontalLineToRelative(8.0f)
                moveToRelative(-1.5f, -6.0f)
                horizontalLineToRelative(-5.0f)
                lineToRelative(-1.0f, 1.0f)
                horizontalLineTo(5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(4.0f)
                horizontalLineToRelative(-3.5f)
                lineToRelative(-1.0f, -1.0f)
                close()
                moveTo(18.0f, 7.0f)
                horizontalLineTo(6.0f)
                verticalLineToRelative(12.0f)
                curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
                horizontalLineToRelative(8.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                verticalLineTo(7.0f)
                close()
            }
        }
        return _delete!!
    }

private var _delete: ImageVector? = null
