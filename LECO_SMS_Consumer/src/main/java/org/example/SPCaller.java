package org.example;


import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;

import java.sql.*;
import java.util.*;

/**
 *
 * @author G-160
 */
public class SPCaller {
    protected Map params = new HashMap();
    protected String spName;
    protected String[] parameters;
    DBConnect DBCON= new DBConnect();
    Connection connection = DBCON.getConnection();

    /**
     * Creates a new caller for the given stored procedure
     *
     *
     * @param spName String
     */
    public SPCaller(String spName, String[] parameters) {
        super();
        this.spName = spName;
        this.parameters = parameters;
    }

    /**
     * Sets IN or OUT parameter
     *
     * @param name Name of the parameter
     *
     * @param param SPParam
     */
    public void setParam(String name, SPParam param) {
        params.put(name.toUpperCase(), param);
    }

    /**
     * Unset parameter
     *
     * @param name Name of the parameter to be removed
     *
     * @return the parameter
     */
    public SPParam removeParam(String name) {
        return (SPParam) params.remove(name.toUpperCase());
    }

    /**
     * Gets a parameter
     *
     * @param name Name of the parameter
     *
     * @return the parameter
     */
    public SPParam getParam(String name) {
        return (SPParam) params.get(name.toUpperCase());
    }

    /**
     * Unsets all parameters
     */
    public void clearParams() {
        params.clear();
    }

    /**
     * Method setSpName.
     *
     * @param spName String
     */
    public void setSpName(String spName) {
        this.spName = spName.toUpperCase();
    }

    /**
     * Method getSpName.
     *
     * @return String
     */
    public String getSpName() {
        return spName;
    }

    /**
     * Call Stored Procedure
     *
     * @param connection connection used to call SP
     *
     * @throws SQLException
     */
    public void call(Connection connection) throws SQLException {
        CallableStatement callableStatement = connection.prepareCall(generateSQL());
        SQLException exception = null;
        try {
            setParameters(callableStatement);
//            System.out.println(spName + "  >  " + params + "  >  " + connection.getMetaData().getUserName());
            callableStatement.execute();
            getParameters(callableStatement);
        } catch (SQLException e) {
            exception = e;
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Calls stored procedure (All the Exceptions are written to the stdOut)
     *
     *
     *
     * @throws SQLException
     */
    public void call() throws SQLException {
        Connection dbConnection = connection;
        SQLException exception = null;
        try {
            call(dbConnection);
        } catch (SQLException ex) {
            exception = ex;
        } finally {
//			DBConnector.closeConnection(dbConnection);
        }
        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Generates SQL to call the procedure
     *
     * @return String
     */
    protected String generateSQL() {
        String str = new String("{CALL ").concat(spName).concat("(");
        for (int i = 0; i < params.size(); i++) {
            if (i == 0) {
                str = str.concat("?");
            } else {
                str = str.concat(",?");
            }
        }
        str = str.concat(") }");
        return str.toString();
    }

    /**
     * Binds parameters
     *
     * @param stmt CallableStatement
     * @throws SQLException
     */
    protected void setParameters(CallableStatement stmt) throws SQLException {
        for (Iterator it = params.keySet().iterator(); it.hasNext();) {
            String paramName = (String) it.next();

            SPParam param = (SPParam) params.get(paramName);
            if (param.isOut()) {
//				try {
                stmt.registerOutParameter(indexOf(paramName), param.getType());
//				} catch (SQLException e) {
//					throw e;
//				}
            }
            if (param.isIn()) {
                try {
                    Object value = param.getValue();
                    int type = param.getType();
                    //  System.out.println(value+"set "+type);
                    if (value instanceof String[] && type == Types.ARRAY) {
                        ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor(DBConnect_DB.SELECTEDSCEMA.toUpperCase()  + ".STR_ARRAY", stmt.getConnection());
                        //  stmt.setObject(indexOf(paramName), new ARRAY(descriptor, stmt.getConnection(), value), type);
                        //System.out.println(value+"set bb "+UserLogin.strBranchName );
                        stmt.setArray(indexOf(paramName), new ARRAY(descriptor, stmt.getConnection(), value)); // IP 07062015 - in order to pass String []....
                    } else if (value instanceof String[][] && type == Types.ARRAY) {
                        ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor(DBConnect_DB.SELECTEDSCEMA.toUpperCase() + ".STR_ARRAY", stmt.getConnection());
                        //stmt.setObject(indexOf(paramName), new ARRAY(descriptor, stmt.getConnection(), value), type);
                        stmt.setArray(indexOf(paramName), new ARRAY(descriptor, stmt.getConnection(), value));// IP 07062015 - in order to pass String []....
                    } else if (value instanceof java.lang.Character) {
                        stmt.setObject(indexOf(paramName), String.valueOf(value), type);
                    } else if (value instanceof java.util.Date) {
                        stmt.setObject(indexOf(paramName), new java.sql.Date(((java.util.Date) value).getTime()), type);
                    } else {
                        stmt.setObject(indexOf(paramName), value, type);
                    }
                } catch (SQLException e) {
                    System.out.println("" + DBConnect_DB.SELECTEDSCEMA.toUpperCase());
                    SQLException sQLException = new SQLException("Error setting out parameter " + paramName);
                    sQLException.setNextException(e);
                    throw sQLException;
                } catch (Exception e) {
                    throw new SQLException(e.getMessage());
                }
            }
        }
    }

    /**
     * get values from OUT parameters
     *
     * @param stmt CallableStatement
     * @throws SQLException
     */
    protected void getParameters(CallableStatement stmt) throws SQLException {
        SQLException exception = null;
        for (Iterator it = params.keySet().iterator(); it.hasNext();) {
            String paramName = (String) it.next();
            SPParam param = (SPParam) params.get(paramName);
            if (param.isOut()) {
                final Object object = stmt.getObject(indexOf(paramName));
                if (object instanceof ResultSet) {
                    List rows = new ArrayList();
                    ResultSet resultSet = (ResultSet) object;
                    try {
                        while (resultSet.next()) {
                            Map row = new HashMap();
                            final ResultSetMetaData metaData = resultSet.getMetaData();
                            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                final Object value = resultSet.getObject(i);
                                if (value != null) {
//									if (value instanceof Date) {
//										row.put(metaData.getColumnName(i).toUpperCase(), resultSet.getTimestamp(i));
//									} else {
                                    row.put(metaData.getColumnName(i).toUpperCase(), value);
//									}
                                }
                            }
                            rows.add(row);
                        }
                    } catch (SQLException ex) {
                        exception = ex;
                    } finally {
                        resultSet.close();
                    }
                    param.setValue(rows);
                } else if (object instanceof java.sql.Date) {
                    param.setValue(new java.util.Date(((java.sql.Date) object).getTime()));
                } else {
                    param.setValue(object);
                }
            }
        }
        stmt.close();
        if (exception != null) {
            throw exception;
        }
    }

    protected int indexOf(String parameter) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameter.equalsIgnoreCase(parameters[i])) {
                return i + 1;
            }
        }
        return -1;
    }
}
