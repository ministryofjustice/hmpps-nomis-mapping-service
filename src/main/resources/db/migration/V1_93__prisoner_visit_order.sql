create table prisoner_visit_order_mapping
(
    dps_id              varchar(36)              not null PRIMARY KEY,
    nomis_prison_number varchar(10)              not null,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint prisoner_visit_order_mapping_nomis_prison_number_unique unique (nomis_prison_number)
);
create index prisoner_visit_order_mapping_when_created_index on prisoner_visit_order_mapping (when_created);
create index prisoner_visit_order_mapping_label_index on prisoner_visit_order_mapping (label);
