alter table core_person_address_mapping
    add column
        nomis_prison_number varchar(10) not null;

alter table core_person_address_mapping
    add constraint
        nomis_prison_number_fk1 FOREIGN KEY (nomis_prison_number) REFERENCES core_person_mapping (nomis_prison_number);

create index core_person_address_mapping_nomis_prison_number_index ON core_person_address_mapping (nomis_prison_number);

alter table core_person_phone_mapping
    add column
        nomis_prison_number varchar(10) not null;

alter table core_person_phone_mapping
    add constraint
        nomis_prison_number_fk1 FOREIGN KEY (nomis_prison_number) REFERENCES core_person_mapping (nomis_prison_number);

create index core_person_phone_mapping_nomis_prison_number_index ON core_person_phone_mapping (nomis_prison_number);

alter table core_person_email_address_mapping
    add column
        nomis_prison_number varchar(10) not null;

alter table core_person_email_address_mapping
    add constraint
        nomis_prison_number_fk1 FOREIGN KEY (nomis_prison_number) REFERENCES core_person_mapping (nomis_prison_number);

create index core_person_email_address_mapping_nomis_prison_number_index ON core_person_email_address_mapping (nomis_prison_number);
