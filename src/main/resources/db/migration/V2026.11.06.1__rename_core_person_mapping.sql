alter table core_person_mapping
    rename to core_person_migration_address_contact;

create view core_person_mapping as
select *
from core_person_migration_address_contact;
