package com.shardsoft.dubplug;

import com.atlassian.bamboo.build.test.TestCollectionResult;
import com.atlassian.bamboo.build.test.TestCollectionResultBuilder;
import com.atlassian.bamboo.build.test.TestReportProvider;
import com.atlassian.bamboo.results.tests.TestResults;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResultErrorImpl;
import com.atlassian.bamboo.resultsummary.tests.TestState;
import com.atlassian.bamboo.task.TaskContext;
import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides a TestReportProvider to parse and output the results of tests run through 'tested'.
 * This class has a life-time of a single build.
 */
public class TestedTestReporter implements TestReportProvider {

    private final String applicationOutput;
    private final Pattern linePattern;
    private final TaskContext taskContext;

    /**
     * Creates a new instance of this TestReporter with the given output.
     * This output is expected to be the results of using a ConsoleTestOutput.
     */
    public TestedTestReporter(@NotNull String applicationOutput, @NotNull TaskContext taskContext) {
        this.applicationOutput = applicationOutput;
        this.linePattern = Pattern.compile("^(PASS|FAIL) \"(.*)\" \\((.+)\\) after (\\d\\.\\d+) s(?:(.+))?");
        this.taskContext = taskContext;
    }


    @NotNull
    @Override
    public TestCollectionResult getTestCollectionResult() {
        TestCollectionResultBuilder tests = new TestCollectionResultBuilder();
        Scanner scanner = new Scanner(applicationOutput);
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            Matcher matcher = linePattern.matcher(line);
            if(matcher.matches() && matcher.groupCount() >= 4) {
                // TODO: Find a way to get JSON output, not incredibly hackish command line parsing...
                // Tested does have support for a JSON outputter, just not sure how to hook it up.
                // Since we would have a generated main. Perhaps we could just create our own.
                // Unfortunately creating our own main doesn't work if there's an existing main...
                // Of course, then neither does the auto-generated one so that's a moot point.
                // Still, a task for later.
                String status = matcher.group(1);
                TestState state;
                if(status.equalsIgnoreCase("PASS"))
                    state = TestState.SUCCESS;
                else
                    state = TestState.FAILED;
                String testName = matcher.group(2);
                String unfriendlyName = matcher.group(3);
                String className;
                if(unfriendlyName.contains(".") && unfriendlyName.indexOf(".") != unfriendlyName.length() - 1) {
                    className = unfriendlyName.substring(0, unfriendlyName.indexOf("."));
                    unfriendlyName = unfriendlyName.substring(unfriendlyName.indexOf(".") + 1);
                } else
                    className = null;
                if(Strings.isNullOrEmpty(testName))
                    testName = unfriendlyName;
                String duration = matcher.group(4);
                TestResults results = new TestResults(className, testName, duration);
                results.setState(state);
                if(state == TestState.SUCCESS)
                    tests.addSuccessfulTestResults(Arrays.asList(results));
                else
                    tests.addFailedTestResults(Arrays.asList(results));
            }
        }
        return tests.build();
    }
}
