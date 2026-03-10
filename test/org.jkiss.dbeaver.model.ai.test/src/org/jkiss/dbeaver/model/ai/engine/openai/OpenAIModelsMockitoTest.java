package org.jkiss.dbeaver.model.ai.engine.openai;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class OpenAIModelsMockitoTest {

    @Test
    public void testMockedModelResolution() {

        OpenAIModels mockModels = mock(OpenAIModels.class);

        when(mockModels.getEffectiveModelName("GPT-4"))
                .thenReturn("gpt-4");

        String result = mockModels.getEffectiveModelName("GPT-4");

        assertEquals("gpt-4", result);

        verify(mockModels).getEffectiveModelName("GPT-4");
    }
}