<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=messagesPerField.exists('global'); section>

  <#if section = "header">
    <div class="form-header">
      <div class="form-kicker">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" width="11" height="11">
          <path fill-rule="evenodd" d="M8 1a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7ZM4.5 8A2.5 2.5 0 0 0 2 10.5v.5a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-.5A2.5 2.5 0 0 0 11.5 8h-7Z" clip-rule="evenodd"/>
        </svg>
        Account Details
      </div>
      <h1 class="form-heading">${msg("loginProfileTitle")}</h1>
      <p class="form-subheading">Please confirm your details to continue.</p>
    </div>

  <#elseif section = "form">
    <form id="kc-update-profile-form" class="cms-form" action="${url.loginAction}" method="post">

      <#assign currentGroup=""/>
      <#list profile.attributes as attribute>
        <#assign group = (attribute.group)!"">
        <#if group != currentGroup>
          <#assign currentGroup=group>
          <#if currentGroup != "">
            <#assign groupDisplayHeader=(group.displayHeader)!"">
            <div class="form-header" style="margin-bottom:0.875rem">
              <h2 class="field-label" style="font-size:0.9375rem;color:#fff">
                <#if groupDisplayHeader != ""><#assign groupHeaderText=advancedMsg(groupDisplayHeader)!group><#else><#assign groupHeaderText=group.name!""></#if>
                ${groupHeaderText}
              </h2>
            </div>
          </#if>
        </#if>

        <div class="field-group <#if messagesPerField.existsError('${attribute.name}')>field-group--error</#if>">
          <label class="field-label" for="${attribute.name}">
            ${advancedMsg(attribute.displayName!'')}<#if attribute.required> *</#if>
          </label>
          <div class="field-input-wrap">
            <#if attribute.annotations.inputType!'' == 'textarea'>
              <textarea
                id="${attribute.name}"
                name="${attribute.name}"
                class="field-input"
                style="height:auto;padding:0.75rem 1rem"
                aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
                <#if attribute.readOnly>disabled</#if>
              >${(attribute.value!'')}</textarea>
            <#elseif (attribute.annotations.inputType!'') == 'select' || (attribute.annotations.inputType!'') == 'multiselect'>
              <#if attribute.annotations.inputOptionsFromValidation?? && attribute.validators[attribute.annotations.inputOptionsFromValidation]?? && attribute.validators[attribute.annotations.inputOptionsFromValidation].options??>
                <#assign options=attribute.validators[attribute.annotations.inputOptionsFromValidation].options>
              <#elseif attribute.validators.options?? && attribute.validators.options.options??>
                <#assign options=attribute.validators.options.options>
              <#else>
                <#assign options=[]>
              </#if>
              <select
                id="${attribute.name}"
                name="${attribute.name}"
                class="field-input"
                aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
                <#if attribute.readOnly>disabled</#if>
                <#if attribute.annotations.inputType == 'multiselect'>multiple</#if>
              >
                <#if attribute.annotations.inputType == 'select'><option value=""></option></#if>
                <#list options as option>
                  <option value="${option}" <#if attribute.values?seq_contains(option)>selected</#if>>${option}</option>
                </#list>
              </select>
            <#else>
              <input
                type="<#if attribute.annotations.inputType?? && attribute.annotations.inputType?starts_with('html5-')>${attribute.annotations.inputType[6..]}<#elseif attribute.annotations.inputType??>${attribute.annotations.inputType}<#else>text</#if>"
                id="${attribute.name}"
                name="${attribute.name}"
                class="field-input"
                value="${(attribute.value!'')}"
                aria-invalid="<#if messagesPerField.existsError('${attribute.name}')>true</#if>"
                <#if attribute.readOnly>disabled</#if>
                <#if attribute.autocomplete??>autocomplete="${attribute.autocomplete}"</#if>
                <#if attribute.annotations.inputTypePlaceholder??>placeholder="${advancedMsg(attribute.annotations.inputTypePlaceholder)}"</#if>
                <#if attribute.annotations.inputTypeMaxlength??>maxlength="${attribute.annotations.inputTypeMaxlength}"</#if>
              />
            </#if>
          </div>
          <#if messagesPerField.existsError('${attribute.name}')>
            <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('${attribute.name}'))?no_esc}</span>
          </#if>
        </div>
      </#list>

      <button tabindex="1" type="submit" class="btn-primary">
        <span>${msg("doSubmit")}</span>
      </button>

      <#if isAppInitiatedAction??>
        <button tabindex="2" type="submit" name="cancel-aia" value="true" formnovalidate class="btn-ghost">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16" aria-hidden="true">
            <path fill-rule="evenodd" d="M17 10a.75.75 0 01-.75.75H5.612l4.158 3.96a.75.75 0 11-1.04 1.08l-5.5-5.25a.75.75 0 010-1.08l5.5-5.25a.75.75 0 111.04 1.08L5.612 9.25H16.25A.75.75 0 0117 10z" clip-rule="evenodd"/>
          </svg>
          ${msg("doCancel")}
        </button>
      </#if>

    </form>
  </#if>

</@layout.registrationLayout>
