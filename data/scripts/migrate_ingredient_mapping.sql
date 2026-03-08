-- interaction_rules에 UNIQUE 제약 추가 (이미 있으면 무시)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'interaction_rules_drug_ingredient_nutrient_key'
    ) THEN
        ALTER TABLE interaction_rules ADD CONSTRAINT interaction_rules_drug_ingredient_nutrient_key UNIQUE (drug_ingredient, nutrient);
    END IF;
EXCEPTION
    WHEN duplicate_object THEN NULL; -- 이미 존재하면 무시
END $$;
