package com.etm.common;

import lombok.Data;

/**
 * 统一响应结构：{ code, msg, data }
 * code = 200 成功，其余为失败
 */
@Data
public class Result<T> {

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "操作成功";
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String msg) {
        return fail(400, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }

    public static <T> Result<T> warn(T data) {
        Result<T> warn = new Result<>();
        warn.code = 201;
        warn.msg = "已处理，但需要确认";
        warn.data = data;
        return warn;
    }
    public static <T> Result<T> warn() {
        return warn(null);
    }
}
