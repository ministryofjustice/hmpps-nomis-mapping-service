create table alert_mapping
(
    dps_alert_id         varchar(36)              not null PRIMARY KEY,
    nomis_booking_id     bigint                   not null,
    nomis_alert_sequence bigint                   not null,
    when_created         timestamp with time zone not null default now(),
    label                varchar(20),
    mapping_type         varchar(20)              not null,
    constraint alert_mapping_nomis_id_unique unique (nomis_booking_id, nomis_alert_sequence)
);
create index alert_mapping_when_created_index on alert_mapping (when_created);
create index alert_mapping_label_index on alert_mapping (label);
