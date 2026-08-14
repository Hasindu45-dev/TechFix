package com.techfix.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.techfix.app.models.Appointment;
import com.techfix.app.models.Branch;
import com.techfix.app.models.RequiredPart;
import com.techfix.app.models.Service;
import com.techfix.app.models.SparePart;
import com.techfix.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "techfix_offline.db";
    private static final int DATABASE_VERSION = 2;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_BRANCHES = "branches";
    private static final String TABLE_SERVICES = "services";
    private static final String TABLE_APPOINTMENTS = "appointments";
    private static final String TABLE_REPAIR_HISTORY = "repair_history";
    private static final String TABLE_SPARE_PARTS = "spare_parts";

    // Common column name
    private static final String KEY_ID = "id";

    // USERS Table columns
    private static final String COL_USER_NAME = "name";
    private static final String COL_USER_EMAIL = "email";
    private static final String COL_USER_PHONE = "phone";
    private static final String COL_USER_ROLE = "role";

    // BRANCHES Table columns
    private static final String COL_BRANCH_NAME = "name";
    private static final String COL_BRANCH_ADDRESS = "address";
    private static final String COL_BRANCH_LAT = "latitude";
    private static final String COL_BRANCH_LNG = "longitude";

    // SERVICES Table columns
    private static final String COL_SERVICE_NAME = "name";
    private static final String COL_SERVICE_CATEGORY = "category";
    private static final String COL_SERVICE_DESC = "description";
    private static final String COL_SERVICE_PRICE = "price";
    private static final String COL_SERVICE_DURATION = "duration";
    private static final String COL_SERVICE_IMAGE = "image_url";
    private static final String COL_SERVICE_REQUIRED_PARTS = "required_parts";

    // SPARE PARTS Table columns
    private static final String COL_PART_NAME = "name";
    private static final String COL_PART_DESC = "description";
    private static final String COL_PART_CATEGORY = "category";
    private static final String COL_PART_PRICE = "price";
    private static final String COL_PART_QTY = "quantity";
    private static final String COL_PART_BRANCH_ID = "branch_id";
    private static final String COL_PART_MIN_STOCK = "minimum_stock_level";

    // APPOINTMENTS Table columns
    private static final String COL_APPT_CUSTOMER_ID = "customer_id";
    private static final String COL_APPT_SERVICE_ID = "service_id";
    private static final String COL_APPT_DEVICE = "device_model";
    private static final String COL_APPT_PROBLEM = "problem_description";
    private static final String COL_APPT_IMAGE = "image_url";
    private static final String COL_APPT_BRANCH = "assigned_branch";
    private static final String COL_APPT_TECH = "assigned_technician";
    private static final String COL_APPT_STATUS = "status";
    private static final String COL_APPT_DATE = "date";

    // REPAIR HISTORY Table columns
    private static final String COL_HIST_APPT_ID = "appointment_id";
    private static final String COL_HIST_CUST_ID = "customer_id";
    private static final String COL_HIST_DEVICE = "device_model";
    private static final String COL_HIST_SERVICE = "service_name";
    private static final String COL_HIST_BRANCH = "branch_name";
    private static final String COL_HIST_DATE = "date";
    private static final String COL_HIST_COST = "cost";
    private static final String COL_HIST_PAY_STATUS = "payment_status";
    private static final String COL_HIST_REP_STATUS = "repair_status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_USER_NAME + " TEXT,"
                + COL_USER_EMAIL + " TEXT,"
                + COL_USER_PHONE + " TEXT,"
                + COL_USER_ROLE + " TEXT" + ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Create Branches Table
        String CREATE_BRANCHES_TABLE = "CREATE TABLE " + TABLE_BRANCHES + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_BRANCH_NAME + " TEXT,"
                + COL_BRANCH_ADDRESS + " TEXT,"
                + COL_BRANCH_LAT + " REAL,"
                + COL_BRANCH_LNG + " REAL" + ")";
        db.execSQL(CREATE_BRANCHES_TABLE);

        // Create Services Table
        String CREATE_SERVICES_TABLE = "CREATE TABLE " + TABLE_SERVICES + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_SERVICE_NAME + " TEXT,"
                + COL_SERVICE_CATEGORY + " TEXT,"
                + COL_SERVICE_DESC + " TEXT,"
                + COL_SERVICE_PRICE + " REAL,"
                + COL_SERVICE_DURATION + " TEXT,"
                + COL_SERVICE_IMAGE + " TEXT,"
                + COL_SERVICE_REQUIRED_PARTS + " TEXT" + ")";
        db.execSQL(CREATE_SERVICES_TABLE);

        // Create Spare Parts Table
        String CREATE_SPARE_PARTS_TABLE = "CREATE TABLE " + TABLE_SPARE_PARTS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_PART_NAME + " TEXT,"
                + COL_PART_DESC + " TEXT,"
                + COL_PART_CATEGORY + " TEXT,"
                + COL_PART_PRICE + " REAL,"
                + COL_PART_QTY + " INTEGER,"
                + COL_PART_BRANCH_ID + " TEXT,"
                + COL_PART_MIN_STOCK + " INTEGER" + ")";
        db.execSQL(CREATE_SPARE_PARTS_TABLE);

        // Create Appointments Table
        String CREATE_APPOINTMENTS_TABLE = "CREATE TABLE " + TABLE_APPOINTMENTS + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_APPT_CUSTOMER_ID + " TEXT,"
                + COL_APPT_SERVICE_ID + " TEXT,"
                + COL_APPT_DEVICE + " TEXT,"
                + COL_APPT_PROBLEM + " TEXT,"
                + COL_APPT_IMAGE + " TEXT,"
                + COL_APPT_BRANCH + " TEXT,"
                + COL_APPT_TECH + " TEXT,"
                + COL_APPT_STATUS + " TEXT,"
                + COL_APPT_DATE + " TEXT" + ")";
        db.execSQL(CREATE_APPOINTMENTS_TABLE);

        // Create Repair History Table
        String CREATE_REPAIR_HISTORY_TABLE = "CREATE TABLE " + TABLE_REPAIR_HISTORY + "("
                + KEY_ID + " TEXT PRIMARY KEY,"
                + COL_HIST_APPT_ID + " TEXT,"
                + COL_HIST_CUST_ID + " TEXT,"
                + COL_HIST_DEVICE + " TEXT,"
                + COL_HIST_SERVICE + " TEXT,"
                + COL_HIST_BRANCH + " TEXT,"
                + COL_HIST_DATE + " TEXT,"
                + COL_HIST_COST + " REAL,"
                + COL_HIST_PAY_STATUS + " TEXT,"
                + COL_HIST_REP_STATUS + " TEXT" + ")";
        db.execSQL(CREATE_REPAIR_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BRANCHES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_APPOINTMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPAIR_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SPARE_PARTS);
        onCreate(db);
    }

    // --- USERS CRUD ---

    public void insertOrUpdateUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, user.getUserId());
        values.put(COL_USER_NAME, user.getName());
        values.put(COL_USER_EMAIL, user.getEmail());
        values.put(COL_USER_PHONE, user.getPhone());
        values.put(COL_USER_ROLE, user.getRole());

        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public User getUser(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;
        String selectQuery = "SELECT * FROM " + TABLE_USERS + " WHERE " + KEY_ID + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{userId})) {
            if (cursor != null && cursor.moveToFirst()) {
                user = new User(
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PHONE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_ROLE))
                );
            }
        }
        return user;
    }

    // --- BRANCHES CRUD ---

    public void insertOrUpdateBranch(Branch branch) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, branch.getBranchId());
        values.put(COL_BRANCH_NAME, branch.getName());
        values.put(COL_BRANCH_ADDRESS, branch.getAddress());
        values.put(COL_BRANCH_LAT, branch.getLatitude());
        values.put(COL_BRANCH_LNG, branch.getLongitude());

        db.insertWithOnConflict(TABLE_BRANCHES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Branch> getAllBranches() {
        List<Branch> branchList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_BRANCHES;
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    Branch branch = new Branch(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_BRANCH_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_BRANCH_ADDRESS)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BRANCH_LAT)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BRANCH_LNG))
                    );
                    branchList.add(branch);
                } while (cursor.moveToNext());
            }
        }
        return branchList;
    }

    // --- SERVICES CRUD ---

    public void clearServicesTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SERVICES, null, null);
    }

    public void insertOrUpdateService(Service service) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, service.getServiceId());
        values.put(COL_SERVICE_NAME, service.getName());
        values.put(COL_SERVICE_CATEGORY, service.getCategory());
        values.put(COL_SERVICE_DESC, service.getDescription());
        values.put(COL_SERVICE_PRICE, service.getPrice());
        values.put(COL_SERVICE_DURATION, service.getDuration());
        values.put(COL_SERVICE_IMAGE, service.getImageURL());
        values.put(COL_SERVICE_REQUIRED_PARTS, requiredPartsToJson(service.getRequiredParts()));

        db.insertWithOnConflict(TABLE_SERVICES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Service> getAllServices() {
        List<Service> serviceList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_SERVICES;
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    Service service = new Service(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_CATEGORY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DESC)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SERVICE_PRICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DURATION)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_IMAGE))
                    );
                    String partsJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_REQUIRED_PARTS));
                    service.setRequiredParts(jsonToRequiredParts(partsJson));
                    serviceList.add(service);
                } while (cursor.moveToNext());
            }
        }
        return serviceList;
    }

    // --- JSON SERIALIZERS FOR REQUIRED PARTS ---

    public static String requiredPartsToJson(List<RequiredPart> list) {
        if (list == null) return "[]";
        org.json.JSONArray array = new org.json.JSONArray();
        for (RequiredPart rp : list) {
            try {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("partName", rp.getPartName());
                obj.put("quantity", rp.getQuantity());
                array.put(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return array.toString();
    }

    public static List<RequiredPart> jsonToRequiredParts(String jsonStr) {
        List<RequiredPart> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.isEmpty()) return list;
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                list.add(new RequiredPart(obj.getString("partName"), obj.getInt("quantity")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    // --- SPARE PARTS CRUD ---

    public void clearSparePartsTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SPARE_PARTS, null, null);
    }

    public void insertOrUpdateSparePart(SparePart part) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, part.getPartId());
        values.put(COL_PART_NAME, part.getName());
        values.put(COL_PART_DESC, part.getDescription());
        values.put(COL_PART_CATEGORY, part.getCategory());
        values.put(COL_PART_PRICE, part.getPrice());
        values.put(COL_PART_QTY, part.getQuantity());
        values.put(COL_PART_BRANCH_ID, part.getBranchId());
        values.put(COL_PART_MIN_STOCK, part.getMinimumStockLevel());

        db.insertWithOnConflict(TABLE_SPARE_PARTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<SparePart> getAllSpareParts() {
        List<SparePart> partList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_SPARE_PARTS;
        try (Cursor cursor = db.rawQuery(selectQuery, null)) {
            if (cursor.moveToFirst()) {
                do {
                    SparePart part = new SparePart(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_PART_QTY)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PART_PRICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_BRANCH_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_DESC)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_CATEGORY)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_PART_MIN_STOCK)),
                            "", // imageURL optional for offline cache
                            0, // createdAt
                            0  // updatedAt
                    );
                    partList.add(part);
                } while (cursor.moveToNext());
            }
        }
        return partList;
    }

    public List<SparePart> getSparePartsForBranch(String branchId) {
        List<SparePart> partList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_SPARE_PARTS + " WHERE " + COL_PART_BRANCH_ID + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{branchId})) {
            if (cursor.moveToFirst()) {
                do {
                    SparePart part = new SparePart(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_NAME)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_PART_QTY)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PART_PRICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_BRANCH_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_DESC)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_PART_CATEGORY)),
                            cursor.getInt(cursor.getColumnIndexOrThrow(COL_PART_MIN_STOCK)),
                            "", // imageURL
                            0,
                            0
                    );
                    partList.add(part);
                } while (cursor.moveToNext());
            }
        }
        return partList;
    }

    public List<Service> searchServices(String query) {
        List<Service> serviceList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Match service name or category using SQL LIKE
        String selectQuery = "SELECT * FROM " + TABLE_SERVICES + " WHERE " 
                + COL_SERVICE_NAME + " LIKE ? OR " 
                + COL_SERVICE_CATEGORY + " LIKE ?";
        String wildcardQuery = "%" + query + "%";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{wildcardQuery, wildcardQuery})) {
            if (cursor.moveToFirst()) {
                do {
                    Service service = new Service(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_NAME)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_CATEGORY)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DESC)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SERVICE_PRICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_DURATION)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_SERVICE_IMAGE))
                    );
                    serviceList.add(service);
                } while (cursor.moveToNext());
            }
        }
        return serviceList;
    }

    // --- APPOINTMENTS CRUD ---

    public void insertOrUpdateAppointment(Appointment appt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, appt.getAppointmentId());
        values.put(COL_APPT_CUSTOMER_ID, appt.getCustomerId());
        values.put(COL_APPT_SERVICE_ID, appt.getServiceId());
        values.put(COL_APPT_DEVICE, appt.getDeviceModel());
        values.put(COL_APPT_PROBLEM, appt.getProblemDescription());
        values.put(COL_APPT_IMAGE, appt.getImageURL());
        values.put(COL_APPT_BRANCH, appt.getAssignedBranch());
        values.put(COL_APPT_TECH, appt.getAssignedTechnician());
        values.put(COL_APPT_STATUS, appt.getStatus());
        values.put(COL_APPT_DATE, appt.getDate());

        db.insertWithOnConflict(TABLE_APPOINTMENTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Appointment> getAppointmentsForCustomer(String customerId) {
        List<Appointment> apptList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_APPOINTMENTS + " WHERE " + COL_APPT_CUSTOMER_ID + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{customerId})) {
            if (cursor.moveToFirst()) {
                do {
                    Appointment appt = new Appointment(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_CUSTOMER_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_SERVICE_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_DEVICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_PROBLEM)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_IMAGE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_BRANCH)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_TECH)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_APPT_DATE))
                    );
                    apptList.add(appt);
                } while (cursor.moveToNext());
            }
        }
        return apptList;
    }

    // --- REPAIR HISTORY CRUD ---

    public void insertOrUpdateHistory(String id, String appointmentId, String customerId, String deviceModel, 
                                      String serviceName, String branchName, String date, double cost, 
                                      String paymentStatus, String repairStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, id);
        values.put(COL_HIST_APPT_ID, appointmentId);
        values.put(COL_HIST_CUST_ID, customerId);
        values.put(COL_HIST_DEVICE, deviceModel);
        values.put(COL_HIST_SERVICE, serviceName);
        values.put(COL_HIST_BRANCH, branchName);
        values.put(COL_HIST_DATE, date);
        values.put(COL_HIST_COST, cost);
        values.put(COL_HIST_PAY_STATUS, paymentStatus);
        values.put(COL_HIST_REP_STATUS, repairStatus);

        db.insertWithOnConflict(TABLE_REPAIR_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static class HistoryRecord {
        public String id;
        public String appointmentId;
        public String customerId;
        public String deviceModel;
        public String serviceName;
        public String branchName;
        public String technicianName;
        public String date;
        public double cost;
        public String paymentStatus;
        public String repairStatus;

        public HistoryRecord(String id, String appointmentId, String customerId, String deviceModel, 
                             String serviceName, String branchName, String date, double cost, 
                             String paymentStatus, String repairStatus) {
            this(id, appointmentId, customerId, deviceModel, serviceName, branchName, "Pending Allocation", date, cost, paymentStatus, repairStatus);
        }

        public HistoryRecord(String id, String appointmentId, String customerId, String deviceModel, 
                             String serviceName, String branchName, String technicianName, String date, double cost, 
                             String paymentStatus, String repairStatus) {
            this.id = id;
            this.appointmentId = appointmentId;
            this.customerId = customerId;
            this.deviceModel = deviceModel;
            this.serviceName = serviceName;
            this.branchName = branchName;
            this.technicianName = technicianName;
            this.date = date;
            this.cost = cost;
            this.paymentStatus = paymentStatus;
            this.repairStatus = repairStatus;
        }
    }

    public List<HistoryRecord> getHistoryForCustomer(String customerId) {
        List<HistoryRecord> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_REPAIR_HISTORY + " WHERE " + COL_HIST_CUST_ID + " = ?";
        try (Cursor cursor = db.rawQuery(selectQuery, new String[]{customerId})) {
            if (cursor.moveToFirst()) {
                do {
                    HistoryRecord record = new HistoryRecord(
                            cursor.getString(cursor.getColumnIndexOrThrow(KEY_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_APPT_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_CUST_ID)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_DEVICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_SERVICE)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_BRANCH)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_DATE)),
                            cursor.getDouble(cursor.getColumnIndexOrThrow(COL_HIST_COST)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_PAY_STATUS)),
                            cursor.getString(cursor.getColumnIndexOrThrow(COL_HIST_REP_STATUS))
                    );
                    historyList.add(record);
                } while (cursor.moveToNext());
            }
        }
        return historyList;
    }
}
