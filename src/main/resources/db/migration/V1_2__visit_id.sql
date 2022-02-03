create table visit_id
(
    nomis_id bigint       not null  constraint visit_id_nomis_id_pkey   primary key,
    vsip_id  varchar(80)  not null  constraint visit_id_vsip_id_unique  unique,
    label    varchar(20),
    source   varchar(20)  not null
);

create index visit_id_label_index on visit_id (label);
