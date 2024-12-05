package ui.viewmodels

import at.asitplus.dif.ConstraintField
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.RequestOptionParameters

class AuthenticationAttributesSelectionViewModel (val navigateUp: () -> Unit,
                                                  val requests: Map<String, Pair<RequestOptionParameters,Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>>>,
                                                  val selectedCredentials: Map<String, SubjectCredentialStore. StoreEntry>,
                                                  val selectAttributes: (Map<String, Set<NormalizedJsonPath>>) -> Unit,){
}