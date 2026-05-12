create table court_scheduler_migration
(
    offender_no             varchar(10)              not null PRIMARY KEY,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20)
);
