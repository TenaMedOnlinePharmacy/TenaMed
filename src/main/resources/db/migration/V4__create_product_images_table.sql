-- Phase 1: Create product_images table
CREATE TABLE IF NOT EXISTS product_images (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id    UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    pharmacy_id   UUID REFERENCES pharmacies(id) ON DELETE CASCADE,
    image_url     VARCHAR(5000) NOT NULL,
    is_primary    BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_product_images_product_pharmacy_primary UNIQUE (product_id, pharmacy_id, is_primary)
);

CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_pharmacy_id ON product_images(pharmacy_id);
CREATE INDEX IF NOT EXISTS idx_product_images_product_pharmacy ON product_images(product_id, pharmacy_id);

-- Backfill: copy existing product.image_url into product_images as default (pharmacy_id = NULL)
INSERT INTO product_images (product_id, pharmacy_id, image_url, is_primary)
SELECT id, NULL, image_url, TRUE
FROM products
WHERE image_url IS NOT NULL AND image_url <> ''
ON CONFLICT DO NOTHING;
