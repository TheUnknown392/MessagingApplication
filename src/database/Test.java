/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database;

import java.sql.Connection;

/**
 *
 * @author theunknown
 */
public class Test {
    public static void main(String[] args) {
        GetConnectionDB con = new GetConnectionDB("localhost", "3306", "test", "user", "1234", true);
        Connection connect = con.getConnection();
    }
}
