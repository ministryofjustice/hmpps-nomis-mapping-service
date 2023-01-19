create table sentence_adjustment_mapping
(
    nomis_sentence_adjustment_id   bigint       not null    UNIQUE,
    sentence_adjustment_id         bigint       not null    PRIMARY KEY,
    label                          varchar(20),
    mapping_type                   varchar(20)  not null,
    when_created                   timestamp with time zone not null default now()
);

create index sentence_adjustment_mapping_label_index on sentence_adjustment_mapping (label);
create index sentence_adjustment_mapping_when_created_index on sentence_adjustment_mapping (when_created);