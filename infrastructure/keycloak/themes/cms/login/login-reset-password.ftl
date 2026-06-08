<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>

  <#if section = "header">
    <div class="form-header">
      <div class="form-kicker">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" width="11" height="11">
          <path fill-rule="evenodd" d="M8 1a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7ZM4.5 8A2.5 2.5 0 0 0 2 10.5v.5a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-.5A2.5 2.5 0 0 0 11.5 8h-7Z" clip-rule="evenodd"/>
        </svg>
        Password Reset
      </div>
      <h1 class="form-heading">Reset password</h1>
      <p class="form-subheading">Enter your account email and we'll send a reset link.</p>
    </div>

  <#elseif section = "form">
    <form id="kc-reset-password-form" class="cms-form" action="${url.loginAction}" method="post">

      <div class="field-group <#if messagesPerField.existsError('username')>field-group--error</#if>">
        <label class="field-label" for="username">
          <#if !realm.loginWithEmailAllowed>${msg("username")}
          <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
          <#else>${msg("email")}
          </#if>
        </label>
        <div class="field-input-wrap">
          <span class="field-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
              <path d="M3 4a2 2 0 00-2 2v1.161l8.441 4.221a1.25 1.25 0 001.118 0L19 7.162V6a2 2 0 00-2-2H3z"/>
              <path d="M19 8.839l-7.77 3.885a2.75 2.75 0 01-2.46 0L1 8.839V14a2 2 0 002 2h14a2 2 0 002-2V8.839z"/>
            </svg>
          </span>
          <input
            tabindex="1"
            id="username"
            class="field-input"
            name="username"
            value="${(auth.attemptedUsername!'')}"
            type="text"
            autofocus
            autocomplete="<#if !realm.loginWithEmailAllowed>username<#else>email</#if>"
            placeholder="<#if !realm.loginWithEmailAllowed>Your username<#elseif !realm.registrationEmailAsUsername>Username or email<#else>Your email address</#if>"
          />
        </div>
        <#if messagesPerField.existsError('username')>
          <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
        </#if>
      </div>

      <button tabindex="4" type="submit" class="btn-primary">
        <span>${msg("doSubmit")}</span>
      </button>

      <#assign signInUrl = (url.loginRestartFlowUrl!url.loginUrl)>
      <a href="${signInUrl}" class="btn-ghost" tabindex="5">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16" aria-hidden="true">
          <path fill-rule="evenodd" d="M17 10a.75.75 0 01-.75.75H5.612l4.158 3.96a.75.75 0 11-1.04 1.08l-5.5-5.25a.75.75 0 010-1.08l5.5-5.25a.75.75 0 111.04 1.08L5.612 9.25H16.25A.75.75 0 0117 10z" clip-rule="evenodd"/>
        </svg>
        Back to sign in
      </a>

    </form>

  <#elseif section = "info">
    <p class="form-footer-text">${msg("emailInstruction")}</p>
  </#if>

</@layout.registrationLayout>
