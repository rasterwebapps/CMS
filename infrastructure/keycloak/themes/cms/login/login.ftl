<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>

  <#if section = "header">
    <div class="form-header">
      <div class="form-kicker">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" width="11" height="11">
          <path fill-rule="evenodd" d="M8 1a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7ZM4.5 8A2.5 2.5 0 0 0 2 10.5v.5a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-.5A2.5 2.5 0 0 0 11.5 8h-7Z" clip-rule="evenodd"/>
        </svg>
        Secure Sign In
      </div>
      <h1 class="form-heading">Welcome back</h1>
      <p class="form-subheading">Sign in with your college account to continue</p>
    </div>

  <#elseif section = "form">
    <#if realm.password>
      <form id="kc-form-login" class="cms-form" onsubmit="cms_handleSubmit(this, event)" action="${url.loginAction}" method="post">

        <!-- ── Username / Email ── -->
        <#if !usernameHidden??>
          <div class="field-group <#if messagesPerField.existsError('username','password')>field-group--error</#if>">
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
                value="${(login.username!'')}"
                type="text"
                autocomplete="<#if !realm.loginWithEmailAllowed>username<#else>email</#if>"
                autofocus
                spellcheck="false"
                placeholder="<#if !realm.loginWithEmailAllowed>Enter your username<#elseif !realm.registrationEmailAsUsername>Username or email<#else>Enter your email</#if>"
              />
            </div>
            <#if messagesPerField.existsError('username')>
              <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('username'))?no_esc}</span>
            </#if>
          </div>
        </#if>

        <!-- ── Password ── -->
        <#if realm.password>
          <div class="field-group <#if messagesPerField.existsError('username','password')>field-group--error</#if>">
            <label class="field-label" for="password">${msg("password")}</label>
            <div class="field-input-wrap">
              <span class="field-icon">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                  <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd"/>
                </svg>
              </span>
              <input
                tabindex="2"
                id="password"
                class="field-input"
                name="password"
                type="password"
                autocomplete="current-password"
                placeholder="Enter your password"
              />
              <button
                type="button"
                class="field-eye-btn"
                aria-label="Toggle password visibility"
                onclick="cms_togglePwd('password', this)"
              >
                <svg id="eye-show-password" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
                  <path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z"/>
                  <path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.186A10.004 10.004 0 0110 3c4.257 0 7.893 2.66 9.336 6.41.147.381.146.804 0 1.186A10.004 10.004 0 0110 17c-4.257 0-7.893-2.66-9.336-6.41zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
                </svg>
                <svg id="eye-hide-password" class="hidden" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
                  <path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06l-1.745-1.745a10.029 10.029 0 003.3-4.38 1.651 1.651 0 000-1.185A10.004 10.004 0 009.999 3a9.956 9.956 0 00-4.744 1.194L3.28 2.22zM7.752 6.69l1.092 1.092a2.5 2.5 0 013.374 3.373l1.091 1.092a4 4 0 00-5.557-5.557z" clip-rule="evenodd"/>
                  <path d="M10.748 13.93l2.523 2.524a9.987 9.987 0 01-3.27.547c-4.258 0-7.894-2.66-9.337-6.41a1.651 1.651 0 010-1.186A10.007 10.007 0 012.839 6.02L6.07 9.252a4 4 0 004.678 4.678z"/>
                </svg>
              </button>
            </div>
            <#if messagesPerField.existsError('password')>
              <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
            </#if>
          </div>
        </#if>

        <!-- ── Options row (Remember me + Forgot password) ── -->
        <div class="form-options">
          <#if realm.rememberMe && !usernameHidden??>
            <label class="checkbox-label">
              <input
                tabindex="3"
                id="rememberMe"
                name="rememberMe"
                type="checkbox"
                class="checkbox-input"
                <#if login.rememberMe??>checked</#if>
              />
              <span class="checkbox-mark"></span>
              <span class="checkbox-text">${msg("rememberMe")}</span>
            </label>
          </#if>
          <div class="form-options-spacer"></div>
          <#if realm.resetPasswordAllowed>
            <a href="${url.loginResetCredentialsUrl}" class="link-muted" tabindex="5">${msg("doForgotPassword")}</a>
          </#if>
        </div>

        <!-- ── Submit ── -->
        <button
          tabindex="4"
          id="kc-login"
          name="login"
          type="submit"
          class="btn-primary"
        >
          <span class="btn-text">${msg("doLogIn")}</span>
          <span class="btn-spinner hidden" aria-hidden="true">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" width="18" height="18">
              <circle class="spinner-track" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3"/>
              <path class="spinner-arc" stroke="currentColor" stroke-width="3" stroke-linecap="round" d="M12 2a10 10 0 0110 10"/>
            </svg>
          </span>
        </button>

      </form>
    </#if>

  <#elseif section = "info">
    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
      <p class="form-footer-text">
        Don't have an account?
        <a href="${url.registrationUrl}" class="link-primary" tabindex="6">${msg("doRegister")}</a>
      </p>
    </#if>
  </#if>

</@layout.registrationLayout>

<script>
  function cms_togglePwd(id, btn) {
    var input = document.getElementById(id);
    var showIcon = btn.querySelector('#eye-show-' + id);
    var hideIcon = btn.querySelector('#eye-hide-' + id);
    var isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    if (showIcon) showIcon.classList.toggle('hidden', isHidden);
    if (hideIcon) hideIcon.classList.toggle('hidden', !isHidden);
  }

  function cms_handleSubmit(form, event) {
    var btn = document.getElementById('kc-login');
    if (btn) {
      btn.disabled = true;
      var text = btn.querySelector('.btn-text');
      var spinner = btn.querySelector('.btn-spinner');
      if (text) text.classList.add('hidden');
      if (spinner) spinner.classList.remove('hidden');
    }
    return true;
  }
</script>

