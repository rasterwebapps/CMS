<#import "template.ftl" as layout>
<@layout.registrationLayout; section>

  <#if section = "header">
    <div class="form-header">
      <div class="form-kicker">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16" fill="currentColor" width="11" height="11">
          <path fill-rule="evenodd" d="M8 1a3.5 3.5 0 1 1 0 7 3.5 3.5 0 0 1 0-7ZM4.5 8A2.5 2.5 0 0 0 2 10.5v.5a1 1 0 0 0 1 1h10a1 1 0 0 0 1-1v-.5A2.5 2.5 0 0 0 11.5 8h-7Z" clip-rule="evenodd"/>
        </svg>
        New Password
      </div>
      <h1 class="form-heading">Set new password</h1>
      <p class="form-subheading">Choose a strong password for your account.</p>
    </div>

  <#elseif section = "form">
    <form id="kc-passwd-update-form" class="cms-form" action="${url.loginAction}" method="post">

      <input type="text" id="username" name="username" value="${username}" autocomplete="username" readonly style="display:none" />

      <!-- ── New Password ── -->
      <div class="field-group <#if messagesPerField.existsError('password','password-confirm')>field-group--error</#if>">
        <label class="field-label" for="password-new">${msg("passwordNew")}</label>
        <div class="field-input-wrap">
          <span class="field-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
              <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd"/>
            </svg>
          </span>
          <input
            tabindex="1"
            id="password-new"
            class="field-input"
            name="password-new"
            type="password"
            autofocus
            autocomplete="new-password"
            placeholder="Enter new password"
          />
          <button
            type="button"
            class="field-eye-btn"
            aria-label="Toggle password visibility"
            onclick="cms_togglePwd('password-new', this)"
          >
            <svg id="eye-show-password-new" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
              <path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z"/>
              <path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.186A10.004 10.004 0 0110 3c4.257 0 7.893 2.66 9.336 6.41.147.381.146.804 0 1.186A10.004 10.004 0 0110 17c-4.257 0-7.893-2.66-9.336-6.41zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
            </svg>
            <svg id="eye-hide-password-new" class="hidden" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
              <path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06l-1.745-1.745a10.029 10.029 0 003.3-4.38 1.651 1.651 0 000-1.185A10.004 10.004 0 009.999 3a9.956 9.956 0 00-4.744 1.194L3.28 2.22zM7.752 6.69l1.092 1.092a2.5 2.5 0 013.374 3.373l1.091 1.092a4 4 0 00-5.557-5.557z" clip-rule="evenodd"/>
              <path d="M10.748 13.93l2.523 2.524a9.987 9.987 0 01-3.27.547c-4.258 0-7.894-2.66-9.337-6.41a1.651 1.651 0 010-1.186A10.007 10.007 0 012.839 6.02L6.07 9.252a4 4 0 004.678 4.678z"/>
            </svg>
          </button>
        </div>
        <#if messagesPerField.existsError('password')>
          <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
        </#if>
      </div>

      <!-- ── Confirm Password ── -->
      <div class="field-group <#if messagesPerField.existsError('password-confirm')>field-group--error</#if>">
        <label class="field-label" for="password-confirm">${msg("passwordConfirm")}</label>
        <div class="field-input-wrap">
          <span class="field-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
              <path fill-rule="evenodd" d="M10 1a4.5 4.5 0 00-4.5 4.5V9H5a2 2 0 00-2 2v6a2 2 0 002 2h10a2 2 0 002-2v-6a2 2 0 00-2-2h-.5V5.5A4.5 4.5 0 0010 1zm3 8V5.5a3 3 0 10-6 0V9h6z" clip-rule="evenodd"/>
            </svg>
          </span>
          <input
            tabindex="2"
            id="password-confirm"
            class="field-input"
            name="password-confirm"
            type="password"
            autocomplete="new-password"
            placeholder="Confirm new password"
          />
          <button
            type="button"
            class="field-eye-btn"
            aria-label="Toggle confirm password visibility"
            onclick="cms_togglePwd('password-confirm', this)"
          >
            <svg id="eye-show-password-confirm" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
              <path d="M10 12.5a2.5 2.5 0 100-5 2.5 2.5 0 000 5z"/>
              <path fill-rule="evenodd" d="M.664 10.59a1.651 1.651 0 010-1.186A10.004 10.004 0 0110 3c4.257 0 7.893 2.66 9.336 6.41.147.381.146.804 0 1.186A10.004 10.004 0 0110 17c-4.257 0-7.893-2.66-9.336-6.41zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
            </svg>
            <svg id="eye-hide-password-confirm" class="hidden" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="18" height="18">
              <path fill-rule="evenodd" d="M3.28 2.22a.75.75 0 00-1.06 1.06l14.5 14.5a.75.75 0 101.06-1.06l-1.745-1.745a10.029 10.029 0 003.3-4.38 1.651 1.651 0 000-1.185A10.004 10.004 0 009.999 3a9.956 9.956 0 00-4.744 1.194L3.28 2.22zM7.752 6.69l1.092 1.092a2.5 2.5 0 013.374 3.373l1.091 1.092a4 4 0 00-5.557-5.557z" clip-rule="evenodd"/>
              <path d="M10.748 13.93l2.523 2.524a9.987 9.987 0 01-3.27.547c-4.258 0-7.894-2.66-9.337-6.41a1.651 1.651 0 010-1.186A10.007 10.007 0 012.839 6.02L6.07 9.252a4 4 0 004.678 4.678z"/>
            </svg>
          </button>
        </div>
        <#if messagesPerField.existsError('password-confirm')>
          <span class="field-error" aria-live="polite">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
        </#if>
      </div>

      <button tabindex="3" type="submit" class="btn-primary">
        <span>${msg("doSubmit")}</span>
      </button>

      <#if isAppInitiatedAction??>
        <button tabindex="4" type="submit" name="cancel-aia" value="true" class="btn-ghost">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16" aria-hidden="true">
            <path fill-rule="evenodd" d="M17 10a.75.75 0 01-.75.75H5.612l4.158 3.96a.75.75 0 11-1.04 1.08l-5.5-5.25a.75.75 0 010-1.08l5.5-5.25a.75.75 0 111.04 1.08L5.612 9.25H16.25A.75.75 0 0117 10z" clip-rule="evenodd"/>
          </svg>
          Cancel
        </button>
      </#if>

    </form>
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
</script>
