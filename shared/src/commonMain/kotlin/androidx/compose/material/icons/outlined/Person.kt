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

public val Icons.Outlined.Person: ImageVector
    get() {
        if (_person != null) {
            return _person!!
        }
        _person = materialIcon(name = "Outlined.Person") {
            materialPath {
                moveTo(12.0f, 6.0f)
                curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
                reflectiveCurveToRelative(-0.9f, 2.0f, -2.0f, 2.0f)
                reflectiveCurveToRelative(-2.0f, -0.9f, -2.0f, -2.0f)
                reflectiveCurveToRelative(0.9f, -2.0f, 2.0f, -2.0f)
                moveToRelative(0.0f, 10.0f)
                curveToRelative(2.7f, 0.0f, 5.8f, 1.29f, 6.0f, 2.0f)
                lineTo(6.0f, 18.0f)
                curveToRelative(0.23f, -0.72f, 3.31f, -2.0f, 6.0f, -2.0f)
                moveToRelative(0.0f, -12.0f)
                curveTo(9.79f, 4.0f, 8.0f, 5.79f, 8.0f, 8.0f)
                reflectiveCurveToRelative(1.79f, 4.0f, 4.0f, 4.0f)
                reflectiveCurveToRelative(4.0f, -1.79f, 4.0f, -4.0f)
                reflectiveCurveToRelative(-1.79f, -4.0f, -4.0f, -4.0f)
                close()
                moveTo(12.0f, 14.0f)
                curveToRelative(-2.67f, 0.0f, -8.0f, 1.34f, -8.0f, 4.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(16.0f)
                verticalLineToRelative(-2.0f)
                curveToRelative(0.0f, -2.66f, -5.33f, -4.0f, -8.0f, -4.0f)
                close()
            }
        }
        return _person!!
    }

private var _person: ImageVector? = null
