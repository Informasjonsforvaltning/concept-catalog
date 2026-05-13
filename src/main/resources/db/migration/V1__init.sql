CREATE TABLE concepts (
    id                       VARCHAR(255) PRIMARY KEY,
    originalt_begrep         VARCHAR(255) NOT NULL,
    ansvarlig_virksomhet_id  VARCHAR(255) NOT NULL,
    status                   VARCHAR(50),
    er_publisert             BOOLEAN DEFAULT FALSE,
    is_archived              BOOLEAN DEFAULT FALSE,
    data                     JSONB NOT NULL
);

CREATE INDEX idx_concepts_org ON concepts (ansvarlig_virksomhet_id);
CREATE INDEX idx_concepts_org_status ON concepts (ansvarlig_virksomhet_id, status);
CREATE INDEX idx_concepts_originalt ON concepts (originalt_begrep);
CREATE INDEX idx_concepts_originalt_archived ON concepts (originalt_begrep, is_archived);
CREATE INDEX idx_concepts_originalt_publisert ON concepts (originalt_begrep, er_publisert);

CREATE TABLE change_requests (
    id                 VARCHAR(255) PRIMARY KEY,
    concept_id         VARCHAR(255),
    catalog_id         VARCHAR(255) NOT NULL,
    status             VARCHAR(50) NOT NULL,
    time_for_proposal  TIMESTAMP NOT NULL,
    title              VARCHAR(1000) NOT NULL,
    proposed_by        JSONB NOT NULL,
    operations         JSONB NOT NULL,
    concept_snapshot   JSONB
);

CREATE INDEX idx_cr_catalog ON change_requests (catalog_id);
CREATE INDEX idx_cr_catalog_status ON change_requests (catalog_id, status);
CREATE INDEX idx_cr_catalog_concept ON change_requests (catalog_id, concept_id);
CREATE INDEX idx_cr_concept_status ON change_requests (concept_id, status);

CREATE TABLE import_results (
    id                   VARCHAR(255) PRIMARY KEY,
    created              TIMESTAMP NOT NULL,
    catalog_id           VARCHAR(255) NOT NULL,
    status               VARCHAR(50) NOT NULL,
    total_concepts       INTEGER DEFAULT 0,
    extracted_concepts   INTEGER DEFAULT 0,
    saved_concepts       INTEGER DEFAULT 0,
    failure_message      TEXT,
    concept_extractions  JSONB NOT NULL DEFAULT '[]'
);

CREATE INDEX idx_ir_catalog ON import_results (catalog_id);
