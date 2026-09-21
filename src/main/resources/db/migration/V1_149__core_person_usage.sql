create table core_person_address_usage_mapping
(
    cpr_id                    varchar(36)              not null PRIMARY KEY,
    nomis_id                  bigint                   not null,
    address_usage_code        varchar(10)              not null,
    nomis_prison_number       varchar(10)              not null,
    when_created              timestamp with time zone not null default now(),
    label                     varchar(20),
    mapping_type              varchar(20)              not null,
    constraint core_person_address_usage_mapping_nomis_id_unique unique (nomis_id, address_usage_code)
);
create index core_person_address_usage_mapping_when_created_index on offender_identifier_mapping (when_created);
create index core_person_address_usage_mapping_label_index on offender_identifier_mapping (label);
create index core_person_address_usage_mapping_prison_number_index on offender_identifier_mapping (nomis_prison_number);
