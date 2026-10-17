package cn.campus.lostfound;

import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.*;
import org.springframework.stereotype.Component;

@Component
public class Store {

  final JdbcTemplate db;

  public Store(JdbcTemplate db) {
    this.db = db;
  }

  public Map<String, Object> one(String sql, Object... args) {
    List<Map<String, Object>> rows = db.queryForList(sql, args);
    if (rows.isEmpty()) throw new ApiError.Failure(404, "记录不存在");
    return rows.get(0);
  }

  // 明确只请求id，兼容H2同时返回默认时间字段的生成键行为。
  public long insert(String sql, Object... args) {
    KeyHolder keys = new GeneratedKeyHolder();
    db.update(
      c -> {
        PreparedStatement p = c.prepareStatement(sql, new String[] { "id" });
        for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
        return p;
      },
      keys
    );
    return keys.getKey().longValue();
  }

  public static long id(Map<String, Object> r, String key) {
    return ((Number) r.get(key)).longValue();
  }

  public static boolean yes(Object v) {
    return Boolean.TRUE.equals(v) || (v instanceof Number && ((Number) v).intValue() != 0);
  }
}
