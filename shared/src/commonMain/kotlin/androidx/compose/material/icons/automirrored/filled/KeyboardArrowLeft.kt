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

package androidx.compose.material.icons.automirrored.filled

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.AutoMirrored.Filled.KeyboardArrowLeft: ImageVector
    get() {
        if (_keyboardArrowLeft != null) {
            return _keyboardArrowLeft!!
        }
        _keyboardArrowLeft = materialIcon(name = "AutoMirrored.Filled.KeyboardArrowLeft", autoMirror
                = true) {
            materialPath {
                moveTo(15.41f, 16.59f)
                lineTo(10.83f, 12.0f)
                lineToRelative(4.58f, -4.59f)
                lineTo(14.0f, 6.0f)
                lineToRelative(-6.0f, 6.0f)
                lineToRelative(6.0f, 6.0f)
                lineToRelative(1.41f, -1.41f)
                close()
            }
        }
        return _keyboardArrowLeft!!
    }

private var _keyboardArrowLeft: ImageVector? = null
