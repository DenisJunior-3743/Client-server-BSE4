-- Same five products the web/mobile mock data shipped with in Task 1.
INSERT INTO loan_products (name, type, interest_rate_annual, min_amount, max_amount, min_term_months, max_term_months) VALUES
    ('Personal Loan',        'PERSONAL',      19.00,  500000, 15000000,  3, 36),
    ('Business Growth Loan', 'BUSINESS',      21.00, 2000000, 80000000,  6, 60),
    ('Agriculture Loan',     'AGRICULTURE',   16.00,  500000, 25000000,  3, 24),
    ('Asset Finance',        'ASSET_FINANCE', 18.00, 3000000, 60000000, 12, 48),
    ('Emergency Loan',       'EMERGENCY',     24.00,  200000,  5000000,  1, 12);
