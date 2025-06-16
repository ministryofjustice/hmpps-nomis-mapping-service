create table visit_balance_adjustment_mapping
(
    nomis_id        bigint                      not null  PRIMARY KEY,
    dps_id          varchar(36)                 not null,
    when_created    timestamp with time zone    not null default now(),
    label           varchar(20),
    mapping_type    varchar(20)                 not null
);
create index visit_balance_adjustment_mapping_when_created_index on visit_balance_adjustment_mapping (when_created);
create index visit_balance_adjustment_mapping_label_index on visit_balance_adjustment_mapping (label);
