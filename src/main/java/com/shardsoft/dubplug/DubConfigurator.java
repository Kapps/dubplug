package com.shardsoft.dubplug;

import com.atlassian.bamboo.build.Job;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.BuildTaskRequirementSupport;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.agent.capability.Requirement;
import com.atlassian.bamboo.v2.build.agent.capability.RequirementImpl;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/** Provides configuration support for a DubTask. */
public class DubConfigurator extends AbstractTaskConfigurator implements BuildTaskRequirementSupport {

    /** The field for specifying any additional parameters to dub. */
    public static final String FIELD_ADDITIONAL_OPTIONS = "options";
    /** The field that specifies the label to use for the dub capability. */
    public static final String FIELD_DUB_LABEL = "label";

    private static final Set<String> FIELDS_TO_COPY = Sets.newHashSet(FIELD_ADDITIONAL_OPTIONS, FIELD_DUB_LABEL);
    private static final String UI_SUPPORT_FIELD = "uiConfigSupport";

    private final UIConfigSupport uiConfigSupport;

    public DubConfigurator(final UIConfigSupport uiConfigSupport) {
        this.uiConfigSupport = uiConfigSupport;
    }

    private void copyFields(final Set<String> fieldNames, final Map<String, String> output, final ActionParametersMap params) {
        for(String field : FIELDS_TO_COPY) {
            output.put(field, params.getString(field));
        }
    }

    private void addSupport(@NotNull Map<String, Object> context) {
        context.put(UI_SUPPORT_FIELD, uiConfigSupport);
    }

    @NotNull
    @Override
    public Set<Requirement> calculateRequirements(@NotNull TaskDefinition taskDefinition, @NotNull Job job) {
        final String label = taskDefinition.getConfiguration().get(FIELD_DUB_LABEL);
        return Sets.<Requirement>newHashSet(new RequirementImpl(DubTask.DUB_CAPABILITY_PREFIX + "." + label, true, ".*"));
    }

    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        copyFields(FIELDS_TO_COPY, config, params);
        return config;
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection) {
        super.validate(params, errorCollection);
        final String dubLabel = params.getString(FIELD_DUB_LABEL);
        if(StringUtils.isEmpty(dubLabel))
            errorCollection.addError(FIELD_DUB_LABEL, "The dub label must be set.");
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context) {
        super.populateContextForCreate(context);
        addSupport(context);
        context.put(FIELD_ADDITIONAL_OPTIONS, "");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        addSupport(context);
        for(String field : FIELDS_TO_COPY) {
            context.put(field, taskDefinition.getConfiguration().get(field));
        }
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition) {
        super.populateContextForView(context, taskDefinition);
        addSupport(context);
        for(String field : FIELDS_TO_COPY) {
            context.put(field, taskDefinition.getConfiguration().get(field));
        }
    }

}
