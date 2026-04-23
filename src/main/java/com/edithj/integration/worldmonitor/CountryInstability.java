package com.edithj.integration.worldmonitor;

/**
 * Country instability/risk projection from World Monitor CII responses.
 */
public record CountryInstability(String isoCode, double score, String level, String summary) {

}
