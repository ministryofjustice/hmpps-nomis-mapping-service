create table csra_mapping
(
    dps_csra_id      uuid                     not null primary key,
    nomis_booking_id bigint                   not null,
    nomis_sequence   integer                  not null,
    offender_no      varchar(10)              not null,
    label            varchar(20),
    mapping_type     varchar(20)              not null,
    when_created     timestamp with time zone not null default now(),
    constraint csras_mapping_nomis_id_unique unique (nomis_booking_id, nomis_sequence)
);

create index csras_mapping_label_index on csra_mapping (label);
create index csras_mapping_when_created_index on csra_mapping (when_created);
create index csra_mapping_offender_no_index ON csra_mapping (offender_no);
