create table core_person_mapping
(
    cpr_id        varchar(36)              not null PRIMARY KEY,
    prison_number varchar(10)              not null,
    when_created  timestamp with time zone not null default now(),
    label         varchar(20),
    mapping_type  varchar(20)              not null,
    constraint core_person_mapping_prison_number_unique unique (prison_number)
);
create index core_person_mapping_when_created_index on core_person_mapping (when_created);
create index core_person_mapping_label_index on core_person_mapping (label);
