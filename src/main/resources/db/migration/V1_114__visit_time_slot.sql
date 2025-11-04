create table visit_time_slot_mapping
(
    dps_id              varchar(36)              not null PRIMARY KEY,
    nomis_prison_id     varchar(6)               not null,
    nomis_day_of_week   varchar(9)               not null,
    nomis_slot_sequence int                      not null,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint visit_time_slot_mapping_nomis_id_unique unique (nomis_prison_id, nomis_day_of_week, nomis_slot_sequence)
);
create index visit_time_slot_mapping_when_created_index on visit_time_slot_mapping (when_created);
create index visit_time_slot_mapping_label_index on visit_time_slot_mapping (label);
