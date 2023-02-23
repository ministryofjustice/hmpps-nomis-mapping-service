create table appointment_mapping (
    appointment_instance_id  bigint                   not null  primary key,
    nomis_event_id           bigint                   not null  unique,
    when_created             timestamp with time zone not null  default now(),
    constraint appointment_mapping_nomis_id_unique unique(nomis_event_id)
);
create index appointment_mapping_when_created_index on appointment_mapping (when_created);
