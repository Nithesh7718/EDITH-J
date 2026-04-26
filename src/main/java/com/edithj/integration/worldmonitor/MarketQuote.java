package com.edithj.integration.worldmonitor;

/**
 * Individual market symbol quote from World Monitor.
 */
public record MarketQuote(String symbol, double price, double changePercent, String trend) {

}
