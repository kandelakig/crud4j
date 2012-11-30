package com.lemondo.commons.db;

import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.lemondo.commons.db.meta.ProcMetaData;
import com.lemondo.commons.db.meta.TableMetaData;
import com.lemondo.commons.db.processor.DataProcessor;

public class ApiModel<T, L> implements Model<T, L> {

	private final Helper helper;

	private final DataProcessor<T, L> processor;

	private final TableModel<T, L> tableModel;

	private Procedure insertApi;
	private Procedure updateApi;
	private Procedure deleteApi;
	private Procedure readApi;
	private Procedure listApi;

	public ApiModel(Helper helper, DataProcessor<T, L> processor) {
		this.helper = helper;
		this.processor = processor;
		this.tableModel = null;
	}

	public ApiModel(Helper helper, DataProcessor<T, L> processor, TableMetaData meta) {
		this.helper = helper;
		this.processor = processor;
		this.tableModel = new TableModel<T, L>(meta, helper, processor);
	}

	public void setInsertApi(ProcMetaData insertMetaData) {
		this.insertApi = new Procedure(insertMetaData, helper);
	}

	public void setUpdateApi(ProcMetaData updateMetaData) {
		this.updateApi = new Procedure(updateMetaData, helper);
	}

	public void setDeleteApi(ProcMetaData deleteMetaData) {
		this.deleteApi = new Procedure(deleteMetaData, helper);
	}

	public void setReadApi(ProcMetaData readMetaData) {
		this.readApi = new Procedure(readMetaData, helper);
	}

	public void setListApi(ProcMetaData listMetaData) {
		this.listApi = new Procedure(listMetaData, helper);
	}

	@Override
	public void create(String key, T body) {
		if (insertApi != null) {
			try {
				Map<String, Object> args = processor.bodyAsMap(body);
				args.put("key", key);
				insertApi.executeCall(args);
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			tableModel.create(key, body);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}

	@Override
	public String create(T Body) {
		// TODO Implement it
		return null;
	}

	@Override
	public int update(String key, T body) {
		if (updateApi != null) {
			try {
				Map<String, Object> args = processor.bodyAsMap(body);
				args.put("key", key);
				return updateApi.executeCall(args);
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			return tableModel.update(key, body);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}

	@Override
	public int delete(String key) {
		if (deleteApi != null) {
			try {
				Map<String, Object> args = new HashMap<String, Object>();
				args.put("key", key);
				return deleteApi.executeCall(args);
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			return tableModel.delete(key);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}

	@Override
	public T read(String key) {
		if (readApi != null) {
			try {
				Map<String, Object> args = new HashMap<String, Object>();
				args.put("key", key);
				ResultSet rs = readApi.executeQuery(args);
				if (rs.next()) {
					ResultSetMetaData rsmd = rs.getMetaData();
					return processor.readRow(rs, rsmd, rsmd.getColumnCount());
				} else {
					throw new RuntimeException("BOOM: No data found");
				}
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			return tableModel.read(key);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}

	@Override
	public L list(Map<String, Object> options) {
		if (listApi != null) {
			try {
				ResultSet rs = listApi.executeQuery(options);
				ResultSetMetaData rsmd = rs.getMetaData();
				int numColumns = rsmd.getColumnCount();

				return processor.readAll(rs, rsmd, numColumns);
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			return tableModel.list(options);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}

	@Override
	public void list(OutputStream out, Map<String, Object> options) {
		if (listApi != null) {
			try {
				ResultSet rs = listApi.executeQuery(options);
				ResultSetMetaData rsmd = rs.getMetaData();
				int numColumns = rsmd.getColumnCount();

				processor.writeRows(out, rs, rsmd, numColumns);
			} catch (SQLException e) {
				throw new RuntimeException("BOOM!", e);
			}
		} else if (tableModel != null) {
			tableModel.list(options);
		} else {
			throw new RuntimeException("BOOM!");
		}
	}
}
