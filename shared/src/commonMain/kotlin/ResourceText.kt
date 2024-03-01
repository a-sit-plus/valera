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

    const val BUTTON_LABEL_AUTHENTICATE = "Anmelden"
    const val BUTTON_LABEL_SHOW_DATA = "Vorzeigen"
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
    const val BUTTON_LABEL_CONCLUDE = "Abschließen"
    const val BUTTON_LABEL_SCAN_QR_CODE = "QR-Code scannen"
    const val BUTTON_LABEL_FAQ = "FAQ"
    const val BUTTON_LABEL_LICENSES = "Lizenzen"
    const val BUTTON_LABEL_DATA_PROTECTION_POLICY = "Datenschutzrichtlinie"
    const val BUTTON_LABEL_SHARE_LOG_FILE = "Log-Datei teilen"
    const val BUTTON_LABEL_RESET_APP = "App zurücksetzen"

    const val ATTRIBUTE_FRIENDLY_NAME_BPK = "BPK"
    const val ATTRIBUTE_FRIENDLY_NAME_FIRSTNAME = "Vorname"
    const val ATTRIBUTE_FRIENDLY_NAME_LASTNAME = "Nachname"
    const val ATTRIBUTE_FRIENDLY_NAME_DATE_OF_BIRTH = "Geburtsdatum"
    const val ATTRIBUTE_FRIENDLY_NAME_PORTRAIT = "Portrait"
    const val ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_14 = "Zumindest 14 Jahre alt"
    const val ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_16 = "Zumindest 16 Jahre alt"
    const val ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_18 = "Zumindest 18 Jahre alt"
    const val ATTRIBUTE_FRIENDLY_NAME_AGE_AT_LEAST_21 = "Zumindest 21 Jahre alt"
    const val ATTRIBUTE_FRIENDLY_NAME_MAIN_ADDRESS = "Hauptwohnsitz"

    const val ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_NAME = "Name"
    const val ATTRIBUTE_FRIENDLY_NAME_DATA_RECIPIENT_LOCATION = "Hauptwohnsitz"

    const val NAVIGATION_BUTTON_LABEL_SHOW_DATA = "Daten Vorzeigen"
    const val NAVIGATION_BUTTON_LABEL_MY_DATA = "Meine Daten"
    const val NAVIGATION_BUTTON_LABEL_SETTINGS = "Einstellungen"

    const val TEXT_LABEL_ISSUING_SERVICE = "Aussteller"
    const val TEXT_LABEL_ID_FORMAT = "ID-Format"
    const val ID_FORMAT_PLAIN_JWT_LABEL = "PLAIN_JWT"
    const val ID_FORMAT_SD_JWT_LABEL = "SD-JWT"
    const val ID_FORMAT_ISO_MDOC_LABEL = "ISO_MDOC"

    const val TEXT_LABEL_STAGE = "Stage"
    const val TEXT_LABEL_VERSION = "Version"

    const val HEADING_LABEL_AUTHENTICATE_AT_DEVICE_TITLE = "Anmelden"
    const val HEADING_LABEL_AUTHENTICATE_AT_DEVICE_SUBTITLE = "an Schalter oder Maschine"
    const val HEADING_LABEL_SHOW_DATA = "Daten Vorzeigen"
    const val HEADING_LABEL_AUTHENTICATION_SUCCESS = "Angemeldet"
    const val HEADING_LABEL_SETTINGS_SCREEN = "Einstellungen"
    const val HEADING_LABEL_ERROR_SCREEN = "Error"
    const val HEADING_LABEL_LOADING_SCREEN = "Loading"
    const val HEADING_LABEL_CREDENTIAL_SCANNER_SCREEN = "Credential Scanner"
    const val HEADING_LABEL_LOG_SCREEN = "Log"
    const val HEADING_LABEL_INFORMATION = "Information"
    const val HEADING_LABEL_MY_DATA_OVERVIEW = "Meine Daten"
    const val HEADING_LABEL_NAVIGATE_BACK = "Zurück"
    const val HEADING_LABEL_TERMS_OF_USE = "Nutzungsbedingungen"
    const val HEADING_LABEL_DATA_PROTECTION = "Datenschutz"
    const val HEADING_LABEL_TERMS_OF_USE_AND_DATA_PROTECTION = "$HEADING_LABEL_TERMS_OF_USE & $HEADING_LABEL_DATA_PROTECTION"
    const val HEADING_LABEL_LOAD_DATA = "Daten laden"
    const val HEADING_LABEL_AUTHENTICATE_AT_DEVICE = "Anmelden an\nSchalter oder Maschine"

    const val DESCRIPTION_READ_TERMS = "Bitte lesen und akzeptieren Sie die Nutzungsbedingungen und Datenschutzerklärung."

    const val BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_TITLE = "Biometrische Authentifizierung"
    const val BIOMETRIC_AUTHENTICATION_PROMPT_FOR_DATA_TRANSMISSION_CONSENT_SUBTITLE = "Anmelden bei"
    const val BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_TITLE = "Biometrische Authentifizierung"
    const val BIOMETRIC_AUTHENTICATION_PROMPT_TO_LOAD_DATA_SUBTITLE = "Daten laden"

    const val CONTENT_DESCRIPTION_NAVIGATE_BACK = "Zurück"
    const val CONTENT_DESCRIPTION_PORTRAIT = "Portrait"
    const val CONTENT_DESCRIPTION_REFRESH_CREDENTIALS = "Daten aktualisieren"

    const val PROMPT_ASK_LOAD_MISSING_DATA = "Sollen die fehlenden Daten nachgeladen werden?"
    const val PROMPT_SEND_ABOVE_DATA = "Sollen diese Daten übermittelt werden?"
    const val PROMPT_SEND_ALL_DATA = "Sollen alle Daten übermittelt werden?"

    const val ERROR_FEATURE_NOT_YET_AVAILABLE = "Incomplete Implementation"
    const val ERROR_AUTHENTICATION_AT_SP_FAILED = "Authentication failed"
    const val ERROR_BIOMETRIC_ERROR_NO_HARDWARE = "ERROR_BIOMETRIC_ERROR_NO_HARDWARE"
    const val ERROR_BIOMETRIC_ERROR_HW_UNAVAILABLE = "BIOMETRIC_ERROR_HW_UNAVAILABLE"
    const val ERROR_BIOMETRIC_ERROR_UNSUPPORTED = "BIOMETRIC_ERROR_UNSUPPORTED"
    const val ERROR_BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED = "BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED"
    const val ERROR_BIOMETRIC_STATUS_UNKNOWN = "BIOMETRIC_STATUS_UNKNOWN"
    const val ERROR_BIOMETRIC_ERROR_NONE_ENROLLED = "BIOMETRIC_ERROR_NONE_ENROLLED"
    const val ERROR_BIOMETRIC_ERROR_UNKNOWN = "ERROR_BIOMETRIC_ERROR_UNKNOWN"

    const val INFO_TEXT_SHOW_DATA_SITUATION = "In welcher Situation möchten Sie Ihre Daten vorzeigen?"
    const val INFO_TEXT_IRREVOCABLE_ERROR = "Der Fehler kann nicht behoben werden."
    const val INFO_TEXT_RESTART_APP = "Bitte starten Sie die App erneut."
    const val INFO_TEXT_DATA_ITEMS_MISSING = "fehlend"
    const val INFO_TEXT_AUTHENTICATION_SUCCESS = "Die Anmeldung wurde erfolgreich durchgeführt."
    const val INFO_TEXT_REDICRECTION_TO_ID_AUSTRIA_FOR_CREDENTIAL_PROVISIONING = "Zur Abfrage Ihrer Daten werden Sie zu ID Austria weitergeleitet."
    const val INFO_TEXT_NOTICE_DEVELOPMENT_PROVISIONING_USING_QR_CODE_CREDENTIALS = "Zu Entwicklungszwecken gibt es auch die Möglichkeit, Daten über einen QR-Code zu laden."
    const val INFO_TEXT_ENTHUSIASTIC_WELCOME_END = "Daten laden und los geht's!"

    const val SECTION_HEADING_AUTHENTICATE_AT_DEVICE_TITLE = "Anmelden an"
    const val SECTION_HEADING_AUTHENTICATE_AT_DEVICE_SUBTITLE = "Schalter oder Maschine"
    const val SECTION_HEADING_SHOW_DATA_TO_EXECUTIVE_TITLE = "Daten vorzeigen an"
    const val SECTION_HEADING_SHOW_DATA_TO_EXECUTIVE_SUBTITLE = "Exekutive"
    const val SECTION_HEADING_SHOW_DATA_TO_OTHER_CITIZEN_TITLE = "Daten vorzeigen an"
    const val SECTION_HEADING_SHOW_DATA_TO_OTHER_CITIZEN_SUBTITLE = "dritte Bürger"
    const val SECTION_HEADING_ACTIONS = "Aktionen"
    const val SECTION_HEADING_INFORMATION = "Information"
    const val SECTION_HEADING_CONFIGURATION = "Konfiguration"
    const val SECTION_HEADING_REQUESTED_DATA = "Angefragte Daten"
    const val SECTION_HEADING_DATA_RECIPIENT = "Empfänger"
    const val SECTION_HEADING_ADMISSION_DATA = "Zulassungsdaten"
    const val SECTION_HEADING_ADMISSION_DATA_ICON_TEXT = "Z"
    const val SECTION_HEADING_AGE_DATA = "Altersstufen"
    const val SECTION_HEADING_AGE_DATA_ICON_TEXT = "AS"
    const val SECTION_HEADING_DRIVING_PERMISSION_DATA = "Lenkberechtigung"
    const val SECTION_HEADING_DRIVING_PERMISSION_DATA_ICON_TEXT = "LB"
    const val SECTION_HEADING_IDENTITY_DATA = "Identitätsdaten"
    const val SECTION_HEADING_IDENTITY_DATA_ICON_TEXT = "ID"
    const val SECTION_HEADING_RESIDENCE_DATA = "Meldeadresse"
    const val SECTION_HEADING_RESIDENCE_DATA_ICON_TEXT = "MA"

    const val ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_ICON_TEXT = "1"
    const val ONBOARDING_SECTION_TERMS_AND_DATA_PROTECTION_TITLE = "Nutzungsbedingungen und Datenschutz"
    const val ONBOARDING_SECTION_LOAD_DATA_ICON_TEXT = "2"
    const val ONBOARDING_SECTION_LOAD_DATA_TITLE = "Daten in die App laden"
    const val ONBOARDING_SECTION_LOAD_DATA_SUBTITLE = "mit einmaligem Kontakt zum zentralen ID Austria"
    const val ONBOARDING_SECTION_SHOW_DATA_ICON_TEXT = "3"
    const val ONBOARDING_SECTION_SHOW_DATA_TITLE = "Daten vorzeigen"
    const val ONBOARDING_SECTION_SHOW_DATA_SUBTITLE = "direkt aus dem Speicher der App"
    const val ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_ICON_TEXT = "A"
    const val ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_TITLE = "am Schalter oder bei Maschinen"
    const val ONBOARDING_SECTION_DATA_USAGE_AUTHENTICATE_AT_MASHINE_SUBTITLE = "bspw. bei Amtswegen"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_ICON_TEXT = "B"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_TITLE = "gegenüber der Exekutive"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_EXECUTIVE_SUBTITLE = "bspw. bei Verkehrskontrollen"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_ICON_TEXT = "C"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_TITLE = "gegenüber dritten Bürgern"
    const val ONBOARDING_SECTION_DATA_USAGE_SHOW_OTHER_CITIZEN_SUBTITLE = "bspw. Veranstaltungen"

    const val WARNING_REQUESTED_DATA_NOT_AVAILABLE_HEADING = "Angefragte Daten nicht verfügbar"
    const val WARNING_REQUESTED_DATA_NOT_AVAILABLE_CONTENT = "Nicht alle angefragten Daten wurden bereits in die App geladen.\nFehlende Daten können über ID Austria nachgeladen werden."

    // files
    val ONBOARDING_START_SCREEN_BACKGROUND_FILENAME = "onboardingBackground.jpg"
}