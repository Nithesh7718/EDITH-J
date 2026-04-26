package com.edithj.desktop.session;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public interface FocusSessionState {

    void startFocus(Duration duration);

    Instant getFocusEndsAt();

    void endFocus();

    boolean isFocusActive(Instant now);

    void blockDomain(String domain);

    void unblockDomain(String domain);

    Set<String> blockedDomains();
}
