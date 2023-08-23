alter table activity_migration_mapping
    rename column activity_schedule_id to activity_id;

alter table activity_migration_mapping
    rename column activity_schedule_id2 to activity_id2;

alter table allocation_migration_mapping
    rename column activity_schedule_id to activity_id;