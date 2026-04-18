package com.edithj.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeatherCommandHandlerTest {

    @Test
    void handle_returnsWeatherResponse() {
        WeatherCommandHandler handler = new WeatherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("weather", "weather london", "typed"));
        
        assertTrue(response != null && !response.isBlank());
    }

    @Test
    void handle_withoutLocationReturnsDefaultMessage() {
        WeatherCommandHandler handler = new WeatherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("weather", "", "typed"));
        
        assertTrue(response.toLowerCase().contains("weather") || response.toLowerCase().contains("location"));
    }

    @Test
    void intentType_returnsWeather() {
        WeatherCommandHandler handler = new WeatherCommandHandler();
        
        assertTrue(handler.intentType().toString().contains("WEATHER"));
    }

    @Test
    void handle_withBlankPayloadReturnsFriendlyMessage() {
        WeatherCommandHandler handler = new WeatherCommandHandler();
        String response = handler.handle(new CommandHandler.CommandContext("weather", "   ", "typed"));
        
        assertTrue(!response.isBlank());
    }
}

