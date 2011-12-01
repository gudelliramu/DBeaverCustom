/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import org.eclipse.osgi.util.NLS;

public class MySQLMessages extends NLS {
	static final String BUNDLE_NAME = "org.jkiss.dbeaver.ext.mysql.MySQLResources"; //$NON-NLS-1$

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, MySQLMessages.class);
	}

	private MySQLMessages() {
	}

	public static String dialog_connection_advanced_tab;
	public static String dialog_connection_advanced_tab_tooltip;
	public static String dialog_connection_general_tab;
	public static String dialog_connection_general_tab_tooltip;
	public static String dialog_connection_host;
	public static String dialog_connection_database;
	public static String dialog_connection_password;
	public static String dialog_connection_port;
	public static String dialog_connection_test_connection;
	public static String dialog_connection_user_name;
	public static String edit_catalog_manager_action_create_schema;
	public static String edit_catalog_manager_action_drop_schema;
	public static String edit_catalog_manager_dialog_schema_name;
	public static String edit_command_change_user_action_create_new_user;
	public static String edit_command_change_user_action_update_user_record;
	public static String edit_command_change_user_name;
	public static String edit_command_grant_privilege_action_grant_privilege;
	public static String edit_command_grant_privilege_name_revoke_privilege;
	public static String edit_constraint_manager_title;
	public static String edit_foreign_key_manager_title;
	public static String edit_index_manager_title;
	public static String edit_procedure_manager_action_create_procedure;
	public static String edit_procedure_manager_action_drop_procedure;
	public static String edit_procedure_manager_body;
	public static String edit_table_column_manager_action_alter_table_column;
	public static String edit_table_manager_action_rename_table;
	public static String edit_user_manager_command_create_user;
	public static String edit_user_manager_command_drop_user;
	public static String edit_user_manager_command_flush_privileges;
	public static String edit_view_manager_action_create_view;
	public static String edit_view_manager_action_drop_view;
	public static String edit_view_manager_definition;
	public static String editors_session_editor_action_kill_Session;
	public static String editors_session_editor_action_terminate_Query;
	public static String editors_session_editor_confirm;
	public static String editors_user_editor_abstract_load_grants;
	public static String editors_user_editor_general_control_dba_privileges;
	public static String editors_user_editor_general_group_limits;
	public static String editors_user_editor_general_group_login;
	public static String editors_user_editor_general_label_confirm;
	public static String editors_user_editor_general_label_host;
	public static String editors_user_editor_general_label_password;
	public static String editors_user_editor_general_label_user_name;
	public static String editors_user_editor_general_service_load_catalog_privileges;
	public static String editors_user_editor_general_spinner_max_connections;
	public static String editors_user_editor_general_spinner_max_queries;
	public static String editors_user_editor_general_spinner_max_updates;
	public static String editors_user_editor_general_spinner_max_user_connections;
	public static String editors_user_editor_privileges_column_catalog;
	public static String editors_user_editor_privileges_column_table;
	public static String editors_user_editor_privileges_control_other_privileges;
	public static String editors_user_editor_privileges_control_table_privileges;
	public static String editors_user_editor_privileges_group_catalogs;
	public static String editors_user_editor_privileges_group_tables;
	public static String editors_user_editor_privileges_service_load_privileges;
	public static String editors_user_editor_privileges_service_load_tables;
}
