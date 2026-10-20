CREATE TABLE products (
    sku              VARCHAR(32) PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    brand            VARCHAR(100),
    category         VARCHAR(100),
    attributes       JSONB NOT NULL DEFAULT '{}'::jsonb,
    price            NUMERIC(12,2),
    currency         CHAR(3),
    price_updated_at TIMESTAMPTZ,

    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple',
            coalesce(name, '')        || ' ' ||
            coalesce(brand, '')       || ' ' ||
            coalesce(category, '')    || ' ' ||
            coalesce(description, '') || ' ' ||
            coalesce(attributes::text, ''))
    ) STORED
);

CREATE INDEX idx_products_search ON products USING GIN (search_vector);

CREATE TABLE sync_state (
    name           VARCHAR(64) PRIMARY KEY,
    last_synced_at TIMESTAMPTZ NOT NULL
);

INSERT INTO sync_state VALUES ('pricing', '1970-01-01T00:00:00Z');
