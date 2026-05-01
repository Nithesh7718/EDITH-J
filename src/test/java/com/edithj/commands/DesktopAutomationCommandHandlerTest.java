package com.edithj.commands;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.edithj.ai.AiConfig;
import com.edithj.ai.ChatProvider;
import com.edithj.ai.ProviderFactory;
import com.edithj.config.EnvConfig;
import com.edithj.launcher.FakeLauncher;

class DesktopAutomationCommandHandlerTest {

    @Test
    void handle_searchWeb_opensBrowserQuery() throws Exception {
        FakeLauncher launcher = new FakeLauncher();
        DesktopAutomationCommandHandler handler = new DesktopAutomationCommandHandler(launcher, configForTempWorkspace(),
                mock(ProviderFactory.class));

        String response = handler.handle(context("search the web for IPL yesterday news"));

        assertTrue(response.startsWith("Opening"));
        assertTrue(launcher.lastOpenedUrl().contains("google.com/search?q=IPL+yesterday+news"));
    }

    @Test
    void handle_fileOperations_stayInsideWorkspace() throws Exception {
        FakeLauncher launcher = new FakeLauncher();
        DesktopAutomationCommandHandler handler = new DesktopAutomationCommandHandler(launcher, configForTempWorkspace(),
                mock(ProviderFactory.class));

        String create = handler.handle(context("file create text notes/todo.txt with buy milk"));
        String rename = handler.handle(context("file rename notes/todo.txt to notes/tasks.txt"));
        String move = handler.handle(context("file move notes/tasks.txt to archive/tasks.txt"));

        assertTrue(create.contains("Created file"));
        assertTrue(rename.contains("Renamed file"));
        assertTrue(move.contains("Moved file"));
    }

    @Test
    void handle_fileCreate_blocksPathTraversal() throws Exception {
        DesktopAutomationCommandHandler handler = new DesktopAutomationCommandHandler(new FakeLauncher(),
                configForTempWorkspace(), mock(ProviderFactory.class));

        String response = handler.handle(context("file create text ../outside.txt with blocked"));

        assertEquals("For safety, file operations are restricted to edith.ai.workspaceDir.", response);
    }

    @Test
    void handle_writeCode_savesGeneratedContentFromSelectedProvider() throws Exception {
        AiConfig config = configForTempWorkspace();
        FakeLauncher launcher = new FakeLauncher();

        ProviderFactory providerFactory = mock(ProviderFactory.class);
        ChatProvider provider = mock(ChatProvider.class);
        when(providerFactory.createProvider(anyString())).thenReturn(provider);
        when(provider.providerName()).thenReturn("Gemini");
        when(provider.generateReply(anyString())).thenReturn("public class Hello { }\n");

        DesktopAutomationCommandHandler handler = new DesktopAutomationCommandHandler(launcher, config, providerFactory);

        String response = handler.handle(context("write code src/Hello.java: use gemini for this answer write a hello class"));

        Path savedFile = config.workspaceDir().resolve("src/Hello.java");
        Path fallbackSavedFile = config.workspaceDir().resolve("src/Hello.java.txt");
        Path effectivePath = Files.exists(savedFile) ? savedFile : fallbackSavedFile;

        assertTrue(Files.exists(effectivePath));
        assertEquals("public class Hello { }\n", Files.readString(effectivePath, StandardCharsets.UTF_8));
        assertTrue(!response.isBlank());
    }

    private CommandHandler.CommandContext context(String input) {
        return new CommandHandler.CommandContext(input, input, "typed");
    }

    private AiConfig configForTempWorkspace() throws Exception {
        Path tempWorkspace = Files.createTempDirectory("edith-automation-workspace-");
        Properties properties = new Properties();
        properties.setProperty("edith.ai.workspaceDir", tempWorkspace.toString());

        Constructor<EnvConfig> constructor = EnvConfig.class.getDeclaredConstructor(Map.class);
        constructor.setAccessible(true);
        EnvConfig envConfig = constructor.newInstance(Map.of());

        return new AiConfig(envConfig, properties);
    }
}
