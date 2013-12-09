package com.shardsoft.dubplug;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.process.ExternalProcessBuilder;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.utils.process.ExternalProcess;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides the task for running a dub build and retrieving unittest results.
 */
public class DubTask implements TaskType {

    // TODO: Is DUB_CAPABILITY_KEY correct? I don't see anyone else hardcoding it like this...
    // Perhaps there can be multiple labels and need a UI to select one? For now, we'll assume default.

    private static final String DUB_LABEL = "Dub";

    /** Gets the name of the expected dub executable for this platform. */
    public static final String DUB_EXEC_NAME = "dub" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");
    /** Gets the prefix, not including label, of the dub capability. */
    public static final String DUB_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".dub";
    /** Returns the full capability key for the default Dub capability. */
    public static final String DUB_CAPABILITY_KEY = DubTask.DUB_CAPABILITY_PREFIX + "." + DUB_LABEL;

    private final ProcessService processService;
    private final CapabilityContext capabilityContext;

    public DubTask(final ProcessService processService, final CapabilityContext capabilityContext) {
        this.processService = processService;
        this.capabilityContext = capabilityContext;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        final BuildLogger logger = taskContext.getBuildLogger();
        logger.addBuildLogEntry("This is a test log!");
        ExternalProcess dubProc = getProcess(taskContext);
        dubProc.execute();
        return TaskResultBuilder.newBuilder(taskContext).checkReturnCode(dubProc).build();
    }

    private ExternalProcess getProcess(@NotNull final TaskContext taskContext) {
        final String exePath = getExecutablePath(taskContext);
        List<String> commands = Lists.newArrayList(exePath);
        String[] additionalArgs = null;
        String userArgs = taskContext.getConfigurationMap().get(DubConfigurator.FIELD_ADDITIONAL_OPTIONS);
        try {
            if(!StringUtils.isEmpty(userArgs))
                additionalArgs = CommandLineUtils.translateCommandline(userArgs);
        } catch (Exception e) {
            throw new RuntimeException("Invalid command line arguments for dub.");
        }
        if(additionalArgs != null) {
            for(String arg : additionalArgs)
                commands.add(arg);
        }
        ExternalProcessBuilder builder = new ExternalProcessBuilder()
                .command(commands)
                .workingDirectory(taskContext.getWorkingDirectory());
        ExternalProcess dubProc = processService.createExternalProcess(taskContext, builder);
        return dubProc;
    }

    private String getExecutablePath(@NotNull TaskContext taskContext) {
        final String dubLabel = taskContext.getConfigurationMap().get(DubConfigurator.FIELD_DUB_LABEL);
        Preconditions.checkNotNull(dubLabel);
        final Capability capability = capabilityContext.getCapabilitySet().getCapability(DUB_CAPABILITY_PREFIX + "." + dubLabel);
        Preconditions.checkNotNull(capability, "Capability");
        return capability.getValueWithDefault();
    }
}
