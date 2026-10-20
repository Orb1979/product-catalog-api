INSERT INTO products (sku, name, description, brand, category, attributes)
SELECT
    'SKU-' || lpad(i::text, 6, '0'),
    (ARRAY['Trail','City','Studio','Pro','Eco'])[1 + i % 5] || ' ' ||
    (ARRAY['Backpack','Lamp','Kettle','Jacket','Headphones','Chair'])[1 + i % 6] || ' ' || i,
    'Description for product ' || i || '. Durable and well designed.',
    (ARRAY['Acme','Nordic','Zenith','Brightly'])[1 + i % 4],
    (ARRAY['Outdoor','Home','Kitchen','Clothing','Audio','Furniture'])[1 + i % 6],
    jsonb_build_object(
        'color', (ARRAY['red','blue','black','green','white'])[1 + i % 5],
        'size',  (ARRAY['S','M','L','XL'])[1 + i % 4])
FROM generate_series(1, 10000) AS i;
