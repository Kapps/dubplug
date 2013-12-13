[#-- @ftlvariable name="uiConfigSupport" type="com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport" --]
[#assign addExecutableLink][@ui.displayAddExecutableInline executableKey='dub' /][/#assign]
[@ww.select cssClass="builderSelectWidget" labelKey='dub.label' name='label'
    list=uiConfigSupport.getExecutableLabels('dub') extraUtility=addExecutableLink required='true' /]
[@ww.checkbox labelKey='dub.runTests' name='runtests' toggle='true' /]
[@ww.checkbox labelKey='dub.buildRelease' name='runbuild' toggle='true' /]
[@ww.textfield labelKey='dub.additionalOptions' name='options' cssClass="long-field" /]