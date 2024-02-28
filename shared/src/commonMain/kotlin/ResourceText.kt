enum class BuildStage(val abbreviation: String) {
    Development("D"),
    QualityAssurance("Q"),
    Production("P");
}

object Resources {
    val BUILD_FOR_STAGE = BuildStage.Development.abbreviation
    const val BUILD_VERSION = "0.0.1"
    const val BUILD_NUMBER = "42"

    const val ADD_ID_TEXT = "To add an ID, login on https://abcd.at/xyz/ with a secondary device and scan the displayed QR code."
    const val BUTTON_SCAN_QR = "Scan QR-Code"
    const val BUTTON_LOGIN_ID_AUSTRIA = "Login with ID Austria"
    const val CHOOSE_NEXT_STEP = "Choose next step"
    const val FIRST_NAME = "First name"
    const val LAST_NAME = "Last name"
    const val BIRTH_DATE = "Date of Birth"
    const val BUTTON_DELETE = "Delete"
    const val BUTTON_CLOSE = "Close"
    const val BUTTON_CONTINUE = "Continue"
    const val ICONS_FROM = "Icons from"
    const val PICTURES_FROM = "Pictures from"
    const val VERSION = "Version"
    const val CAMERA_ACCESS_DENIED = "Camera access denied"
    const val CREDENTIAL = "Credential"
    const val DEMO_WALLET = "DemoWallet"
    const val ID_AUSTRIA_CREDENTIAL = "IDAustria Credential"
    const val WALLET = "Wallet"
    const val ADD_ID = "Add ID"
    const val COMPOSE_WALLET = "Compose Wallet"
    const val DEMO_APP = "Demo App"
    const val RESET_APP = "Reset App"
    const val WARNING = "Warning"
    const val RESET_APP_ALERT_TEXT = "Do you really want to reset the App?"
    const val BUTTON_CONFIRM = "Confirm"
    const val BUTTON_DISMISS = "Dismiss"
    const val DATASTORE_KEY_VCS = "VCs"
    const val DATASTORE_KEY_XAUTH = "xauth"
    const val DATASTORE_KEY_COOKIES = "cookies"
    const val SNACKBAR_CREDENTIAL_LOADED_SUCCESSFULLY = "Credential loaded successfully"
    const val SNACKBAR_RESET_APP_SUCCESSFULLY = "Reset App successfully"
    const val DEBUG_DATASTORE_KEY = "DBGKEY"
    const val DEBUG_DATASTORE_VALUE = "DBGVALUE"
    const val IOS_TEST_VALUE = "TESTVALUE"
    const val DATASTORE_KEY_CONFIG = "config"
    const val BUTTON_SHOW_LOG = "Show Log"
    const val BUTTON_EXIT_APP = "Exit App"
    const val LOGIN = "Login"
    const val BUTTON_CANCEL = "Cancel"
    const val BUTTON_ACCEPT = "Accept"
    const val LOGIN_TERMINAL_MACHINE = "Login to terminal or machine"
    const val RECIPIENT = "Recipient"
    const val NAME = "Name"
    const val LOCATION = "Location"
    const val REQUESTED_DATA = "Requested data"
    const val TRANSMIT_THIS_INFORMATION = "Would you like to transmit this information?"
    const val UNKNOWN_EXCEPTION = "Unknown exception"

    const val APP_DISPLAY_NAME = "Wallet Demo App"

    const val BUTTON_LABEL_ACCEPT = "Akzeptieren"
    const val BUTTON_LABEL_CANCEL = "Abbrechen"
    const val BUTTON_LABEL_CONSENT = "Zustimmen"
    const val BUTTON_LABEL_CONTINUE = "Weiter"
    const val BUTTON_LABEL_DETAILS = "Details"
    const val BUTTON_LABEL_SHARE = "Teilen"
    const val BUTTOM_LABEL_LOAD_DATA = "Daten laden"
    const val BUTTOM_LABEL_RELOAD_DATA = "Daten nachladen"
    const val BUTTON_LABEL_START = "Starten"
    const val BUTTON_LABEL_SAVE = "Speichern"

    const val HEADING_LABEL_INFORMATION = "Information"
    const val HEADING_LABEL_NAVIGATE_BACK = "Zurück"
    const val HEADING_LABEL_TERMS_OF_USE = "Nutzungsbedingungen"
    const val HEADING_LABEL_DATA_PROTECTION = "Datenschutz"
    const val HEADING_LABEL_TERMS_OF_USE_AND_DATA_PROTECTION = "$HEADING_LABEL_TERMS_OF_USE & $HEADING_LABEL_DATA_PROTECTION"
//    const val HEADING_LABEL_PERMISSIONS = "Berechtigungen"
    const val HEADING_LABEL_LOAD_DATA = "Daten laden"

    const val DESCRIPTION_READ_TERMS = "Bitte lesen und akzeptieren Sie die Nutzungsbedingungen und Datenschutzerklärung."


    const val CONTENT_DESCRIPTION_NAVIGATE_BACK = "Zurück"
    const val CONTENT_DESCRIPTION_SAVE_BUTTON = "Speichern"
    const val CONTENT_DESCRIPTION_EDIT_BUTTON = "Bearbeiten"

    // files
    val ONBOARDING_START_SCREEN_BACKGROUND_FILENAME = "onboardingBackground.jpg"
}