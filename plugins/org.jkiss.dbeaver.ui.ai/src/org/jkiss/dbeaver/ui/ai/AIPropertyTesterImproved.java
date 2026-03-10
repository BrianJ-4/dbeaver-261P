package org.jkiss.dbeaver.ui.ai;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;

public class AIPropertyTesterImproved extends PropertyTester {

    private final AISettingsManager settingsManager;

    public AIPropertyTesterImproved(AISettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        if (AIPropertyTester.PROP_IS_DISABLED.equals(property)) {
            return settingsManager.getSettings().isAiDisabled();
        }

        return false;
    }
}