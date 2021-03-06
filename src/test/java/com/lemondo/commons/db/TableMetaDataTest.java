package com.lemondo.commons.db;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import com.lemondo.commons.db.exception.InvalidFieldException;
import com.lemondo.commons.db.meta.FilterCondition;
import com.lemondo.commons.db.meta.TableMetaData;

public class TableMetaDataTest extends TestCase {

	private TableMetaData metaDataWithDeactivatedFlag;
	private TableMetaData metaDataWithoutDeactivatedFlag;

	private final Map<String, Integer> columnDef;

	public TableMetaDataTest(String name) {
		super(name);

		columnDef = new HashMap<String, Integer>();
		columnDef.put("empcode", Types.INTEGER);
		columnDef.put("loginname", Types.VARCHAR);
		columnDef.put("password", Types.VARCHAR);
		columnDef.put("loginenabled", Types.VARCHAR);
	}

	protected void setUp() throws Exception {
		super.setUp();

		metaDataWithDeactivatedFlag = new TableMetaData("test_table", columnDef, PrimarykeyType.VARCHAR, true);
		metaDataWithoutDeactivatedFlag = new TableMetaData("test_table", columnDef, PrimarykeyType.VARCHAR, false);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetColumnDef() {
		assertEquals(metaDataWithDeactivatedFlag.getColumnDef(), columnDef);
	}

	public void testGenInsertSqlNoGenKey() throws InvalidFieldException {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("INSERT INTO test_table (`id`");
		for (String col : columns) {
			expected.append(",`").append(col).append("`");
		}
		expected.append(") VALUES (?,?,?,?,?)");

		String actual = metaDataWithDeactivatedFlag.genInsertSql(columns, false);

		assertEquals(expected.toString(), actual);
	}

	public void testGenInsertSqlWithGenKey() throws InvalidFieldException {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("INSERT INTO test_table");
		String prefix = " (";
		for (String col : columns) {
			expected.append(prefix).append("`").append(col).append("`");
			prefix = ",";
		}
		expected.append(") VALUES (?,?,?,?)");

		String actual = metaDataWithDeactivatedFlag.genInsertSql(columns, true);

		assertEquals(expected.toString(), actual);
	}

	public void testGenUpdateSqlWithDeactivatedFlag() throws InvalidFieldException {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("UPDATE test_table SET ");
		for (String col : columns) {
			expected.append("`").append(col).append("`=?,");
		}
		expected.deleteCharAt(expected.length() - 1);
		expected.append(" WHERE `id`=? AND `deactivated`=0");

		String actual = metaDataWithDeactivatedFlag.genUpdateSql(columns);

		assertEquals(expected.toString(), actual);
	}

	public void testGenUpdateSqlWithoutDeactivatedFlag() throws InvalidFieldException {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("UPDATE test_table SET ");
		for (String col : columns) {
			expected.append("`").append(col).append("`=?,");
		}
		expected.deleteCharAt(expected.length() - 1);
		expected.append(" WHERE `id`=?");

		String actual = metaDataWithoutDeactivatedFlag.genUpdateSql(columns);

		assertEquals(expected.toString(), actual);
	}

	public void testGenDeleteSqlWithDeactivatedFlag() {
		String expected = "UPDATE test_table SET `deactivated`=1 WHERE `deactivated`=0 AND `id`=?";
		String actual = metaDataWithDeactivatedFlag.genDeleteSql();

		assertEquals(expected, actual);
	}

	public void testGenDeleteSqlWithoutDeactivatedFlag() {
		String expected = "DELETE FROM test_table WHERE `id`=?";
		String actual = metaDataWithoutDeactivatedFlag.genDeleteSql();

		assertEquals(expected, actual);
	}

	public void testGenSelectOneSql() {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("SELECT `id`");
		for (String col : columns) {
			expected.append(",`").append(col).append("`");
		}
		expected.append(" FROM test_table WHERE `deactivated`=0 AND `id`=?");

		String actual = metaDataWithDeactivatedFlag.genSelectSql(false, null, null);

		assertEquals(expected.toString(), actual);
	}

	public void testGenSelectAllSql() {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("SELECT `id`");
		for (String col : columns) {
			expected.append(",`").append(col).append("`");
		}
		expected.append(" FROM test_table WHERE `deactivated`=0");

		Set<FilterCondition> filter = new HashSet<FilterCondition>();
		filter.add(new FilterCondition("loginenabled", "=", "y", Types.VARCHAR));
		filter.add(new FilterCondition("empcode", ">", 100, Types.VARCHAR));

		for (FilterCondition condition : filter) {
			expected.append(" AND `").append(condition.getColumnName()).append("`").append(condition.getOperator()).append("?");
		}

		String actual = metaDataWithDeactivatedFlag.genSelectSql(true, filter, null);

		assertEquals(expected.toString(), actual);
	}

	public void testGenSelectOrderedSql() {
		Set<String> columns = columnDef.keySet();

		StringBuilder expected = new StringBuilder("SELECT `id`");
		for (String col : columns) {
			expected.append(",`").append(col).append("`");
		}
		expected.append(" FROM test_table WHERE `deactivated`=0");

		Set<FilterCondition> filter = new HashSet<FilterCondition>();
		filter.add(new FilterCondition("loginenabled", "=", "y", Types.VARCHAR));
		filter.add(new FilterCondition("empcode", ">", 100, Types.VARCHAR));

		for (FilterCondition condition : filter) {
			expected.append(" AND `").append(condition.getColumnName()).append("`").append(condition.getOperator()).append("?");
		}
		expected.append(" ORDER BY `loginname`,`empcode`");

		List<String> sortFields = new ArrayList<String>();
		sortFields.add("`loginname`");
		sortFields.add("`empcode`");

		String actual = metaDataWithDeactivatedFlag.genSelectSql(true, filter, sortFields);

		assertEquals(expected.toString(), actual);
	}

}
