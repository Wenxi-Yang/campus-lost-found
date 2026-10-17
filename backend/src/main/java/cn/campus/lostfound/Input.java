package cn.campus.lostfound;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

public class Input {

  public static String text(Map<String, Object> b, String key, int min, int max) {
    String v = Objects.toString(b.get(key), "").trim();
    if (v.length() < min || v.length() > max) throw ApiError.bad(
      key + " 长度应为 " + min + "~" + max
    );
    return v;
  }

  public static long number(Map<String, Object> b, String key) {
    try {
      long n = Long.parseLong(Objects.toString(b.get(key), ""));
      if (n <= 0) throw new Exception();
      return n;
    } catch (Exception e) {
      throw ApiError.bad(key + " 不是有效编号");
    }
  }

  public static Long optionalId(Map<String, Object> b, String key) {
    return b.get(key) == null || "".equals(b.get(key)) ? null : number(b, key);
  }

  public static boolean bool(Map<String, Object> b, String key) {
    if (!(b.get(key) instanceof Boolean)) throw ApiError.bad(key + " 应为布尔值");
    return (Boolean) b.get(key);
  }

  public static String choice(Map<String, Object> b, String key, String... allowed) {
    String v = text(b, key, 1, 30);
    if (!Arrays.asList(allowed).contains(v)) throw ApiError.bad(key + " 取值无效");
    return v;
  }

  public static Timestamp time(Map<String, Object> b, String key, boolean required) {
    String v = Objects.toString(b.get(key), "");
    if (v.isEmpty()) {
      if (required) throw ApiError.bad("请填写 " + key);
      return null;
    }
    try {
      return Timestamp.valueOf(LocalDateTime.parse(v.replace(' ', 'T')));
    } catch (Exception e) {
      throw ApiError.bad(key + " 时间格式不正确");
    }
  }
}
