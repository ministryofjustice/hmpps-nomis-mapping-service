create table adjudication_punishment_mapping
(
    nomis_booking_id        bigint                   not null,
    nomis_sanction_sequence int                      not null,
    dps_punishment_Id       varchar(30)              not null primary key,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null,
    constraint adjudication_punishment_mapping_nomis_id_unique unique (nomis_booking_id, nomis_sanction_sequence)
);
create index adjudication_punishment_mapping_when_created_index on adjudication_punishment_mapping (when_created);
create index adjudication_punishment_mapping_label_index on adjudication_punishment_mapping (label);
