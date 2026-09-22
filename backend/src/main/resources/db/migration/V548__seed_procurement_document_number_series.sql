-- Seeds the 4 number series backing the new document-numbering feature (Quotation Request,
-- Purchase Order, Goods Receipt, Return to Supplier). scope_type/prefix/separator/
-- sequence_padding are all editable afterward via Settings → Number Sequences (existing UI,
-- backend/src/main/java/com/cms/controller/NumberSeriesDefinitionController.java) with no code
-- change — these are just sensible starting defaults, deliberately varied to show the three
-- reset cadences the feature supports: annual (Quotation/PO), monthly (Goods Receipt, the
-- highest-volume of the four), and never (Return to Supplier, the lowest-volume).

INSERT INTO number_series_definitions
    (series_code, series_name, scope_type, prefix, separator, sequence_padding, description)
VALUES
    (
        'QUOTATION_REQUEST_NUMBER',
        'Quotation Request Number',
        'FINANCIAL_YEAR',
        'QR',
        '-',
        5,
        'Sequential number for Quotation Request (RFQ) documents, e.g. QR-2526-00001. Resets each financial year.'
    ),
    (
        'PURCHASE_ORDER_NUMBER',
        'Purchase Order Number',
        'FINANCIAL_YEAR',
        'PO',
        '-',
        5,
        'Sequential number for Purchase Order documents, e.g. PO-2526-00001. Resets each financial year.'
    ),
    (
        'GOODS_RECEIPT_NUMBER',
        'Goods Receipt Number',
        'FINANCIAL_MONTH',
        'GRN',
        '-',
        5,
        'Sequential number for Goods Receipt documents, e.g. GRN-202609-00001. Resets each month.'
    ),
    (
        'SUPPLIER_RETURN_NUMBER',
        'Supplier Return Number',
        'NONE',
        'SR',
        '-',
        5,
        'Sequential number for Return to Supplier documents, e.g. SR-00001. Never resets.'
    )
ON CONFLICT (series_code) DO NOTHING;
