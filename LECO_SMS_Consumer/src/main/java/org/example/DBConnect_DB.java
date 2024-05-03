/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//import general.branchForRoamingUser;
package org.example;

import oracle.jdbc.OracleTypes;

import javax.swing.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author P-465
 */
public class DBConnect_DB {

    String newSchema = "";
    /**
     * User Id
     */
    private String strUsername = "DEVADMIN";
    /**
     * Password
     */
    private String strPassword = "DEV";
    public static final String SELECTEDSCEMA = "UBSNU";
    public static final String strInputIP = "10.20.0.32";
    public static final String strInputSID = "LECOUAT";
    /**
     * USER
     */
    // private LogedUser thisUser = null;
    // V1.5
    /**
     * Oracle User Id
     */
    private String strOracleUsername = "";
    /**
     * URL giving the location of the database
     */

    private static final String DATABASE_URL = "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS_LIST=(ADDRESS=(PROTOCOL=TCP)(HOST="
            + strInputIP + ")(PORT=1521)))(CONNECT_DATA=(SID=" + strInputSID + ")(SERVER=DEDICATED))) ";
    /**
     * Connection to the database
     */
    private java.sql.Connection LECOConnection = null;
    /**
     * Database Driver name
     */
    private static final String DATABASE_DRIVER = "oracle.jdbc.driver.OracleDriver";
    /**
     * Tag to identify Billing System Users
     */
    private static final String BILLING_USER_NAME_TAG = "UB_";
    private Date sysDate;
    private Date appDate;

    public synchronized boolean setConnection(String strUser, String strPwd) throws Exception {

        boolean gotConnected = false;
        // ProgressBarBill PB = new ProgressBarBill();
        try {
            String strDBUrl = DATABASE_URL;
            strUsername = "DEVADMIN";
            strPassword = "DEV";
            // if (user != null) {
            // thisUser = user;
            // }
            // if (strUser.equals("")) {
            // strUsername = "UBSNU";
            // }
            // if (strPwd.equals("")) {
            // strPassword = "UBSNU";
            // }

            // TK1 If the user is the schema owner don't add 'BL_'
            strOracleUsername = BILLING_USER_NAME_TAG + strUsername;

            Class.forName(DATABASE_DRIVER);
            // UPODATED AS UBSNU
            // System.out.println(strDBUrl + "|" + this.strOracleUsername + "|" +
            // this.strPassword);
            System.out.println(strDBUrl + "|" + strUsername + "|" + strPassword);
            // TK V1.5 ObtainedConnection = DriverManager.getConnection(strDBUrl,
            // this.strUsername, this.strPassword);
            LECOConnection = DriverManager.getConnection(strDBUrl, strUsername, strPassword);
            if ((this.LECOConnection == null) || (this.LECOConnection.isClosed())) {
                gotConnected = false;
            } else {
                gotConnected = true;
            }
            if (gotConnected) {

                setCurrentSchema("ESMS");// strBranchName);

            }

            DatabaseMetaData meta = LECOConnection.getMetaData();

        } catch (Exception instantiationError) {
            // instantiationError.printStackTrace();
            JOptionPane.showMessageDialog(null, instantiationError.toString(), "Connection", JOptionPane.ERROR_MESSAGE);
            LECOConnection = null;
        }
        return gotConnected;
    }

    public java.sql.Connection getConnection() {

        java.sql.Connection con = null;
        try {
            if (this.LECOConnection == null) {
                con = reConnect();
            } else {
                con = this.LECOConnection;
            }

            if (this.LECOConnection.isClosed()) {
                con = reConnect();
            } else {
                con = this.LECOConnection;
            }

        } catch (Exception getConnectionEx) {
            System.err.println("  getConnectionEx :   " + getConnectionEx);
        }

        // con = this.ObtainedConnection ;
        // to check users login date with system date SP27072012

        return con;
    }

    private java.sql.Connection reConnect() {
        try {

            System.out.println("reConnect....");
            setConnection(this.strUsername, this.strPassword);
        } catch (Exception ReConnectionExceptin) {
            JOptionPane.showMessageDialog(null, "Could not obtain a connection to the database", "Database Connection",
                    JOptionPane.ERROR_MESSAGE);
        }
        return this.LECOConnection;
    }

