package com.bhk.unic;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;

public class DBManager {
    private String dbName = ":memory:";
    SQLiteDatabase db = null;
    boolean bIsOpened = false;
    public boolean opendb()
    {
        if (bIsOpened)
        {
            return true;
        }
        db = SQLiteDatabase.openOrCreateDatabase(dbName, null);
        if (null == db)
        {
            return false;
        }
        String strSQL = "create table if not exists plamdata(id text not null,feature text not null)";
        db.execSQL(strSQL);
        bIsOpened = true;
        return true;
    }

    public boolean isPalmExited(String id)
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return false;
        }
        Cursor cursor = db.query("plamdata", null, "id=?", new String[] { id }, null, null, null);
        return cursor.getCount() > 0;
    }

    public boolean deleteUser(String pin)
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return false;
        }
        db.delete("plamdata", "id=?", new String[] { pin });
        return true;
    }


    public boolean clear()
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return false;
        }
        String strSQL = "delete from plamdata;";
        db.execSQL(strSQL);
        return true;
    }

    public boolean modifyUser(String id, String feature)
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return false;
        }
        ContentValues value = new ContentValues();
        value.put("feature", feature);
        db.update("plamdata", value, "id=?", new String[] { id });
        return true;
    }

    public int getCount()
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return 0;
        }
        Cursor cursor = db.query("plamdata", null, null, null, null, null, null);
        return cursor.getCount();
    }

    public boolean insertUser(String id, String feature)
    {
        if (!bIsOpened)
        {
            opendb();
        }
        if (null == db)
        {
            return false;
        }
        ContentValues value = new ContentValues();
        value.put("id", id);
        value.put("feature", feature);
        db.insert("plamdata", null, value);
        return true;
    }

    public HashMap<String, String> queryUserList()
    {
        if (!bIsOpened)
        {
            return null;
        }
        if (null == db)
        {
            return null;
        }
        Cursor cursor = db.query("plamdata", null, null, null, null, null, null);
        if (cursor.getCount() == 0)
        {
            cursor.close();
            return null;
        }
        HashMap<String, String> map = new HashMap<String, String>();
        for (cursor.moveToFirst();!cursor.isAfterLast();cursor.moveToNext()) {
           map.put(cursor.getString(cursor.getColumnIndex("id")), cursor.getString(cursor.getColumnIndex("feature")));
        }
        cursor.close();
        return map;
    }

}
