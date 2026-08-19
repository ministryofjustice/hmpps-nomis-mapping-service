create table offender_identifier_mapping
(
    cpr_id                    varchar(36)              not null PRIMARY KEY,
    nomis_offender_id         bigint                   not null,
    nomis_identifier_sequence int                      not null,
    nomis_prison_number       varchar(10)              not null,
    when_created              timestamp with time zone not null default now(),
    label                     varchar(20),
    mapping_type              varchar(20)              not null,
    constraint offender_identifier_mapping_nomis_id_unique unique (nomis_offender_id, nomis_identifier_sequence)
);
create index offender_identifier_mapping_when_created_index on offender_identifier_mapping (when_created);
create index offender_identifier_mapping_label_index on offender_identifier_mapping (label);
create index offender_identifier_mapping_nomis_prison_number_index on offender_identifier_mapping (nomis_prison_number);
