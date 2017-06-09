alter table manifest_item  add column version int not null default 1;
alter table audit_log_item add column version int not null default 1;
alter table bit_report add column version int not null default 1;
alter table bit_log_item  add column version int not null default 1;
alter table space_stats add column version int not null default 1;

