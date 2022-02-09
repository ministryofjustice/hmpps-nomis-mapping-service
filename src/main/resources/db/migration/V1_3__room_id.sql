create table room_id
(
    id                         serial            not null primary key,
    nomis_room_description     varchar(240)      not null,
    prison_id                  varchar(6)        not null,
    vsip_id                    varchar(255)      not null,
    is_open                    boolean           not null
);

CREATE UNIQUE INDEX room_id_nomis_is_prison_id_index
    ON room_id (nomis_room_description, prison_id);


