package com.edithj.commands;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.edithj.launcher.FakeLauncher;

class WhatsAppCommandHandlerTest {

    @Test
    void parseRequest_extractsMessageWithoutContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed = handler.parseRequest("whatsapp hello");

        assertEquals("hello", parsed.message());
        assertTrue(parsed.contactName().isBlank());
    }

    @Test
    void parseRequest_extractsQuotedMessageAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("send a \"hello\" message to Krithick via whatsapp");

        assertEquals("hello", parsed.message());
        assertEquals("Krithick", parsed.contactName());
        assertTrue(!parsed.callIntent());
    }

    @Test
    void parseRequest_openAndSendExtractsCleanMessageAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("open whatsapp and send hi to krithick");

        assertEquals("hi", parsed.message());
        assertEquals("krithick", parsed.contactName());
        assertTrue(!parsed.callIntent());
    }

    @Test
    void parseRequest_typoSendVerbStillExtractsMessageAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("sen HI to krithick");

        assertEquals("HI", parsed.message());
        assertEquals("krithick", parsed.contactName());
        assertTrue(!parsed.callIntent());
    }

    @Test
    void parseRequest_detectsWhatsappCallIntentAndContact() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        WhatsAppCommandHandler.ParsedWhatsAppRequest parsed
                = handler.parseRequest("make a call to krithick via whatsapp");

        assertTrue(parsed.callIntent());
        assertEquals("krithick", parsed.contactName());
        assertEquals("", parsed.message());
    }

    @Test
    void handle_openCommandLaunchesWhatsappApp() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("whatsapp", "whatsapp", "typed"));

        assertTrue(response.contains("WhatsApp"));
        assertTrue(launcherService.lastOpenedUrl().startsWith("whatsapp://send"));
    }

    @Test
    void handle_buildsWhatsAppAppTargetAndLaunchesIt() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext("whatsapp hello world", "whatsapp hello world", "typed"));

        assertTrue(launcherService.lastOpenedUrl().startsWith("whatsapp://send?text=hello%20world"));
        assertTrue(response.contains("Opening WhatsApp in the app with your message: \"hello world\"."));
    }

    @Test
    void handle_typoVariantStillLaunchesWhatsApp() {
        FakeLauncher launcherService = new FakeLauncher();
        // Use empty Properties so real edith.properties contact mappings don't interfere.
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService, new Properties());

        String response = handler.handle(new CommandHandler.CommandContext(
                "open whtsapp and send hi to krithick",
                "open whtsapp and send hi to krithick",
                "typed"));

        assertTrue(launcherService.lastOpenedUrl().startsWith("whatsapp://send?text=hi"));
        assertTrue(response.contains("to krithick"));
        assertTrue(response.contains("couldn't auto-select"));
    }

    @Test
    void handle_withMappedRecipientLaunchesDirectChat() {
        FakeLauncher launcherService = new FakeLauncher();
        Properties properties = new Properties();
        properties.setProperty("edith.whatsapp.contact.krithick", "+919876543210");
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService, properties);

        String response = handler.handle(new CommandHandler.CommandContext(
                "open whatsapp and send hi to krithick",
                "open whatsapp and send hi to krithick",
                "typed"));

        assertTrue(launcherService.lastOpenedUrl().startsWith("whatsapp://send?phone=919876543210&text=hi"));
        assertTrue(response.contains("to krithick"));
        assertTrue(!response.contains("couldn't auto-select"));
    }

    @Test
    void handle_callIntent_returnsLimitationAndDoesNotLaunchWhatsApp() {
        FakeLauncher launcherService = new FakeLauncher();
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(launcherService);

        String response = handler.handle(new CommandHandler.CommandContext(
                "make a call to Krithick via whatsapp",
                "make a call to Krithick via whatsapp",
                "typed"));

        assertTrue(response.contains("can’t start WhatsApp calls") || response.contains("can't start WhatsApp calls"));
        assertTrue(response.contains("Krithick"));
        assertTrue(launcherService.lastOpenedTarget().isBlank());
    }

    @Test
    void buildWhatsAppAppTarget_encodesSpacesAsPercentTwenty() {
        WhatsAppCommandHandler handler = new WhatsAppCommandHandler(new FakeLauncher());

        assertEquals("whatsapp://send?text=hello%20world", handler.buildWhatsAppAppTarget("hello world"));
    }

}
