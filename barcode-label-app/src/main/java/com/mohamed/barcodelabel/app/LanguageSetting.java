package com.mohamed.barcodelabel.app;

import com.mohamed.barcodelabel.fx.i18n.UiLanguage;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Where the application keeps the user's interface language between runs. */
interface LanguageSetting {
    UiLanguage load();

    void save(UiLanguage language);

    /** The user's preferences; Arabic until the user chooses another language. */
    static LanguageSetting preferences() {
        return new LanguageSetting() {
            private final Preferences node = Preferences.userNodeForPackage(BarcodeLabelSuiteApp.class);

            @Override public UiLanguage load() {
                return UiLanguage.fromTag(node.get("language", null), UiLanguage.ARABIC);
            }

            @Override public void save(UiLanguage language) {
                node.put("language", language.tag());
                try {
                    node.flush();
                } catch (BackingStoreException ignored) {
                    // The language applies now; it is only not remembered for the next run.
                }
            }
        };
    }
}
