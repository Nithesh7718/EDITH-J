package com.edithj.desktop;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class SystemClipboardService implements ClipboardService {

    @Override
    public String readText() {
        try {
            Object value = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            return value == null ? "" : value.toString();
        } catch (UnsupportedFlavorException | IOException | IllegalStateException exception) {
            return "";
        }
    }

    @Override
    public boolean writeText(String text) {
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            return true;
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
