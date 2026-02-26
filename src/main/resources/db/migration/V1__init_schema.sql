CREATE TABLE users
(
    id                 UUID PRIMARY KEY,
    token              VARCHAR(64) UNIQUE  NOT NULL,
    username           VARCHAR(100) UNIQUE NOT NULL,
    email              VARCHAR(255) UNIQUE NOT NULL,
    password           VARCHAR(255)        NOT NULL,
    two_factor_enabled BOOLEAN     DEFAULT FALSE,
    mfa_secret         VARCHAR(255),
    is_active          BOOLEAN     DEFAULT FALSE,
    created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE roles
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles
(
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE audit_logs
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id),
    action     VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255),
    details    JSONB, -- Poprawiono z JSOB
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dishes_categories
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(100)       NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dishes
(
    id                 UUID PRIMARY KEY,
    token              VARCHAR(64) UNIQUE NOT NULL,
    category_id        UUID REFERENCES dishes_categories (id),
    name               VARCHAR(255)       NOT NULL,
    description        TEXT,
    price              INTEGER            NOT NULL,
    is_available       BOOLEAN     DEFAULT TRUE, -- Poprawiono literówkę
    unavailable_reason TEXT,                     -- Poprawiono literówkę
    created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingredients
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(100)       NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE dish_composition
(
    dish_id       UUID REFERENCES dishes (id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients (id) ON DELETE CASCADE,
    quantity      VARCHAR(50),
    PRIMARY KEY (dish_id, ingredient_id)
);

CREATE TABLE allergens
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(100)       NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingredient_allergens
(
    ingredient_id UUID REFERENCES ingredients (id) ON DELETE CASCADE,
    allergen_id   UUID REFERENCES allergens (id) ON DELETE CASCADE,
    PRIMARY KEY (ingredient_id, allergen_id)
);

CREATE TABLE restaurant_tables
( -- Zmieniono z TABLES na restaurant_tables
    id           UUID PRIMARY KEY,
    token        VARCHAR(64) UNIQUE NOT NULL,
    table_number INTEGER UNIQUE     NOT NULL,
    capacity     INTEGER            NOT NULL,
    status       VARCHAR(50)        NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    user_id    UUID REFERENCES users (id),
    table_id   UUID REFERENCES restaurant_tables (id),
    start_time TIMESTAMPTZ        NOT NULL,
    end_time   TIMESTAMPTZ        NOT NULL,
    status     VARCHAR(50)        NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders
(
    id             UUID PRIMARY KEY,
    token          VARCHAR(64) UNIQUE NOT NULL,
    table_id       UUID REFERENCES restaurant_tables (id),
    waiter_id      UUID REFERENCES users (id),
    reservation_id UUID REFERENCES reservations (id),
    total_price    INTEGER     DEFAULT 0,
    status         VARCHAR(50)        NOT NULL,
    created_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items
(
    id                     UUID PRIMARY KEY,
    token                  VARCHAR(64) UNIQUE NOT NULL,
    order_id               UUID REFERENCES orders (id) ON DELETE CASCADE,
    product_id             UUID REFERENCES dishes (id),
    quantity               INTEGER            NOT NULL,
    price_at_time_of_order INTEGER            NOT NULL,
    note                   TEXT,
    item_status            VARCHAR(50)        NOT NULL,
    created_at             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);