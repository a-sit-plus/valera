package at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core

import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment

operator fun NormalizedJsonPath.plus(segment: NormalizedJsonPathSegment) = this + NormalizedJsonPath(segment)

operator fun NormalizedJsonPath.plus(segment: String) = this + NormalizedJsonPathSegment.NameSegment(segment)
operator fun NormalizedJsonPath.plus(index: UInt) = this + NormalizedJsonPathSegment.IndexSegment(index)
