package com.example.potholedetectionapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "pothole_data.db";
    public static final String TABLE_NAME="pothole_table";

    public static final String COL_1="ID";
    public static final String COL_2="TIME";
    public static final String COL_3="LONGITUDE";
    public static final String COL_4="LATITUDE";
    public static final String COL_5="POTHOLE";

    //call constructor to create DATABASE
    public DatabaseHelper(@Nullable Context context) {
        //super(context, name, factory, version);
        super(context, DATABASE_NAME, null, 1);
    }

    //use onCreate to create a table
    @Override
    public void onCreate(SQLiteDatabase db) {
        //executes whatever (String) query inside the brackets
        db.execSQL("CREATE TABLE "+ TABLE_NAME+ "(ID INTEGER PRIMARY KEY, TIME LONG, LONGITUDE DOUBLE, LATITUDE DOUBLE, POTHOLE TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+ TABLE_NAME);
        onCreate(db);
    }

    public boolean insertData(String id, String time, String longitude, String latitude, String pothole){
        //creates database and table
        SQLiteDatabase db= this.getWritableDatabase();
        ContentValues contentValues = new ContentValues() ;
        contentValues.put(COL_1,id);
        contentValues.put(COL_2,time);
        contentValues.put(COL_3,longitude);
        contentValues.put(COL_4,latitude);
        contentValues.put(COL_5,pothole);
        //result is -1 or return value
        long result = db.insert(TABLE_NAME,null, contentValues);
        if(result==-1){
            return false;
        }else{
            return true;
        }


    }

    //query to get all the data
    public Cursor getAllData(){
        SQLiteDatabase db= this.getWritableDatabase();
        //result stored in cursor instance and access to the data
        //public Cursor rawQuery (String sql, String[] selectionArgs)
        //Cursor res = db.rawQuery("select * from "+TABLE_NAME,null);
        //public Cursor query (String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy)
        Cursor res = db.query(TABLE_NAME,new String[]{COL_1,COL_2,COL_3,COL_4,COL_5},null,null,null,null,COL_1+" ASC");
        return res;
    }

    public boolean updateData(String id, String time, String longitude, String latitude, String pothole){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_1, id);
        contentValues.put(COL_2, time);
        contentValues.put(COL_3, longitude);
        contentValues.put(COL_4, latitude);
        contentValues.put(COL_5, pothole);
        db.update(TABLE_NAME,contentValues,"ID = ?", new String[] {id});
        return true;
    }

    public Integer deleteData (String id){
        SQLiteDatabase db = this.getWritableDatabase();
        //ID=? shows which column to delete
        return db.delete(TABLE_NAME, "ID = ?", new String[]{id});
    }

    public Integer deleteAll()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "1",null);
//        db.execSQL("delete from "+ TABLE_NAME);
//        db.execSQL("TRUNCATE table" + TABLE_NAME);
//        db.close();
    }

}
