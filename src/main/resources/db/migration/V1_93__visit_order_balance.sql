create table visit_order_balance_mapping
(
    dps_id              varchar(36)              not null PRIMARY KEY,
    nomis_prison_number varchar(10)              not null,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint visit_order_balance_mapping_nomis_prison_number_unique unique (nomis_prison_number)
);
create index visit_order_balance_mapping_when_created_index on visit_order_balance_mapping (when_created);
create index visit_order_balance_mapping_label_index on visit_order_balance_mapping (label);
