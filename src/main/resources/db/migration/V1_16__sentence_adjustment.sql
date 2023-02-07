drop table sentence_adjustment_mapping;

create table sentencing_adjustment_mapping
(
    adjustment_id   varchar(64)  not null PRIMARY KEY,
    nomis_adjustment_id   bigint       not null,
    nomis_adjustment_category   varchar(20)  not null,
    label                          varchar(20),
    mapping_type                   varchar(20)  not null,
    when_created                   timestamp with time zone not null default now()
);

create index sentencing_adjustment_mapping_label_index on sentencing_adjustment_mapping (label);
create index sentencing_adjustment_mapping_when_created_index on sentencing_adjustment_mapping (when_created);
create unique index  nomis_id
    ON sentencing_adjustment_mapping (nomis_adjustment_id, nomis_adjustment_category);
