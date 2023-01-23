truncate table sentence_adjustment_mapping;
alter table sentence_adjustment_mapping drop column nomis_sentence_adjustment_id;

alter table sentence_adjustment_mapping add column nomis_adjustment_id     bigint       not null;
alter table sentence_adjustment_mapping add column nomis_adjustment_type   varchar(20)       not null;

create unique index  nomis_id
    ON sentence_adjustment_mapping (nomis_adjustment_id, nomis_adjustment_type);
