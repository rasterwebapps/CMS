CREATE TABLE app_roles (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100)  NOT NULL UNIQUE,
    display_name    VARCHAR(150)  NOT NULL,
    hierarchy_level INTEGER       NOT NULL,
    is_system_role  BOOLEAN       NOT NULL DEFAULT FALSE,
    description     VARCHAR(500),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE permissions (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(100)  NOT NULL UNIQUE,
    display_name VARCHAR(150)  NOT NULL,
    category     VARCHAR(50)   NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL REFERENCES app_roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE app_users (
    id                 BIGSERIAL PRIMARY KEY,
    keycloak_username  VARCHAR(255) NOT NULL UNIQUE,
    keycloak_user_id   VARCHAR(255),
    email              VARCHAR(255) NOT NULL UNIQUE,
    full_name          VARCHAR(255) NOT NULL,
    app_role_id        BIGINT       REFERENCES app_roles(id),
    is_active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by         VARCHAR(255),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_app_users_keycloak_username ON app_users(keycloak_username);
CREATE INDEX idx_app_users_app_role_id       ON app_users(app_role_id);
CREATE INDEX idx_role_permissions_role_id    ON role_permissions(role_id);
