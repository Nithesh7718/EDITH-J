package com.edithj.integration.worldmonitor;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated world hub update payload for periodic UI/status refresh hooks.
 */
public record WorldSnapshot(Instant fetchedAt, List<ConflictEvent> conflicts, CountryInstability instability, MarketSnapshot marketSnapshot) {

}
