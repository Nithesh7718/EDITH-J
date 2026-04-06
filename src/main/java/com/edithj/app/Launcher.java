package com.edithj.app;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        ApplicationLauncher.launch(args);
    }

    private static final class ApplicationLauncher {

        private static void launch(String[] args) {
            EdithApplication.launch(EdithApplication.class, args);
        }
    }
}
