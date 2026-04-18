package com.edithj.desktop;

import java.util.ArrayList;
import java.util.List;

public class FakeClipboardService implements ClipboardService {

    private final List<String> writes = new ArrayList<>();
    private String text = "";
    private boolean writable = true;

    public void setText(String value) {
        this.text = value == null ? "" : value;
    }

    public void setWritable(boolean writable) {
        this.writable = writable;
    }

    public List<String> writes() {
        return new ArrayList<>(writes);
    }

    @Override
    public String readText() {
        return text;
    }

    @Override
    public boolean writeText(String value) {
        if (!writable) {
            return false;
        }
        String safe = value == null ? "" : value;
        writes.add(safe);
        text = safe;
        return true;
    }
}
