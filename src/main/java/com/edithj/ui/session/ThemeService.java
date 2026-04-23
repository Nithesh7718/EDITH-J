package com.edithj.ui.session;

import java.net.URL;
import java.util.Objects;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;

public final class ThemeService {

    public enum Theme {
        DARK,
        LIGHT
    }

    private static final ThemeService INSTANCE = new ThemeService();
    private static final String DARK_THEME_STYLESHEET = "/css/friday-theme.css";
    private static final String LIGHT_THEME_STYLESHEET = "/css/app-light.css";

    private final ObjectProperty<Theme> themeProperty = new SimpleObjectProperty<>(Theme.DARK);

    private ThemeService() {
    }

    public static ThemeService instance() {
        return INSTANCE;
    }

    public ObjectProperty<Theme> themeProperty() {
        return themeProperty;
    }

    public Theme currentTheme() {
        return themeProperty.get();
    }

    public void toggleTheme(Scene scene) {
        if (currentTheme() == Theme.DARK) {
            applyTheme(scene, Theme.LIGHT);
        } else {
            applyTheme(scene, Theme.DARK);
        }
    }

    public void applyTheme(Scene scene, Theme theme) {
        Objects.requireNonNull(scene, "scene");

        URL dark = ThemeService.class.getResource(DARK_THEME_STYLESHEET);
        URL light = ThemeService.class.getResource(LIGHT_THEME_STYLESHEET);
        if (dark == null || light == null) {
            return;
        }

        String darkCss = dark.toExternalForm();
        String lightCss = light.toExternalForm();

        scene.getStylesheets().remove(darkCss);
        scene.getStylesheets().remove(lightCss);
        scene.getStylesheets().add(theme == Theme.DARK ? darkCss : lightCss);
        themeProperty.set(theme);
    }
}
