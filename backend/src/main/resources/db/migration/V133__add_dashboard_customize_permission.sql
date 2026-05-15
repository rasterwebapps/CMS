-- DASHBOARD_CUSTOMIZE: grants a user the right to save a personal dashboard layout
-- that overrides their role's default.  Assign to roles whose members should be
-- allowed to personalise their own dashboard (e.g. admin, faculty, front-office).

INSERT INTO permissions (code, display_name, category, description)
SELECT 'DASHBOARD_CUSTOMIZE',
       'Customise Personal Dashboard',
       'DASHBOARD',
       'Allows the user to save a personal widget layout that overrides the role default'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE code = 'DASHBOARD_CUSTOMIZE');
