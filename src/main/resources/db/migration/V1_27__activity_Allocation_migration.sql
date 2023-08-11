create table allocation_migration_mapping (
    nomis_allocation_id            bigint       not null primary key,
    activity_allocation_id         bigint       not null,
    activity_schedule_id           bigint       not null,
    label                          varchar(20)  not null,
    when_created                   timestamp with time zone not null default now()
);

create index allocation_migration_activity_index on allocation_migration_mapping (activity_allocation_id);
create index allocation_migration_label_index on allocation_migration_mapping (label);
create index allocation_migration_when_created_index on allocation_migration_mapping (when_created);
