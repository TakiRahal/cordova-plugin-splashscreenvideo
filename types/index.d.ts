interface Navigator {
    /** This plugin displays and hides a splash screen during application launch. */
    splashscreenvideo: {
        /** Dismiss the splash screen. */
        hide(): void;
        /** Displays the splash screen. */
        show(): void;
    }
}