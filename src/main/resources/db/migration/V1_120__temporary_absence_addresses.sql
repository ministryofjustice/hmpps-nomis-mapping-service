create table temporary_absence_address_mapping
(
    id                         bigint                   not null primary key,
    nomis_address_id           bigint                   not null,
    nomis_address_owner_class  varchar(12)              not null,
    nomis_offender_no          varchar(10),
    dps_address_text           varchar(255)             not null,
    dps_uprn                   bigint,
    when_created               timestamp with time zone not null default now(),
    when_updated               timestamp with time zone
);
create unique index temporary_absence_address_mapping_unique on temporary_absence_address_mapping (nomis_address_id, nomis_address_owner_class, nomis_offender_no, dps_uprn, dps_address_text);
create index temporary_absence_address_mapping_nomis_index on temporary_absence_address_mapping (nomis_address_owner_class, nomis_address_id);
create index temporary_absence_address_mapping_nomis_offender_index on temporary_absence_address_mapping (nomis_offender_no, nomis_address_id);
create index temporary_absence_address_mapping_dps_index on temporary_absence_address_mapping (nomis_address_owner_class, dps_uprn, dps_address_text);
create index temporary_absence_address_mapping_dps_offender_index on temporary_absence_address_mapping (nomis_offender_no, dps_uprn, dps_address_text);
