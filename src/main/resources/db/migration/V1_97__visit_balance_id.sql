delete from visit_balance_mapping;
alter table visit_balance_mapping drop column nomis_prison_number;

alter table visit_balance_mapping add column nomis_visit_balance_id bigint not null;
alter table visit_balance_mapping add constraint visit_balance_mapping_nomis_visit_balance_id_unique unique (nomis_visit_balance_id)

