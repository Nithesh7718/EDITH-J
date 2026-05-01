package com.edithj.integration.worldmonitor;

import java.time.Instant;

/**
 * Basic World Monitor conflict event projection used by EDITH routing.
 */
public record ConflictEvent(String id, String title, String region, String severity, Instant timestamp) {

}
