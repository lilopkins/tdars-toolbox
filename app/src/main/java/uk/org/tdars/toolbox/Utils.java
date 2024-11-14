package uk.org.tdars.toolbox;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import lombok.NonNull;

public class Utils {
    /**
     * Load a {@link ResourceBundle} with a fallback locale.
     * @param baseName the base name of the resource bundle.
     * @return the {@link ResourceBundle}.
     */
    protected static @NonNull ResourceBundle loadBundleWithFallback(@NonNull String baseName) {
        try {
            // Try to load the bundle with the default locale
            return ResourceBundle.getBundle(baseName, Locale.getDefault(), Utils.class.getClassLoader());
        } catch (MissingResourceException e) {
            // Fallback to English if the default locale bundle is missing
            return ResourceBundle.getBundle(baseName, Locale.ENGLISH, Utils.class.getClassLoader());
        }
    }
}
