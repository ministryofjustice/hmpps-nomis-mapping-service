drop table if exists profile_mapping;

create table profile_mapping
(
    cpr_id                      uuid                     not null PRIMARY KEY,
    nomis_booking_id            bigint                   not null,
    nomis_profile_type          varchar(12)              not null,
    nomis_prison_number         varchar(10)              not null,
    when_created                timestamp with time zone not null default now(),
    when_updated                timestamp with time zone,
    label                       varchar(20),
    mapping_type                varchar(20)              not null,
    constraint profile_mapping_nomis_id_unique unique (nomis_booking_id, nomis_profile_type)
);
create index profile_mapping_when_created_index on profile_mapping (when_created);
create index profile_mapping_label_index on profile_mapping (label);
create index profile_mapping_offender_no on profile_mapping (nomis_prison_number);
create index profile_mapping_booking_id on profile_mapping (nomis_booking_id, nomis_profile_type);
