<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>

  <#if section = "header">
    <div class="form-header">
      <h1 class="form-heading form-heading--error">Something went wrong</h1>
      <p class="form-subheading">An error occurred during authentication.</p>
    </div>

  <#elseif section = "form">
    <div class="info-card info-card--error">
      <div class="info-card-icon" aria-hidden="true">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="32" height="32">
          <path fill-rule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zm-1.72 6.97a.75.75 0 10-1.06 1.06L10.94 12l-1.72 1.72a.75.75 0 101.06 1.06L12 13.06l1.72 1.72a.75.75 0 101.06-1.06L13.06 12l1.72-1.72a.75.75 0 10-1.06-1.06L12 10.94l-1.72-1.72z" clip-rule="evenodd"/>
        </svg>
      </div>
      <#if message??><p class="info-card-msg">${kcSanitize(message.summary)?no_esc}</p></#if>
    </div>

    <#if client?? && client.baseUrl?has_content>
      <a href="${client.baseUrl}" class="btn-primary">Return to application</a>
    <#else>
      <a href="${url.loginUrl}" class="btn-primary">Back to sign in</a>
    </#if>
  </#if>

</@layout.registrationLayout>

