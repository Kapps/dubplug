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
    private final Pattern unfriendlyNamePattern;
    private final TaskContext taskContext;

    /**
     * Creates a new instance of this TestReporter with the given output.
     * This output is expected to be the results of using a ConsoleTestOutput.
     */
    public TestedTestReporter(@NotNull String applicationOutput, @NotNull TaskContext taskContext) {
        this.applicationOutput = applicationOutput;
        this.linePattern = Pattern.compile("^(PASS|FAIL) \"(.*)\" \\((.+)\\) after (\\d\\.\\d+) s(?:(.+))?");
        this.unfriendlyNamePattern = Pattern.compile("(?:.+\\.)?(.+)\\.__unittestL(\\d+)_(\\d+)");
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
                Matcher unfriendlyMatcher = Strings.isNullOrEmpty(unfriendlyName) ? null : unfriendlyNamePattern.matcher(unfriendlyName);
                if(unfriendlyMatcher != null && unfriendlyMatcher.matches() && unfriendlyMatcher.groupCount() >= 3) {
                    // Last part of name, start line number, test number.
                    className = unfriendlyMatcher.group(1);
                    String lineNumber = unfriendlyMatcher.group(2);
                    unfriendlyName = className + ":" + lineNumber;
                } else
                    className = "Unknown";
                if(Strings.isNullOrEmpty(testName))
                    testName = unfriendlyName;
                String duration = matcher.group(4);
                TestResults results = new TestResults(className, testName, duration);
                results.setState(state);
                // TODO: If we can use JSON as output and get the stack trace, that would be nice.
                // Also, does setSystemOut actually do anything?
                if(matcher.groupCount() >= 5)
                    results.setSystemOut(matcher.group(5));
                if(state == TestState.SUCCESS)
                    tests.addSuccessfulTestResults(Arrays.asList(results));
                else
                    tests.addFailedTestResults(Arrays.asList(results));
            }
        }
        return tests.build();
    }
}
