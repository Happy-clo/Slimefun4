package com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon;

import com.xzavier0722.mc.plugin.slimefun4.storage.common.DataScope;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldKey;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.FieldMapper;
import com.xzavier0722.mc.plugin.slimefun4.storage.common.RecordSet;
import io.github.bakedlibs.dough.collections.Pair;

import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_ID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_NAME;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_NUM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_BACKPACK_SIZE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_ITEM;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_INVENTORY_SLOT;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_PLAYER_NAME;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_PLAYER_UUID;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.FIELD_RESEARCH_KEY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.TABLE_NAME_BACKPACK;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.TABLE_NAME_BACKPACK_INVENTORY;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.TABLE_NAME_PLAYER_PROFILE;
import static com.xzavier0722.mc.plugin.slimefun4.storage.adapter.sqlcommon.SqlConstants.TABLE_NAME_PLAYER_RESEARCH;

public class SqlUtils {

    private static final FieldMapper<String> mapper;
    static {
        var fieldMap = new HashMap<FieldKey, String>();
        fieldMap.put(FieldKey.PLAYER_UUID, FIELD_PLAYER_UUID);
        fieldMap.put(FieldKey.PLAYER_NAME, FIELD_PLAYER_NAME);
        fieldMap.put(FieldKey.RESEARCH_ID, FIELD_RESEARCH_KEY);
        fieldMap.put(FieldKey.BACKPACK_ID, FIELD_BACKPACK_ID);
        fieldMap.put(FieldKey.BACKPACK_NUMBER, FIELD_BACKPACK_NUM);
        fieldMap.put(FieldKey.BACKPACK_NAME, FIELD_BACKPACK_NAME);
        fieldMap.put(FieldKey.BACKPACK_SIZE, FIELD_BACKPACK_SIZE);
        fieldMap.put(FieldKey.INVENTORY_SLOT, FIELD_INVENTORY_SLOT);
        fieldMap.put(FieldKey.INVENTORY_ITEM, FIELD_INVENTORY_ITEM);
        mapper = new FieldMapper<>(fieldMap);
    }

    public static String mapTable(DataScope scope) {
        return mapTable(scope, "");
    }

    public static String mapTable(DataScope scope, String prefix) {
        return prefix + switch (scope) {
            case PLAYER_PROFILE -> TABLE_NAME_PLAYER_PROFILE;
            case PLAYER_RESEARCH -> TABLE_NAME_PLAYER_RESEARCH;
            case BACKPACK_PROFILE -> TABLE_NAME_BACKPACK;
            case BACKPACK_INVENTORY -> TABLE_NAME_BACKPACK_INVENTORY;
            case NONE -> throw new IllegalArgumentException("NONE cannot be a storage data scope!");
        };
    }

    public static String mapField(FieldKey key) {
        if (key == FieldKey.PLAYER_BACKPACK_NUM) {
            key = FieldKey.BACKPACK_NUMBER;
        }
        return mapper.get(key);
    }

    public static FieldKey mapField(String key) {
        return mapper.get(key);
    }

    public static Optional<String> buildFieldStr(Set<FieldKey> fields) {
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(", ", fields.stream().map(SqlUtils::mapField).toList()));
    }
    public static String buildConditionStr(List<Pair<FieldKey, String>> conditions) {
        if (conditions.isEmpty()) {
            return "";
        }

        return " WHERE " + String.join(
                " AND ",
                conditions.stream().map(
                        condition -> buildKvStr(condition.getFirstValue(), condition.getSecondValue())
                ).toList()
        );
    }

    public static String buildKvStr(FieldKey key, String val) {
        return mapField(key) + "=" + toSqlValStr(key, val);
    }

    public static String toSqlValStr(FieldKey key, String val) {
        return key.isNumType() ? val : "'" + val + "'";
    }

    public static List<RecordSet> execQuery(Connection conn, String sql) throws SQLException {
        try (var stmt = conn.createStatement()) {
            try (var result = stmt.executeQuery(sql)) {
                List<RecordSet> re = null;
                ResultSetMetaData metaData = null;
                int columnCount = 0;
                while (result.next()) {
                    if (re == null) {
                        re = new ArrayList<>();
                        metaData = result.getMetaData();
                        columnCount = metaData.getColumnCount();
                    }
                    var row = new RecordSet();
                    for (var i = 1; i <= columnCount; i++) {
                        row.put(SqlUtils.mapField(metaData.getColumnName(i)), result.getString(i));
                    }
                    row.readonly();
                    re.add(row);
                }
                return re == null ? Collections.emptyList() : Collections.unmodifiableList(re);
            }
        }
    }

    public static void execSql(Connection conn, String sql) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
}
