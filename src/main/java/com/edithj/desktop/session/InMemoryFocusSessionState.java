package com.edithj.desktop.session;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

public class InMemoryFocusSessionState implements FocusSessionState {

    private Instant focusEndsAt;
    private final Set<String> blockedDomains = new LinkedHashSet<>();

    @Override
    public synchronized void startFocus(Duration duration) {
        focusEndsAt = Instant.now().plus(duration);
    }

    @Override
    public synchronized Instant getFocusEndsAt() {
        return focusEndsAt;
    }

    @Override
    public synchronized void endFocus() {
        focusEndsAt = null;
    }

    @Override
    public synchronized boolean isFocusActive(Instant now) {
        return focusEndsAt != null && !focusEndsAt.isBefore(now);
    }

    @Override
    public synchronized void blockDomain(String domain) {
        blockedDomains.add(domain);
    }

    @Override
    public synchronized void unblockDomain(String domain) {
        blockedDomains.remove(domain);
    }

    @Override
    public synchronized Set<String> blockedDomains() {
        return Set.copyOf(blockedDomains);
    }
}
