--
-- Title:      Property Value tables
-- Database:   MySQL InnoDB
-- Since:      V3.3 Schema 3001
-- Author:     Derek Hulley
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--

CREATE TABLE alf_prop_class
(
   id BIGINT NOT NULL AUTO_INCREMENT,
   java_class_name VARCHAR(255) NOT NULL,
   java_class_name_short VARCHAR(32) NOT NULL,
   java_class_name_crc BIGINT NOT NULL,
   UNIQUE INDEX idx_alf_prop_class_crc (java_class_name_crc, java_class_name_short),
   INDEX idx_alf_prop_class_class (java_class_name),
   PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_double_value
(
   id BIGINT NOT NULL AUTO_INCREMENT,
   double_value DOUBLE NOT NULL,
   INDEX idx_alf_prop_dbl_val (double_value),
   PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_string_value
(
   id BIGINT NOT NULL AUTO_INCREMENT,
   string_value text NOT NULL,
   INDEX idx_alf_prop_str_val (string_value(64)),
   PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_serializable_value
(
   id BIGINT NOT NULL AUTO_INCREMENT,
   serializable_value BLOB NOT NULL,
   PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_date_value
(
   date_value BIGINT NOT NULL,
   full_year SMALLINT NOT NULL,
   half_of_year TINYINT NOT NULL,
   quarter_of_year TINYINT NOT NULL,
   month_of_year TINYINT NOT NULL,
   week_of_year TINYINT NOT NULL,
   week_of_month TINYINT NOT NULL,
   day_of_year SMALLINT NOT NULL,
   day_of_month TINYINT NOT NULL,
   day_of_week TINYINT NOT NULL,
   INDEX idx_alf_prop_date_val (date_value),
   INDEX idx_alf_prop_date_fy (full_year),
   INDEX idx_alf_prop_date_moy (month_of_year),
   INDEX idx_alf_prop_date_doy (day_of_year),
   INDEX idx_alf_prop_date_dom (day_of_month),
   INDEX idx_alf_prop_date_dow (day_of_week),
   PRIMARY KEY (date_value)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_time_value
(
   time_value BIGINT NOT NULL,
   hour_of_day TINYINT NOT NULL,
   minute_of_hour TINYINT NOT NULL,
   second_of_minute TINYINT NOT NULL,
   ms_of_second TINYINT NOT NULL,
   INDEX idx_alf_prop_time_val (time_value),
   INDEX idx_alf_prop_time_hod (hour_of_day),
   INDEX idx_alf_prop_time_moh (minute_of_hour),
   INDEX idx_alf_prop_time_som (second_of_minute),
   INDEX idx_alf_prop_time_msos (ms_of_second),
   PRIMARY KEY (time_value)
) ENGINE=InnoDB;

CREATE TABLE alf_prop_value
(
   id BIGINT NOT NULL AUTO_INCREMENT,
   actual_type_id BIGINT NOT NULL,
   persisted_type TINYINT NOT NULL,
   long_value BIGINT NOT NULL,
   INDEX idx_alf_prop_per (persisted_type, long_value),
   INDEX idx_alf_prop_act (actual_type_id, long_value),
   PRIMARY KEY (id)
) ENGINE=InnoDB;

--
-- Record script finish
--
DELETE FROM alf_applied_patch WHERE id = 'patch.db-V3.3-PropertyValueTables';
INSERT INTO alf_applied_patch
  (id, description, fixes_from_schema, fixes_to_schema, applied_to_schema, target_schema, applied_on_date, applied_to_server, was_executed, succeeded, report)
  VALUES
  (
    'patch.db-V3.3-PropertyValueTables', 'Manually executed script upgrade V3.3: PropertyValue Tables',
    0, 3000, -1, 3001, null, 'UNKOWN', 1, 1, 'Script completed'
  );