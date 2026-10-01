alter table core_person_address_mapping
    drop constraint nomis_prison_number_fk1;

alter table core_person_phone_mapping
    drop constraint nomis_prison_number_fk1;

alter table core_person_email_address_mapping
    drop constraint nomis_prison_number_fk1;
