-- V263: Configurable thermal label-printer output (ZPL) for library barcode printing.
--   Adds settings for a three-way print-transport switch (BROWSER default / NETWORK / LOCAL_AGENT)
--   used alongside the existing barcode_label_width_mm/height_mm settings from V260. No new
--   permissions are added — printing over any transport is still gated by the existing
--   LIBRARY_CATALOGUE_PRINT_BARCODE / LIBRARY_PERIODICAL_PRINT_BARCODE permissions, since this is
--   the same operation with a different delivery mechanism, not a new one.
INSERT INTO library_settings (setting_key, setting_value, display_name, description, data_type) VALUES
    ('barcode_printer_mode',    'BROWSER', 'Barcode Print Mode',
        'BROWSER (default browser print dialog, unchanged behaviour) | NETWORK (server streams ZPL directly to a networked label printer) | LOCAL_AGENT (browser sends ZPL to a local Browser Print-style agent for a USB-attached printer)',
        'STRING'),
    ('barcode_printer_ip',      '',        'Label Printer IP Address',
        'LAN IP address of the networked label printer (NETWORK mode only); must be a private/RFC1918 address',
        'STRING'),
    ('barcode_printer_port',    '9100',    'Label Printer Port',
        'Raw ZPL listener port on the networked label printer (NETWORK mode only), default 9100',
        'INTEGER'),
    ('barcode_labels_per_row',  '1',       'Labels Per Row',
        'How many labels are physically die-cut across the loaded label roll width (1, 2, or 4) — used only for batch label-sheet printing',
        'INTEGER')
ON CONFLICT (setting_key) DO NOTHING;
