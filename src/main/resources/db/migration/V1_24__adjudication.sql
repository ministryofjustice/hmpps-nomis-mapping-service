drop table adjudication_mapping;
create table adjudication_mapping
(
    adjudication_number bigint                   not null,
    charge_sequence     int                      not null,
    charge_number       varchar(30)              not null primary key,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint adjudication_mapping_nomis_id_unique unique (adjudication_number, charge_sequence)
);
create index adjudication_mapping_when_created_index on adjudication_mapping (when_created);
create index adjudication_mapping_label_index on adjudication_mapping (label);
