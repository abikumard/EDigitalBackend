-- =========================================================
-- ContentHub / MediaVault database schema
-- Run this once in your MySQL (Workbench / CLI / etc) BEFORE
-- starting the Spring Boot backend.
--
--   mysql -u root -p < schema.sql
--
-- If you rename the database, also update
-- spring.datasource.url in application.properties.
-- =========================================================

CREATE DATABASE IF NOT EXISTS contenthub_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE contenthub_db;

-- ---------------------------------------------------------
-- Users who log in on the public site via email/mobile + password.
-- Exactly one of email / mobile is required (enforced in the app layer),
-- both are optional at the DB level so either signup path works.
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(150) NULL UNIQUE,
    mobile          VARCHAR(15) NULL UNIQUE,
    password_hash   VARCHAR(255) NULL,
    name            VARCHAR(150) NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at   DATETIME NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Seller applications (KYC). One row per user who has applied to sell.
-- status: PENDING | APPROVED | REJECTED — reviewed by admin only.
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS sellers (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE,
    business_name           VARCHAR(200) NOT NULL,
    account_holder_name     VARCHAR(200) NOT NULL,
    bank_account_number     VARCHAR(30) NOT NULL,
    ifsc_code               VARCHAR(15) NOT NULL,
    bank_name               VARCHAR(150) NOT NULL,
    pan_number              VARCHAR(15) NOT NULL,
    phone                   VARCHAR(15) NOT NULL,
    address                 VARCHAR(500) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    applied_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at             DATETIME NULL,
    rejection_reason        VARCHAR(500) NULL,
    CONSTRAINT fk_seller_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Wishlist and cart. Hibernate will auto-create these on next backend
-- restart even without running this manually (ddl-auto=update creates new
-- tables reliably; it's altering existing columns that needs manual SQL).
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS wishlist_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_content FOREIGN KEY (content_id) REFERENCES content_items(id) ON DELETE CASCADE,
    CONSTRAINT uq_wishlist_user_content UNIQUE (user_id, content_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS cart_items (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    content_id  BIGINT NOT NULL,
    added_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_content FOREIGN KEY (content_id) REFERENCES content_items(id) ON DELETE CASCADE,
    CONSTRAINT uq_cart_user_content UNIQUE (user_id, content_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Legacy: one-time-passwords, no longer used for public login.
-- Kept (unused) in case you want a "Forgot password" email-OTP
-- flow later — safe to drop this table if you don't need that.
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS otp_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(150) NOT NULL,
    otp_code        VARCHAR(10) NOT NULL,
    expires_at      DATETIME NOT NULL,
    is_used         TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_otp_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Paid content items uploaded by the admin
-- content_type: VIDEO | PDF | PHOTO
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id           BIGINT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT NULL,
    price               DECIMAL(10,2) NOT NULL,
    content_type        VARCHAR(20) NOT NULL,
    thumbnail_path      VARCHAR(500) NOT NULL,
    file_path           VARCHAR(500) NOT NULL,
    original_file_name  VARCHAR(255) NULL,
    is_active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_content_seller FOREIGN KEY (seller_id) REFERENCES sellers(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Extra files bundled onto a product — e.g. a "3 photos" pack, or a
-- couple of bonus PDFs alongside the main file. A product's core price/
-- access/thumbnail still lives on content_items; this just adds more
-- downloadable files under the same purchase.
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS content_files (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_item_id     BIGINT NOT NULL,
    file_type           VARCHAR(20) NOT NULL,
    file_path           VARCHAR(500) NOT NULL,
    original_file_name  VARCHAR(255) NULL,
    label               VARCHAR(150) NULL,
    CONSTRAINT fk_content_file_item FOREIGN KEY (content_item_id) REFERENCES content_items(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Purchases (Razorpay orders/payments)
-- status: CREATED | SUCCESS | FAILED
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchases (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    content_id              BIGINT NOT NULL,
    amount                  DECIMAL(10,2) NOT NULL,
    razorpay_order_id       VARCHAR(100) NOT NULL,
    razorpay_payment_id     VARCHAR(100) NULL,
    razorpay_signature      VARCHAR(255) NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_purchase_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_content
        FOREIGN KEY (content_id) REFERENCES content_items(id) ON DELETE CASCADE,
    INDEX idx_purchase_user_content (user_id, content_id),
    INDEX idx_purchase_status (status)
) ENGINE=InnoDB;
