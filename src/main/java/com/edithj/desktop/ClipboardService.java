package com.edithj.desktop;

public interface ClipboardService {

    String readText();

    boolean writeText(String text);
}
