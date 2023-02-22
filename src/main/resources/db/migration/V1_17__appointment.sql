create table appointment_mapping (
    appointment_instance_id  bigint       not null  PRIMARY KEY,
    nomis_event_id           bigint       not null,
    mapping_type             varchar(20)  not null,
    constraint appointment_mapping_nomis_id_unique unique(nomis_event_id)
);