    public int setCurrentSchema(String schemaName) {
        int schemaChanged = -1;
        java.sql.Connection con = null;
        // schemaName = "NNUGEGODA";
        System.out.println(" setCurrentSchema --- > " + schemaName);

        try {

            String changeSchemaSql = "ALTER SESSION SET CURRENT_SCHEMA=" + schemaName;
            if (this.LECOConnection == null) {
                con = reConnect();
            } else {
                con = this.LECOConnection;
            }

            PreparedStatement stmt = this.LECOConnection.prepareStatement(changeSchemaSql);

            schemaChanged = stmt.executeUpdate();

            stmt.close();

            // System.out.println("setCurrentSchema" + this.getApplicationSchema());
        } catch (Exception COM_DatabaseConnectionUTsetCurrentSchemaException) {
            COM_DatabaseConnectionUTsetCurrentSchemaException.printStackTrace();
        }

        return schemaChanged;
    }

    public String getSystemDateTime(java.sql.Connection dbConnection) {
        String dateSysDate = null;
        try {
            String strSQL = "Select SYSDATE,to_char(SYSDATE, ' dd/mm/yyyy')  as SYSDATE2,to_char(SYSDATE, ' HH24:MI:SSPM') as TIME from DUAL";

            PreparedStatement retData = dbConnection.prepareStatement(strSQL);
            ResultSet rs = retData.executeQuery();

            rs.next();
            dateSysDate = rs.getString("SYSDATE2");
            sysDate = rs.getDate("SYSDATE");
            appDate = rs.getDate("SYSDATE");

            rs.close();
            retData.close();

        } catch (Exception COM_DatabaseConnectionUTgetSystemDateTimeException) {
            COM_DatabaseConnectionUTgetSystemDateTimeException.printStackTrace();
        }

        return dateSysDate;
    }

    public String getDayEndOk(String brName, java.sql.Connection dbConnection) {
        String currentBCCode = null;
        try {

            CallableStatement call = dbConnection.prepareCall("{ CALL pkg_util.isDayEndOK(?,?) }");

            call.setString(1, brName);
            call.registerOutParameter(2, OracleTypes.VARCHAR);
            call.execute();

            String rs = call.getObject(2).toString().trim();
            currentBCCode = rs;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentBCCode;
    }

    public String getCurrentVersion(String brName, java.sql.Connection dbConnection) {
        String currentBCCode = null;
        try {

            CallableStatement call = dbConnection.prepareCall("{ CALL pkg_util.getCurrentVersin(?,?) }");

            call.setString(1, brName);
            call.registerOutParameter(2, OracleTypes.VARCHAR);
            call.execute();

            String rs = call.getObject(2).toString().trim();
            currentBCCode = rs;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return currentBCCode;
    }

    public static String getUserDetailsS(String username, String password, java.sql.Connection dbConnection) {
        try {
            // SPCaller caller = new SPCaller("pkg_user.sel_user_details", new
            // String[]{"puser_name", "pout_msg", "pout_cur"});
            CallableStatement call = dbConnection
                    .prepareCall("{ CALL " + "FLAPP" + ".pkg_user.sel_user_details(?, ?, ?) }");

            call.setString(1, username);
            call.registerOutParameter(2, OracleTypes.VARCHAR);
            call.registerOutParameter(3, OracleTypes.CURSOR);
            call.execute();

            ResultSet rs;
            rs = (ResultSet) call.getObject(3);

            while (rs.next()) {

                // System.out.println(rs.getString("USER_ID").toString()+"++++"+UserLogin.userDetails.getUSER_ID()+"|");
            }
            System.out.println(call.getObject(2).toString());
            String msg = call.getObject(2).toString();
            // UserLogin.lECOMessage.callLECOMessageBox(new Window(null), "W", "test22");
            rs.close();
            return msg;

        } catch (SQLException ex) {
            Logger.getLogger(DBConnect_DB.class
                    .getName()).log(Level.SEVERE, null, ex);
            return "UBSNU";
        }
    }

    public String setSchema(String addedSchema, String newBranchCode) {

        newSchema = addedSchema;
        // int y = setCurrentSchema(newSchema);

        System.out.println("--newly added schema ====" + newBranchCode);

        // if (y == 0) {
        try {// change user selected branch as his branch for log to the system
            CallableStatement call = LECOConnection.prepareCall("{ CALL NLECO.pkg_user.changeSchema(?,?) }");
            call.setString(1, "UBSNU");
            call.setString(2, newBranchCode);
            call.execute();

            // getUserDetails(this.strUsername, this.strPassword, LECOConnection);
            setCurrentSchema("UBSNU");
            return "UBSNU";

        } catch (SQLException ex) {
            Logger.getLogger(DBConnect_DB.class
                    .getName()).log(Level.SEVERE, null, ex);

            return "";
        }

    }
}
