package com.shardsoft.dubplug;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.build.test.TestCollationService;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.agent.capability.Capability;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityDefaultsHelper;
import com.atlassian.utils.process.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Provides the task for running a dub build and retrieving unittest results.
 */
public class DubTask implements TaskType {

    private static final String DUB_LABEL = "Dub";

    /** Gets the name of the expected dub executable for this platform. */
    public static final String DUB_EXEC_NAME = "dub" + (SystemUtils.IS_OS_WINDOWS ? ".exe" : "");
    /** Gets the prefix, not including label, of the dub capability. */
    public static final String DUB_CAPABILITY_PREFIX = CapabilityDefaultsHelper.CAPABILITY_BUILDER_PREFIX + ".dub";
    /** Returns the full capability key for the default Dub capability. */
    public static final String DUB_CAPABILITY_KEY = DubTask.DUB_CAPABILITY_PREFIX + "." + DUB_LABEL;

    private final TestCollationService testCollationService;
    private final ProcessService processService;
    private final CapabilityContext capabilityContext;

    public DubTask(final ProcessService processService, final CapabilityContext capabilityContext, final TestCollationService testCollationService) {
        this.processService = processService;
        this.capabilityContext = capabilityContext;
        this.testCollationService = testCollationService;
    }

    @NotNull
    @Override
    public TaskResult execute(@NotNull TaskContext taskContext) throws TaskException {
        // TODO: Check dub describe for a dependency on tested.
        // Then if it's not there, ideally parse for an assertion error.
        // Or probably just parse for an assertion error and tested output at same time.
        // For now we'll worry just about tested though.
        // Could also just make a fake project that has a dependency on this and tested.
        // Then tested would be used and all would be well.
        // Except maybe not for executables... not sure...
        // And would need to specify local without changing system settings with add-local.

        boolean runBuild = taskContext.getConfigurationMap().getAsBoolean(DubConfigurator.FIELD_DUB_RUN_BUILD);
        boolean runTests = taskContext.getConfigurationMap().getAsBoolean(DubConfigurator.FIELD_DUB_RUN_TESTS);
        boolean testsFailed = false;
        TaskResultBuilder builder = TaskResultBuilder.newBuilder(taskContext);
        ExternalProcess testProcess = null, buildProcess = null;
        // First run the tests.
        if(runTests) {
            testProcess = runProcess(taskContext, BuildMode.test);
            builder = builder.checkReturnCode(testProcess);
        }
        // Then run the build only if those succeed (otherwise report compilation errors multiple times, and wasteful).
        // Plus the point is to generate artifacts, and we don't really want to generate artifacts for broken builds.
        // Then again, that may be a Bamboo setting, so should possibly allow it and let Bamboo handle it.
        if(runBuild) {
            if(testProcess != null && !testProcess.getHandler().succeeded())
                taskContext.getBuildLogger().addErrorLogEntry("Skipping generating release because tests failed.");
            else {
                buildProcess = runProcess(taskContext, BuildMode.release);
                builder = builder.checkReturnCode(buildProcess);
            }
        }
        return builder.build();
    }

    private final ExternalProcess runProcess(@NotNull final TaskContext taskContext, BuildMode mode) {
        final BuildLogger logger = taskContext.getBuildLogger();
        logger.addBuildLogEntry("Preparing to run " + mode + " build.");
        StringOutputHandler outputHandler = new StringOutputHandler();
        StringOutputHandler errorHandler = new StringOutputHandler();
        Collection<String> args = getArgs(mode);
        ExternalProcess dubProc = getProcess(taskContext, args, outputHandler, errorHandler);
        dubProc.execute();
        String applicationOutput = outputHandler.getOutput();
        logger.addBuildLogEntry("---Build Output---\r\n" + applicationOutput + "------------------");
        String errorOutput = errorHandler.getOutput();
        if(!Strings.isNullOrEmpty(errorOutput))
            logger.addErrorLogEntry(errorOutput);
        if(mode == BuildMode.test) {
            // We want to still process tests even though dub did not return 0
            // as if we had an assertion failure or such dub would return non-zero since the application does.
            TestedTestReporter testReporter = new TestedTestReporter(applicationOutput, taskContext);
            testCollationService.collateTestResults(taskContext, testReporter);
        }
        return dubProc;
    }

    private Collection<String> getArgs(BuildMode mode) {
        if(mode == BuildMode.release)
            return Arrays.asList("build", "--build=release");
        else
            return Arrays.asList("test");
    }

    private ExternalProcess getProcess(@NotNull final TaskContext taskContext, Collection<String> args, OutputHandler outputHandler, OutputHandler errorHandler) {
        final String exePath = getExecutablePath(taskContext);
        List<String> commands = Lists.newArrayList(exePath);
        if(args != null)
            commands.addAll(args);
        String[] additionalArgs = null;
        String userArgs = taskContext.getConfigurationMap().get(DubConfigurator.FIELD_ADDITIONAL_OPTIONS);
        try {
            if(!StringUtils.isEmpty(userArgs))
                additionalArgs = CommandLineUtils.translateCommandline(userArgs);
        } catch (Exception e) {
            throw new RuntimeException("Invalid command line arguments for dub.");
        }
        if(additionalArgs != null)
            commands.addAll(Arrays.asList(additionalArgs));
        ExternalProcessBuilder builder = new ExternalProcessBuilder()
                .command(commands, taskContext.getWorkingDirectory())
                .handlers(outputHandler, errorHandler);
        return builder.build();
    }

    private String getExecutablePath(@NotNull TaskContext taskContext) {
        final String dubLabel = taskContext.getConfigurationMap().get(DubConfigurator.FIELD_DUB_LABEL);
        Preconditions.checkNotNull(dubLabel);
        final Capability capability = capabilityContext.getCapabilitySet().getCapability(DUB_CAPABILITY_PREFIX + "." + dubLabel);
        Preconditions.checkNotNull(capability, "Capability");
        return capability.getValueWithDefault();
    }

    private enum BuildMode {
        test,
        release
    }
}
