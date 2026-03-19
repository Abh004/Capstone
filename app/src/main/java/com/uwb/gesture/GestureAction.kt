package com.uwb.gesture

enum class GestureAction(val label: String) {
    SWIPE_LR        ("Swipe L→R  : Next Track"),
    SWIPE_RL        ("Swipe R→L  : Prev Track"),
    SWIPE_UD        ("Swipe U→D  : Scroll Up"),
    SWIPE_DU        ("Swipe D→U  : Scroll Down"),
    DIAG_LR_UD      ("Diag LR-UD"),
    DIAG_LR_DU      ("Diag LR-DU"),
    DIAG_RL_UD      ("Diag RL-UD"),
    DIAG_RL_DU      ("Diag RL-DU"),
    CLOCKWISE       ("Clockwise  : Volume Up"),
    ANTICLOCKWISE   ("Anti-CW    : Volume Down"),
    INWARD_PUSH     ("Push       : Play / Pause"),
    EMPTY           ("Empty      : No Gesture"),
    UNKNOWN         ("Unknown")
}