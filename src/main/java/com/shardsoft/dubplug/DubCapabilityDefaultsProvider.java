package com.shardsoft.dubplug;

import com.atlassian.bamboo.v2.build.agent.capability.AbstractFileCapabilityDefaultsHelper;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a CapabilityDefaultsHelper that attempts to locate a dub install on the system.
 */
public class DubCapabilityDefaultsProvider extends AbstractFileCapabilityDefaultsHelper {

    @NotNull
    @Override
    protected String getExecutableName() {
        return DubTask.DUB_EXEC_NAME;
    }

    @NotNull
    @Override
    protected String getCapabilityKey() {
        return DubTask.DUB_CAPABILITY_KEY;
    }
}
