<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false showAnotherWayIfPresent=true>
<!DOCTYPE html>
<html class="cms-login" lang="${(locale.currentLanguageTag)!'en'}">
<head>
  <meta charset="UTF-8" />
  <meta name="robots" content="noindex, nofollow" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${msg("loginTitle",(realm.displayName!'College Management System'))}</title>
  <link rel="stylesheet" href="${url.resourcesPath}/css/login.css" />
</head>
<body>

  <!-- Aurora blobs (positioned by CSS, injected here for clean DOM) -->
  <div class="aurora-a" aria-hidden="true"></div>
  <div class="aurora-b" aria-hidden="true"></div>
  <div class="aurora-c" aria-hidden="true"></div>

  <div class="cms-split">

    <!-- ═══════════════════════════════════════════════════════════
         SINGLE CENTERED GLASS CARD
    ══════════════════════════════════════════════════════════════ -->
    <main class="cms-form-card">

      <!-- Brand block — client identity takes the hero position -->
      <div class="card-brand">
        <div class="card-brand-logo">
          <img src="${url.resourcesPath}/img/sks-logo.png"
               alt="SKS College Of Nursing"
               class="brand-logo-img" />
        </div>
        <div class="card-brand-name">
          <span class="brand-client">SKS College Of Nursing</span>
        </div>
      </div>

      <!-- Global alert message -->
      <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
        <div class="cms-alert cms-alert--${message.type}" role="alert">
          <span class="cms-alert-icon">
            <#if message.type = 'error'>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.28 7.22a.75.75 0 00-1.06 1.06L8.94 10l-1.72 1.72a.75.75 0 101.06 1.06L10 11.06l1.72 1.72a.75.75 0 101.06-1.06L11.06 10l1.72-1.72a.75.75 0 00-1.06-1.06L10 8.94 8.28 7.22z" clip-rule="evenodd"/></svg>
            <#elseif message.type = 'warning'>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clip-rule="evenodd"/></svg>
            <#else>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" width="16" height="16"><path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a.75.75 0 000 1.5h.253a.25.25 0 01.244.304l-.459 2.066A1.75 1.75 0 0010.747 15H11a.75.75 0 000-1.5h-.253a.25.25 0 01-.244-.304l.459-2.066A1.75 1.75 0 009.253 9H9z" clip-rule="evenodd"/></svg>
            </#if>
          </span>
          <span>${kcSanitize(message.summary)?no_esc}</span>
        </div>
      </#if>

      <!-- Header slot -->
      <#nested "header">

      <!-- Form slot -->
      <#nested "form">

      <!-- Info slot -->
      <#if displayInfo>
        <div class="cms-info">
          <#nested "info">
        </div>
      </#if>

      <!-- Card footer — product + company attribution -->
      <p class="card-footer">
        Powered by <strong>OneCMS</strong> &bull; Raster Images Private Limited &bull; 2026
      </p>

    </main>

  </div>

</body>
</html>
</#macro>
