-- Sample data so reviewers can try the API right away.
INSERT INTO inventory_item (sku, name, description, quantity, unit_price, created_at, updated_at) VALUES
    ('BOX-S-001',  'Small cardboard box', '30x20x15 cm, single wall',  500,  1.20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('PAL-EU-001', 'Euro pallet',         '1200x800 mm wooden pallet',  40, 25.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('TAPE-48-01', 'Packing tape',        '48 mm x 100 m, transparent', 300,  3.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
