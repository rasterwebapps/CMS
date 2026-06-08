<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>

  <#if section = "header">
    <div class="form-header">
      <h1 class="form-heading">Information</h1>
    </div>

  <#elseif section = "form">
    <div class="info-card info-card--info">
      <div class="info-card-icon" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
          <path fill-rule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm8.706-1.442c1.146-.573 2.437.463 2.126 1.706l-.709 2.836.042-.02a.75.75 0 01.67 1.34l-.04.022c-1.147.573-2.438-.463-2.127-1.706l.71-2.836-.042.02a.75.75 0 11-.671-1.34l.041-.022zM12 9a.75.75 0 100-1.5.75.75 0 000 1.5z" clip-rule="evenodd"/>
        </svg>
      </div>
      <#if message??><p class="info-card-msg">${kcSanitize(message.summary)?no_esc}</p></#if>
    </div>

    <#if pageRedirectUri?has_content>
      <a href="${pageRedirectUri}" class="btn-primary">Continue</a>
    <#elseif actionUri?has_content>
      <a href="${actionUri}" class="btn-primary">${msg("proceedWithAction")}</a>
    <#elseif client?? && client.baseUrl?has_content>
      <a href="${client.baseUrl}" class="btn-primary">Return to application</a>
    <#else>
      <#assign signInUrl = (url.loginRestartFlowUrl!url.loginUrl)>
      <a href="${signInUrl}" class="btn-primary">Back to sign in</a>
    </#if>
  </#if>

</@layout.registrationLayout>

