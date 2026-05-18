package com.smartdx.core.result;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * ページングレスポンス構造体
 *
 * @param <T> データ型
 */
public class PageResult<T> implements Serializable {

    private String code;
    private String msg;
    private PageData<T> data;

    /**
     * ページングデータからレスポンスを構築
     */
    public static <T> PageResult<T> success(List<T> list, long total) {
        PageResult<T> result = new PageResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());

        PageData<T> pageData = new PageData<>();
        pageData.setList(list != null ? list : Collections.emptyList());
        pageData.setTotal(total);
        result.setData(pageData);

        return result;
    }

    /**
     * リストデータからレスポンスを構築（ページング無し）
     */
    public static <T> PageResult<T> success(List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMsg(ResultCode.SUCCESS.getMsg());

        PageData<T> pageData = new PageData<>();
        pageData.setList(list != null ? list : Collections.emptyList());
        pageData.setTotal(list != null ? list.size() : 0L);
        result.setData(pageData);

        return result;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public PageData<T> getData() {
        return data;
    }

    public void setData(PageData<T> data) {
        this.data = data;
    }

    public static class PageData<T> implements Serializable {
        private List<T> list;
        private long total;

        public List<T> getList() {
            return list;
        }

        public void setList(List<T> list) {
            this.list = list;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }
    }
}
