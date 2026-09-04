drop index if exists property_container_mapping_offender_no_index;
alter table property_container_mapping drop column if exists offender_no;
