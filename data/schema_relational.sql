-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Drugs Table (Master)
CREATE TABLE IF NOT EXISTS drugs (
    id BIGSERIAL PRIMARY KEY,
    item_seq VARCHAR(255) NOT NULL UNIQUE,
    item_name VARCHAR(255) NOT NULL,
    entp_name VARCHAR(255),
    etc_otc_code VARCHAR(255),
    chart VARCHAR(255),
    material_name VARCHAR(255),
    main_item_ingr VARCHAR(255),
    ingr_name VARCHAR(255),
    atc_code VARCHAR(255),
    total_content VARCHAR(255),
    big_prdt_img_url VARCHAR(1000)
);

-- Drug Identification Table (1:1 with Drugs)
CREATE TABLE IF NOT EXISTS drug_identification (
    id BIGSERIAL PRIMARY KEY,
    drug_id BIGINT NOT NULL,
    drug_shape VARCHAR(255),
    color_class1 VARCHAR(255),
    color_class2 VARCHAR(255),
    print_front VARCHAR(255),
    print_back VARCHAR(255),
    line_front VARCHAR(255),
    line_back VARCHAR(255),
    form_code_name VARCHAR(255),
    item_image VARCHAR(1000),
    class_name VARCHAR(255),
    CONSTRAINT fk_drug_identification_drug FOREIGN KEY (drug_id) REFERENCES drugs(id)
);

-- Drug Ingredients Table (N:1 with Drugs)
CREATE TABLE IF NOT EXISTS drug_ingredients (
    id BIGSERIAL PRIMARY KEY,
    drug_id BIGINT NOT NULL,
    mtral_code VARCHAR(255),
    mtral_nm VARCHAR(255),
    qnt VARCHAR(255),
    ingd_unit_cd VARCHAR(255),
    main_ingr_eng VARCHAR(255),
    CONSTRAINT fk_drug_ingredients_drug FOREIGN KEY (drug_id) REFERENCES drugs(id)
);

-- Contraindications Table (Raw Data)
CREATE TABLE IF NOT EXISTS contraindications (
    id BIGSERIAL PRIMARY KEY,
    ingredient_name1 VARCHAR(255),
    ingredient_code1 VARCHAR(255),
    product_code1 VARCHAR(255),
    product_name1 VARCHAR(255),
    company_name1 VARCHAR(255),
    ingredient_name2 VARCHAR(255),
    ingredient_code2 VARCHAR(255),
    product_code2 VARCHAR(255),
    product_name2 VARCHAR(255),
    company_name2 VARCHAR(255),
    notice_no VARCHAR(255),
    notice_date VARCHAR(255),
    reason TEXT
);

-- Interaction Rules Table (Processed Rules)
CREATE TABLE IF NOT EXISTS interaction_rules (
    id BIGSERIAL PRIMARY KEY,
    drug_ingredient VARCHAR(255) NOT NULL,
    nutrient VARCHAR(255) NOT NULL,
    level VARCHAR(255) NOT NULL, -- ENUM: SAFE, CAUTION, WARNING, SYNERGY
    description TEXT,
    action TEXT,
    UNIQUE(drug_ingredient, nutrient)
);

-- Supplements Table (Master)
CREATE TABLE IF NOT EXISTS supplements (
    id BIGSERIAL PRIMARY KEY,
    prdlst_nm VARCHAR(255) NOT NULL,
    bssh_nm VARCHAR(255),
    primary_fnclty TEXT,
    rawmtrl_nm TEXT,
    indiv_rawmtrl_nm TEXT,
    etc_rawmtrl_nm TEXT,
    ntk_mthd TEXT,
    iftkn_atnt_matr_cn TEXT,
    stdr_stnd TEXT
);

-- User Drugs Table (User's Prescription Drugs)
CREATE TABLE IF NOT EXISTS user_drugs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    drug_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_user_drugs_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_drugs_drug FOREIGN KEY (drug_id) REFERENCES drugs(id)
);

-- User Supplements Table (User's Registered Supplements)
CREATE TABLE IF NOT EXISTS user_supplements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    supplement_name VARCHAR(255) NOT NULL,
    nutrients JSONB, -- OCR result as JSONB
    registered_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_supplements_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Analysis Logs Table
CREATE TABLE IF NOT EXISTS analysis_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    case_type VARCHAR(50) NOT NULL, -- CASE_A, CASE_B
    request JSONB,
    response JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_analysis_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Drug Price Master Table (For code mapping)
CREATE TABLE IF NOT EXISTS drug_price_master (
    id BIGSERIAL PRIMARY KEY,
    item_name VARCHAR(255),
    entp_name VARCHAR(255),
    drug_spec VARCHAR(255),
    form_type VARCHAR(255),
    pkg_type VARCHAR(255),
    std_code VARCHAR(255) UNIQUE,
    permit_date VARCHAR(255),
    etc_otc_type VARCHAR(255),
    represent_code VARCHAR(255),
    bar_code VARCHAR(255),
    ingr_code VARCHAR(255),
    atc_code VARCHAR(255)
);

