 ---                                                                                     
  Phase 3 — Detail / Profile Screens                                                                                                                                                                                               
                                                                                                                                                                                                                                   
  Current-state summary (do not skip this read)                                                                                                                                                                                    
                                                                                                                                                                                                                                   
  ┌────────────────┬──────────────────────────────────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────┐                                                               
  │     Screen     │                        Current shape                         │                                 What's missing                                 │                                                               
  ├────────────────┼──────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────┤                                                               
  │ enquiry-detail │ mat-tab-group with 4 flat content-cards                      │ No sidebar, no quick-stats, no visual timeline, payments/docs are plain tables │
  ├────────────────┼──────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────┤
  │ student-detail │ One giant content-card with 4 section-divider blocks stacked │ No tabs, no hero, no stats, no personality                                     │                                                               
  ├────────────────┼──────────────────────────────────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────┤                                                               
  │ faculty-detail │ Two side-by-side content-card.info-card (content-grid)       │ No tabs, no hero, no stats, no course/lab data                                 │                                                               
  └────────────────┴──────────────────────────────────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────┘                                                               
                                                            
  ---                                                                                                                                                                                                                              
  Step 0 — Pre-work: files to read before writing a line    
                                                                                                                                                                                                                                   
  Read every one of these files in full before starting any screen:
                                                                                                                                                                                                                                   
  enquiry-detail.component.html / .ts / .scss               
  student-detail.component.html / .ts / .scss                                                                                                                                                                                      
  faculty-detail.component.html / .ts / .scss               
                                                                                                                                                                                                                                   
  enquiry.model.ts                                                                                                                                                                                                                 
  enquiry.service.ts          ← check what signals exist, what APIs are called
  student.model.ts (if exists, else read student-detail.component.ts for the inline type)                                                                                                                                          
  faculty.model.ts (if exists, else read faculty-detail.component.ts)                                                                                                                                                              
                                                                                                                                                                                                                                   
  styles.scss                 ← know which tokens and layout classes already exist                                                                                                                                                 
                                                            
  The goals of this read-through:                                                                                                                                                                                                  
  - Confirm which signals are already loaded (enquiry(), documents(), payments(), statusHistory())
  - Confirm which data is not loaded yet (student fee summary, faculty course list) so you know what new API calls to add                                                                                                          
  - Map every existing CSS class to its definition in styles.scss so you don't duplicate                                 
                                                                                                                                                                                                                                   
  ---                                                                                                                                                                                                                              
  Step 1 — Add global layout primitives to styles.scss                                                                                                                                                                             
                                                                                                                                                                                                                                   
  Add the following CSS class groups to styles.scss. Do not add them to any component SCSS file — they are shared across all three screens.
                                                                                                                                                                                                                                   
  1.1 Split-pane detail layout                                                                                                                                                                                                     
                                                                                                                                                                                                                                   
  Used by Enquiry Detail. A two-column layout: narrow sticky sidebar on the left, scrollable main panel on the right.                                                                                                              
                                                            
  Classes to define:                                                                                                                                                                                                               
  - .detail-page — outer wrapper, replaces .list-page on detail screens. Removes the list-page's padding assumptions.
  - .detail-split — CSS Grid with named areas: sidebar (280px) and main (1fr). On screens narrower than 900px, collapse to single column (sidebar moves above main).                                                               
  - .detail-split__sidebar — position: sticky; top: 88px (below the fixed topbar), max-height: calc(100vh - 100px), overflow-y: auto.                               
  - .detail-split__main — min-width: 0 to prevent grid blowout.                                                                                                                                                                    
                                                                                                                                                                                                                                   
  1.2 Profile hero block                                                                                                                                                                                                           
                                                                                                                                                                                                                                   
  Used by Student and Faculty profiles. A full-width hero above the tabs.                                                                                                                                                          
                                                            
  Classes to define:                                                                                                                                                                                                               
  - .profile-hero — horizontal flex row, padding: 28px 32px 24px, border-bottom: 1px solid var(--cms-border).
  - .profile-hero__avatar — 64px circle, background: var(--cms-primary-alpha-16), color: var(--cms-primary), font-family: var(--cms-font-display), font-size: 1.5rem, font-weight: 600, display: flex; align-items: center;        
  justify-content: center.                                                                                                                                                                                                  
  - .profile-hero__body — flex column, gap: 6px, flex: 1.                                                                                                                                                                          
  - .profile-hero__name — font-family: var(--cms-font-display), font-size: 1.5rem, font-weight: 400, color: var(--cms-text-primary).
  - .profile-hero__meta — flex row of badges, gap: 8px, flex-wrap: wrap.                                                                                                                                                           
  - .profile-hero__stats — flex row of 4 stat chips, gap: 12px, margin-top: 16px, flex-wrap: wrap.                                                                                                                                 
  - .profile-stat — single stat chip: padding: 10px 16px, background: var(--cms-bg-tint), border-radius: var(--cms-radius-md), border: 1px solid var(--cms-border).                                                                
  - .profile-stat__value — font-size: 1.25rem, font-weight: 700, font-variant-numeric: tabular-nums, color: var(--cms-text-primary).                                                                                               
  - .profile-stat__label — font-size: 0.6875rem, font-weight: 600, text-transform: uppercase, letter-spacing: 0.08em, color: var(--cms-text-muted).                                                                                
                                                                                                                                                                                                                                   
  1.3 Status timeline                                                                                                                                                                                                              
                                                                                                                                                                                                                                   
  Used in the Enquiry Detail sidebar and optionally reused in Enquiry List detail pop-ins later.                                                                                                                                   
                                                                                                                                                                                                                                   
  Classes to define:                                                                                                                                                                                                               
  - .status-timeline — padding: 4px 0, vertical flex column.
  - .status-timeline__item — flex row, gap: 12px, position: relative. Connects to next item via a vertical line pseudo-element (::before on __connector).                                                                          
  - .status-timeline__connector — width: 2px, background: var(--cms-border), flex-shrink: 0, margin: 0 auto. Height stretches to connect dots.           
  - .status-timeline__dot — 12px circle, border: 2px solid var(--cms-border), background: var(--cms-bg-card). Modifier --active: filled with var(--cms-primary).                                                                   
  - .status-timeline__body — the label + date column.                                                                                                                                                                              
  - .status-timeline__label — font-family: var(--cms-font-display), font-size: 0.9375rem, font-weight: 400.                                                                                                                        
  - .status-timeline__date — font-size: 0.75rem, color: var(--cms-text-muted).                                                                                                                                                     
                                                                                                                                                                                                                                   
  1.4 Receipt card                                                                                                                                                                                                                 
                                                                                                                                                                                                                                   
  Used in the Payments tab of Enquiry Detail.                                                                                                                                                                                      
                                                            
  Classes to define:                                                                                                                                                                                                               
  - .receipt-card — border: 1px solid var(--cms-border), border-left: 4px solid var(--cms-primary), border-radius: var(--cms-radius-md), padding: 14px 16px, display: grid, grid-template-columns: 1fr auto.
  - .receipt-card--cash → border-left-color: green token.                                                                                                                                                                          
  - .receipt-card--online → primary.                     
  - .receipt-card--cheque → amber.                                                                                                                                                                                                 
  - .receipt-card__number — font-family: var(--cms-font-mono), font-size: 0.875rem, color: var(--cms-text-muted).                                                                                                                  
  - .receipt-card__amount — font-size: 1.125rem, font-weight: 700, font-variant-numeric: tabular-nums.                                                                                                                             
  - .receipt-card__meta — small secondary row: date + collected-by.                                                                                                                                                                
                                                                                                                                                                                                                                   
  1.5 Document checklist row                                                                                                                                                                                                       
                                                                                                                                                                                                                                   
  Used in the Documents tab of Enquiry Detail and later Student Profile.                                                                                                                                                           
                                                            
  Classes to define:                                                                                                                                                                                                               
  - .doc-row — flex row, padding: 12px 0, border-bottom: 1px solid var(--cms-border-light), align-items: center, gap: 14px.
  - .doc-row__icon — 24px circle with SVG check (VERIFIED), X (REJECTED), or clock (PENDING). Use CSS modifier classes --verified, --rejected, --pending to swap fill colors using tokens.                                         
  - .doc-row__name — font-weight: 500, flex: 1.                                                                                                                                           
  - .doc-row__status — delegate to <cms-status-badge>.                                                                                                                                                                             
  - .doc-row__meta — small muted text: "Verified by X on date".                                                                                                                                                                    
                                                                                                                                                                                                                                   
  ---                                                                                                                                                                                                                              
  Step 2 — Enquiry Detail (flagship screen)                                                                                                                                                                                        
                                                                                                                                                                                                                                   
  Do this screen first. The patterns you establish here are the template for all other detail screens.
                                                                                                                                                                                                                                   
  2.1 Change the outer wrapper                              
                                                                                                                                                                                                                                   
  Change <div class="list-page"> to <div class="detail-page">. The detail-page class removes the list-page's assumptions about padding and max-width, since detail screens need a different spatial treatment.                     
   
  2.2 Restructure the HTML into split-pane                                                                                                                                                                                         
                                                            
  The new structure inside the @else if (enquiry()) block:                                                                                                                                                                         
                                                            
  div.detail-split                                                                                                                                                                                                                 
    aside.detail-split__sidebar                             
      [sidebar content — see 2.3]
    div.detail-split__main                                                                                                                                                                                                         
      mat-tab-group                                                                                                                                                                                                                
        [same 4 tabs — see 2.4–2.7]                                                                                                                                                                                                
                                                                                                                                                                                                                                   
  Keep <app-page-header> outside the split — it stays at the top spanning the full width.                                                                                                                                          
   
  2.3 Sidebar content                                                                                                                                                                                                              
                                                            
  The sidebar should contain, in order:                                                                                                                                                                                            
                                                            
  Quick stats block (rendered as a simple list, not the stat-grid):                                                                                                                                                                
  - Status — a large <cms-status-badge> with the current status, centered.
  - Days active — compute in TypeScript as Math.floor((Date.now() - Date.parse(enquiry()!.enquiryDate)) / 86_400_000). Display as "42 days".                                                                                       
  - Total paid — computed(() => payments().reduce((s, p) => s + p.amountPaid, 0)). Display as formatted currency.                           
  - Documents — computed(() => documents().filter(d => d.status === 'VERIFIED').length + ' / ' + documents().length + ' verified').                                                                                                
                                                                                                                                                                                                                                   
  Each quick stat: a two-line block — label on top (small caps, muted) + value below (bold, larger).                                                                                                                               
                                                                                                                                                                                                                                   
  Status timeline — rendered from statusHistory() signal which is already loaded. Map each history item to a .status-timeline__item. The most recent item gets the --active dot. Items are in chronological order (oldest first →  
  newest last at the bottom).                                                                                                                                                                                                      
                                                                                                                                                                                                                                   
  Because statusHistory is a separate async load, wrap the timeline in @if (statusHistory().length > 0), otherwise show a brief skeleton placeholder (use <app-skeleton [lines]="3"> from the existing skeleton component).        
   
  2.4 Overview tab                                                                                                                                                                                                                 
                                                            
  No structural change — keep the existing detail-grid. But:                                                                                                                                                                       
  - Rename <h3 class="section-title"> to nothing (remove it) — the tab label already says "Overview".
  - Move Status into the sidebar (it's now in the quick stats block), remove it from the detail-grid.                                                                                                                              
  - Apply font-family: var(--cms-font-display) to the enquiry name in <app-page-header> by passing a CSS class via the header's [titleClass] input (add that input to PageHeaderComponent if it doesn't exist).
  - Any field that is an ID or code (none currently in this grid) should get font-family: var(--cms-font-mono).                                                                                                                    
                                                                                                                                                                                                                                   
  2.5 Payments tab — switch from table to receipt cards                                                                                                                                                                            
                                                                                                                                                                                                                                   
  Remove the <table mat-table> entirely. Replace with a @for loop over payments() that renders a .receipt-card for each payment. The border-left color varies by paymentMode:                                                      
  - CASH → --cms-success (green)                                                                                                                                                                                                   
  - ONLINE / UPI → --cms-primary                                                                                                                                                                                                   
  - CHEQUE / DD → amber (var(--cms-warning))                
                                                                                                                                                                                                                                   
  Inside each receipt card:                                                                                                                                                                                                        
  - Top-left: .receipt-card__number — the receipt number in mono font
  - Top-right: .receipt-card__amount — the amount                                                                                                                                                                                  
  - Bottom row: .receipt-card__meta — date (formatted dd MMM yyyy using DatePipe) + "via MODE" + "collected by NAME"
                                                                                                                                                                                                                                   
  Keep the @if (payments().length === 0) empty state.                                                                                                                                                                              
                                                                                                                                                                                                                                   
  2.6 Documents tab — switch from table to checklist rows                                                                                                                                                                          
                                                                                                                                                                                                                                   
  Remove the <table mat-table>. Replace with a @for loop over documents() rendering .doc-row per document. The icon logic:                                                                                                         
  - status === 'VERIFIED' → checkmark SVG in .doc-row__icon--verified (green)
  - status === 'REJECTED' → X SVG in .doc-row__icon--rejected (red)                                                                                                                                                                
  - Otherwise → clock SVG in .doc-row__icon--pending (amber)       
                                                                                                                                                                                                                                   
  Remove <cms-status-badge> from the document row — the icon already communicates status visually. Only show the badge as a text fallback if there is a custom status value.
                                                                                                                                                                                                                                   
  2.7 Status History tab — keep as-is                       
                                                                                                                                                                                                                                   
  The history table is fine as a reference record. No visual change needed here. It is already rendered with <cms-status-badge> for from/to status.                                                                                
   
  2.8 TypeScript additions for Enquiry Detail                                                                                                                                                                                      
                                                            
  Add these computed signals to the component class:                                                                                                                                                                               
   
  - daysActive — computed from enquiry()!.enquiryDate to Date.now()                                                                                                                                                                
  - totalPaid — sum of payments().map(p => p.amountPaid)    
  - docsVerified — count of documents() where status === 'VERIFIED'                                                                                                                                                                
  - docsTotal — documents().length                                                                                                                                                                                                 
                                                                                                                                                                                                                                   
  No new API calls needed — all data is already loaded by the existing load() method.                                                                                                                                              
                                                                                                                                                                                                                                   
  ---                                                                                                                                                                                                                              
  Step 3 — Student Profile                                  

  3.1 Change outer wrapper and add hero

  Change <div class="list-page"> to <div class="detail-page">.                                                                                                                                                                     
   
  Remove <app-page-header>. Replace it with a .profile-hero block directly inside .detail-page. The page header provided the title + edit button — move both into the hero layout:                                                 
  - Edit button goes in .profile-hero__actions (flex row, margin-left: auto).
  - The subtitle="View student profile and details" is dropped — the hero itself communicates context.                                                                                                                             
                                                            
  3.2 Hero content                                                                                                                                                                                                                 
                                                            
  - Avatar: initials computed as computed(() => student()?.fullName.split(' ').map(w => w[0]).filter((_, i, a) => i === 0 || i === a.length - 1).join('').toUpperCase()). Render as <div class="profile-hero__avatar">{{ initials()
   }}</div>.                                                
  - Name: <h1 class="profile-hero__name">{{ student()!.fullName }}</h1>.                                                                                                                                                           
  - Meta badges: <cms-status-badge> for status + a plain cms-badge for program name + a plain cms-badge for "Sem {{ semester }}".
  - Stats bar (.profile-hero__stats): 4 .profile-stat chips:                                                                                                                                                                       
    a. Roll Number — value in mono font                                                                                                                                                                                            
    b. Program (abbreviated if too long)                                                                                                                                                                                           
    c. Admission Date                                                                                                                                                                                                              
    d. Status (already in meta badges — replace this 4th slot with "Lab Batch" if available, else "—")                                                                                                                             
                                                                                                                                                                                                                                   
  The stats bar here is static data from the already-loaded student() signal, not new API calls. Resist adding new API calls for Phase 3 — the goal is layout and UX, not new data loading.                                        
                                                                                                                                                                                                                                   
  3.3 Replace the single content-card with mat-tabs                                                                                                                                                                                
                                                            
  The current screen is one giant card. Replace it with <mat-tab-group> wrapping four tabs:                                                                                                                                        
                                                            
  Tab 1 — Profile                                                                                                                                                                                                                  
  Render the existing "Basic Information" detail-grid section. Keep the label/value pairs exactly as-is. Remove the section-divider separators — tab grouping replaces them.
                                                                                                                                                                                                                                   
  Tab 2 — Personal & Family
  Move the "Personal Details", "Family Details", and "Address" detail-grid sections here. Three sub-sections within the tab, each with a <p class="form-section-title"> heading (the same class defined in Phase 2 for forms — it  
  works here too as a section micro-label).                                                                                                                                                                                        
   
  Tab 3 — Academic (placeholder for Phase 4 enhancement)                                                                                                                                                                           
  Show an empty-state card: "Academic data will be available here." with the existing empty-state__content pattern. Do not make new API calls.
                                                                                                                                                                                                                                   
  Tab 4 — Finance (placeholder)                                                                                                                                                                                                    
  Same empty-state. The student-fee-detail screen already handles the full finance view — link to it instead: <a class="btn-secondary" [routerLink]="['/student-fees', student()!.id]">View Fee Details</a>.                       
                                                                                                                                                                                                                                   
  3.4 Replace spinner with skeleton                         
                                                                                                                                                                                                                                   
  Change the @if (loading()) block from <mat-spinner> to use the existing <app-skeleton> component. Render two skeleton "lines" of different widths to approximate the hero layout:                                                
  - One circle skeleton for the avatar ([circle]="true" height="64px")
  - Two line skeletons for name + meta                                                                                                                                                                                             
                                                            
  3.5 TypeScript additions for Student Detail                                                                                                                                                                                      
                                                            
  Add one computed signal:                                                                                                                                                                                                         
  - initials — first + last initial of student()!.fullName  
                                                                                                                                                                                                                                   
  No new API calls.
                                                                                                                                                                                                                                   
  ---                                                       
  Step 4 — Faculty Profile
                                                                                                                                                                                                                                   
  4.1 Change outer wrapper and add hero
                                                                                                                                                                                                                                   
  Same pattern as Student:                                  
  - Change <div class="list-page"> to <div class="detail-page">.
  - Remove <app-page-header>. Replace with .profile-hero.                                                                                                                                                                          
  - Keep the @if (authService.isAdmin()) guard on the Edit button — move it into .profile-hero__actions.
                                                                                                                                                                                                                                   
  4.2 Hero content                                                                                                                                                                                                                 
                                                                                                                                                                                                                                   
  - Avatar: initials computed from faculty()!.fullName — same formula as student.                                                                                                                                                  
  - Name: <h1 class="profile-hero__name">{{ faculty()!.fullName }}</h1>.                                                                                                                                                           
  - Meta badges: <cms-status-badge> for status + cms-badge for designation (human-readable via getDesignationLabel()) + cms-badge for department name.                                                                             
  - Stats bar (4 chips):                                                                                                                                                                                                           
    a. Employee Code — mono font                                                                                                                                                                                                   
    b. Department name                                                                                                                                                                                                             
    c. Joining Date (formatted dd MMM yyyy)                                                                                                                                                                                        
    d. Designation label
                                                                                                                                                                                                                                   
  4.3 Replace content-grid with mat-tabs                                                                                                                                                                                           
   
  The current screen is two side-by-side content-card.info-card elements. Replace with <mat-tab-group> and two tabs (plus two placeholders):                                                                                       
                                                            
  Tab 1 — Profile                                                                                                                                                                                                                  
  Render the "Basic Information" detail-grid exactly as-is (email, phone, department, designation, status, joining date).
                                                                                                                                                                                                                                   
  Tab 2 — Professional                                                                                                                                                                                                             
  Render the "Professional Information" content (specialization, lab expertise) as a proper detail-grid instead of the current ad-hoc detail-item elements without a grid wrapper. Use form-section-title labels for sub-sections.
                                                                                                                                                                                                                                   
  Tab 3 — Courses (placeholder)                                                                                                                                                                                                    
  Empty-state: "Assigned courses will appear here." Link to course list filtered by faculty if the route supports it.                                                                                                              
                                                                                                                                                                                                                                   
  Tab 4 — Lab Schedules (placeholder)                       
  Empty-state: "Lab schedule will appear here." Link to lab-schedule list.                                                                                                                                                         
                                                                                                                                                                                                                                   
  4.4 TypeScript additions for Faculty Detail
                                                                                                                                                                                                                                   
  Add one computed signal:                                  
  - initials — same formula as student

  No new API calls.

  ---
  Step 5 — Cross-cutting: typography pass
                                                                                                                                                                                                                                   
  After all three screens are restructured, do one final pass across all three for typography consistency. This is purely CSS — no TypeScript changes.
                                                                                                                                                                                                                                   
  Rule 1: Names use Instrument Serif.                                                                                                                                                                                              
  Any element rendering a person's full name should have font-family: var(--cms-font-display). In practice: .profile-hero__name already has this from Step 1. Also apply it to the <app-page-header> title on the Enquiry Detail   
  (the enquiry name enquiry()!.name should render in display font). Add a [titleClass] input to PageHeaderComponent that gets applied to the title <h1> — or simply add an inline style binding                                    
  [style.fontFamily]="'var(--cms-font-display)'" if you want to avoid changing the shared component.
                                                                                                                                                                                                                                   
  Rule 2: Codes use JetBrains Mono.                         
  Any element rendering a roll number, employee code, receipt number, or system-generated ID should have font-family: var(--cms-font-mono). These are already inside .receipt-card__number. Also wrap student()!.rollNumber and
  faculty()!.employeeCode values in <span class="code-value"> and add .code-value { font-family: var(--cms-font-mono); } to styles.scss.                                                                                           
   
  Rule 3: Stat values use tabular numerals.                                                                                                                                                                                        
  .profile-stat__value and .receipt-card__amount should have font-variant-numeric: tabular-nums so numbers align cleanly. Already baked into the class definitions in Step 1.
                                                                                                                                                                                                                                   
  ---
  Step 6 — Cross-cutting: replace spinners with skeletons                                                                                                                                                                          
                                                                                                                                                                                                                                   
  All three screens currently use <mat-spinner diameter="48"> in their loading state. Replace each with <app-skeleton> to give the screen a shape while loading:
                                                                                                                                                                                                                                   
  - Enquiry Detail loading state: Render a skeleton approximating the split-pane layout — a narrow sidebar shape ([lines]="5") + a wider main shape ([lines]="8"). Use a flex row wrapper.                                         
  - Student Detail loading state: A circle skeleton for the avatar + two line skeletons for name + badges + four small rectangles for the stat chips.                                                                              
  - Faculty Detail loading state: Same as student.                                                                                                                                                                                 
                                                                                                                                                                                                                                   
  The CmsSkeletonComponent already exists in shared/skeleton/. Import it into each of the three detail components' imports array.                                                                                                  
                                                                                                                                                                                                                                   
  ---                                                                                                                                                                                                                              
  Step 7 — Cross-cutting: mat-tab-group visual upgrade      
                                                                                                                                                                                                                                   
  All three screens use <mat-tab-group>. After completing all screen changes, do one SCSS pass to make mat-tabs match the token system. Add these overrides in each component's .scss file (not globally, to avoid affecting
  dialogs and other tab contexts):                                                                                                                                                                                                 
                                                            
  Target selectors to override:                                                                                                                                                                                                    
  - .mat-mdc-tab-header — remove box-shadow, set border-bottom: 1px solid var(--cms-border)
  - .mat-mdc-tab .mdc-tab__text-label — set font-family: var(--cms-font-ui), font-size: 0.875rem, font-weight: 500                                                                                                                 
  - .mat-mdc-tab.mdc-tab--active .mdc-tab__text-label — color: var(--cms-primary), font-weight: 600               
  - .mat-mdc-tab-body-wrapper — padding-top: 0 (tab content handles its own spacing)                                                                                                                                               
  - .mdc-tab-indicator__content--underline — border-color: var(--cms-primary)                                                                                                                                                      
                                                                                                                                                                                                                                   
  ---                                                                                                                                                                                                                              
  Step 8 — Implementation order                                                                                                                                                                                                    
                                                                                                                                                                                                                                   
  Execute in this sequence to avoid blocked states:         
                                                                                                                                                                                                                                   
  8.1  styles.scss additions (Step 1 — all primitives)
  8.2  Enquiry Detail restructure (Step 2, all sub-steps)                                                                                                                                                                          
  8.3  Build — verify zero errors before proceeding                                                                                                                                                                                
  8.4  Student Profile restructure (Step 3, all sub-steps)                                                                                                                                                                         
  8.5  Faculty Profile restructure (Step 4, all sub-steps)                                                                                                                                                                         
  8.6  Build — verify again                                 
  8.7  Typography pass (Step 5) — one file at a time                                                                                                                                                                               
  8.8  Skeleton replacements (Step 6)                       
  8.9  mat-tab SCSS overrides (Step 7)                                                                                                                                                                                             
  8.10 Final build + visual QA                                                                                                                                                                                                     
   
  ---                                                                                                                                                                                                                              
  Skills / techniques required                              
                                                                                                                                                                                                                                   
  ┌───────────────────────────────────┬────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
  │               Area                │                                                                                 What you need to know                                                                                  │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ CSS Grid                          │ Named template areas, minmax(), responsive collapse with @media. The split-pane sidebar uses sticky positioning inside a grid column.                                                  │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ CSS position: sticky              │ The sidebar sticks relative to the nearest scrolling ancestor. Ensure .detail-split__main (not the body) is the scroller, otherwise sticky breaks.                                     │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
  │ Angular computed()                │ All derived values (initials, totalPaid, daysActive, docsVerified) must be computed() signals so they update reactively when the source signals change. Never compute them in the      │   
  │                                   │ template.                                                                                                                                                                              │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Angular DatePipe                  │ Required for formatting changedAt timestamps in the status history and receipt meta. Import DatePipe into each affected component's imports array.                                     │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ Angular CurrencyPipe              │ Already imported in enquiry-detail — verify it's also imported in student-detail and faculty-detail when you add financial stats.                                                      │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ SCSS ::before pseudo-element      │ The vertical connector line between timeline dots uses ::before on .status-timeline__connector. It's an absolute-positioned line, not a real DOM element.                              │   
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
  │ @angular/material/tabs SCSS       │ Material's Angular 17+ MDC-based tabs use .mat-mdc-tab selectors. The older .mat-tab selectors will not match. Inspect the rendered DOM first to confirm the exact class names in the  │   
  │ override                          │ project's Material version.                                                                                                                                                            │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
  │ SVG inline icons                  │ The doc-row check/X/clock icons are inline SVG — no new icon library needed. Reuse the same SVG pattern used in the list screen action buttons.                                        │
  ├───────────────────────────────────┼────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤   
  │ font-variant-numeric:             │ A CSS property, not a font feature. No font loading change needed — it tells the browser to use tabular (fixed-width) digit glyphs from whichever font is active.                      │
  │ tabular-nums                      │                                                                                                                                                                                        │   
  └───────────────────────────────────┴────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                                                        
