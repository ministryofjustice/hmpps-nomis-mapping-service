create table offender_alias_mapping
(
    cpr_id                    varchar(36)              not null PRIMARY KEY,
    nomis_offender_id         bigint                   not null,
    nomis_prison_number       varchar(10)              not null,
    when_created              timestamp with time zone not null default now(),
    label                     varchar(20),
    mapping_type              varchar(20)              not null,
    constraint offender_alias_mapping_nomis_id_unique unique (nomis_offender_id)
);
create index offender_alias_mapping_when_created_index on offender_alias_mapping (when_created);
create index offender_alias_mapping_label_index on offender_alias_mapping (label);
create index offender_alias_mapping_nomis_prison_number_index on offender_alias_mapping (nomis_prison_number);
