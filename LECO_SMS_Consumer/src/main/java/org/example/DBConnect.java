/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



package org.example;
import java.sql.Connection;

/**
 *
 * @author P-465
 */
public class DBConnect {

    public static DBConnect_DB  connect_DB = new DBConnect_DB ();
    //************************Public setConnection method******************************

    public static boolean setConnection(String id, String pwd ) {
        boolean conFound = false;
        try {
            conFound = connect_DB.setConnection(id, pwd);
            System.out.println("conFound  :" + conFound);
            if (conFound) {
                //userDetails.GET(id, pwd);
            }
        }//End Try
        catch (Exception e) {
            e.printStackTrace();
            //  strMessage = "ClassNotFoundException: " + e.getMessage();
        }//End Catch
        return conFound;
    }

    public Connection getConnection() {
//        System.out.println("connect_DB" + connect_DB);
        return connect_DB.getConnection();
    }
}
