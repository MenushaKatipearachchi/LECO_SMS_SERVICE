package org.example;

import oracle.jdbc.OracleTypes;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class all_bd {
    List getSMS_Data( ) {
        List a=null;
        SPCaller caller5 = new SPCaller("UBSNU.PKG_SMSRBT.GET_SMS_data", new String[]{"NUM", "SMS_DATA"});
        caller5.setParam("NUM", new SPParam(SPParam.Direction.IN, OracleTypes.VARCHAR, 1));
        caller5.setParam("SMS_DATA", new SPParam(SPParam.Direction.OUT, OracleTypes.CURSOR));
        try {
            caller5.call();
            a = caller5.getParam("SMS_DATA").getValueAsList();
        } catch (SQLException ex) {
            Logger.getLogger(all_bd.class.getName()).log(Level.SEVERE, null, ex);
        }
        return a;

    }
}
