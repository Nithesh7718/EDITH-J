package com.edithj.integration.worldmonitor;

import java.time.Instant;
import java.util.List;

/**
 * Market snapshot projection used by EDITH ASK_WORLD_MARKETS responses.
 */
public record MarketSnapshot(Instant asOf, List<MarketQuote> quotes) {

}
