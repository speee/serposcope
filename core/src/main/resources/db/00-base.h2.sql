SET FOREIGN_KEY_CHECKS=0;
drop table if exists `CONFIG`;
create table `CONFIG` (
    name varchar(255) primary key,
    value text
) engine = innodb default charset=utf8 /*! collate utf8_bin */;

drop table if exists `USER`;
create table `USER` (
    id int primary key auto_increment,
    name varchar(255),
    email varchar(255),
    password_hash tinyblob,
    password_salt tinyblob,
    admin boolean,
    logout datetime
) engine = innodb default charset=utf8 /*! collate utf8_bin */;
create unique index USER_EMAIL on `USER`(email);

drop table if exists `GROUP`;
create table `GROUP` (
    id int primary key auto_increment,
    module_id int,
    name varchar(255),
    shared boolean,
    owner_id int,
    sunday_enabled boolean,
    monday_enabled boolean,
    tuesday_enabled boolean,
    wednesday_enabled boolean,
    thursday_enabled boolean,
    friday_enabled boolean,
    saturday_enabled boolean
) engine = innodb default charset=utf8 /*! collate utf8_bin */;
create unique index GROUP_NAME on `GROUP`(name);
create index GROUP_OWNER on `GROUP`(owner_id);

drop table if exists `EVENT`;
create table `EVENT` (
    group_id int,
    day date,
    title varchar(255),
    description text,
    primary key(group_id,day),
    foreign key (group_id) references `GROUP`(id)
) engine = innodb default charset=utf8 /*! collate utf8_bin */;


drop table if exists `USER_GROUP`;
create table `USER_GROUP` (
    user_id int,
    group_id int,
    primary key(user_id, group_id),
    foreign key (user_id) references `USER`(id),
    foreign key (group_id) references `GROUP`(id)
) engine = innodb default charset=utf8 /*! collate utf8_bin */;
create index USER_GROUP_GROUP_ID on `USER_GROUP`(group_id);


drop table if exists `RUN`;
create table `RUN` (
    id int primary key auto_increment,
    module_id int,
    day date,
    started datetime,
    finished datetime,
    total int,
    done int,
    progress int,
    captchas int,
    errors int,
    status int, -- running, aborted, finished, error
    mode int,
    user_id int,
    group_id int
) engine = innodb default charset=utf8 /*! collate utf8_bin */;
create index RUN_MODULE_ID_DAY on RUN(module_id,day);
create index RUN_USER_ID on RUN(user_id);
create index RUN_GROUP_ID on RUN(group_id);
create index RUN_DAY on RUN(day);

drop table if exists `PROXY`;
create table `PROXY` (
    `id` int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `type` int,
    `ip` text,
    `port` int,
    `user` text,
    `password` text,
    `last_check` datetime,
    `status` tinyint,
    `remote_ip` varchar(256),
    `instance_id` varchar(32),
    `region` varchar(32)
) engine = innodb default charset=utf8 /*! collate utf8_bin */;
create unique index PROXY_INSTANCE_ID on PROXY(instance_id);


SET FOREIGN_KEY_CHECKS=1;