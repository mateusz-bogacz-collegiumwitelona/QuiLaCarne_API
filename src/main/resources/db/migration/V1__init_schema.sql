CREATE TABLE roles
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE  NOT NULL,
    name       VARCHAR(255) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE table_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(50) UNIQUE NOT NULL,
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

CREATE TABLE ban_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(100)       NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guest_report_status
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE  NOT NULL,
    name       VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE restaurant_tables
(
    id           UUID PRIMARY KEY,
    token        VARCHAR(64) UNIQUE NOT NULL,
    table_number INTEGER UNIQUE     NOT NULL,
    capacity     INTEGER            NOT NULL,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ingredients
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    name       VARCHAR(100)       NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE allergens
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
    is_available       BOOLEAN     DEFAULT TRUE,
    unavailable_reason TEXT,
    created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    user_id    UUID REFERENCES users (id),
    table_id   UUID REFERENCES restaurant_tables (id),
    start_time TIMESTAMPTZ        NOT NULL,
    end_time   TIMESTAMPTZ        NOT NULL,
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
    created_at             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs
(
    id         UUID PRIMARY KEY,
    token      VARCHAR(64) UNIQUE NOT NULL,
    user_id    UUID REFERENCES users (id),
    action     VARCHAR(255)       NOT NULL,
    ip_address VARCHAR(255),
    details    JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE guest_reports
(
    id          UUID PRIMARY KEY,
    token       VARCHAR(64) UNIQUE NOT NULL,
    guest_id    UUID REFERENCES users (id) ON DELETE CASCADE,
    reporter_id UUID REFERENCES users (id),
    reason      TEXT               NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bans
(
    id           UUID PRIMARY KEY,
    token        VARCHAR(64) UNIQUE NOT NULL,
    user_id      UUID REFERENCES users (id) ON DELETE CASCADE,
    banned_by_id UUID REFERENCES users (id),
    reason       TEXT               NOT NULL,
    expires_at   TIMESTAMPTZ,
    is_active    BOOLEAN     DEFAULT TRUE,
    created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE x_user_roles
(
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    role_id UUID REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE x_dish_composition
(
    dish_id       UUID REFERENCES dishes (id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients (id) ON DELETE CASCADE,
    quantity      VARCHAR(50),
    PRIMARY KEY (dish_id, ingredient_id)
);

CREATE TABLE x_ingredient_allergens
(
    ingredient_id UUID REFERENCES ingredients (id) ON DELETE CASCADE,
    allergen_id   UUID REFERENCES allergens (id) ON DELETE CASCADE,
    PRIMARY KEY (ingredient_id, allergen_id)
);

CREATE TABLE x_table_status
(
    table_id        UUID REFERENCES restaurant_tables (id) ON DELETE CASCADE,
    table_status_id UUID REFERENCES table_status (id) ON DELETE CASCADE,
    PRIMARY KEY (table_id, table_status_id)
);

CREATE TABLE x_reservations_status
(
    reservations_id        UUID REFERENCES reservations (id) ON DELETE CASCADE,
    reservations_status_id UUID REFERENCES reservations_status (id) ON DELETE CASCADE,
    PRIMARY KEY (reservations_id, reservations_status_id)
);

CREATE TABLE x_order_status
(
    order_id        UUID REFERENCES orders (id) ON DELETE CASCADE,
    order_status_id UUID REFERENCES order_status (id) ON DELETE CASCADE,
    PRIMARY KEY (order_id, order_status_id)
);

CREATE TABLE x_order_items_status
(
    order_items_id        UUID REFERENCES order_items (id) ON DELETE CASCADE,
    order_items_status_id UUID REFERENCES order_items_status (id) ON DELETE CASCADE,
    PRIMARY KEY (order_items_id, order_items_status_id)
);

CREATE TABLE x_ban_status
(
    ban_id        UUID REFERENCES bans (id) ON DELETE CASCADE,
    ban_status_id UUID REFERENCES ban_status (id) ON DELETE CASCADE,
    PRIMARY KEY (ban_id, ban_status_id)
);

CREATE TABLE x_guest_report_status
(
    guest_report_id UUID REFERENCES guest_reports (id) ON DELETE CASCADE,
    status_id       UUID REFERENCES guest_report_status (id) ON DELETE CASCADE,
    PRIMARY KEY (guest_report_id, status_id)
);
