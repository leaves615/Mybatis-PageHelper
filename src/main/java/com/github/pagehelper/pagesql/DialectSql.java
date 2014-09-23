package com.github.pagehelper.pagesql;

import com.github.pagehelper.Page;

import java.util.Map;

/**
 * @author liuzh
 */
public interface DialectSql {
    //分页的id后缀
    String SUFFIX_PAGE = "_PageHelper";
    //count查询的id后缀
    String SUFFIX_COUNT = SUFFIX_PAGE + "_Count";
    //第一个分页参数
    String PAGEPARAMETER_FIRST = "First" + SUFFIX_PAGE;
    //第二个分页参数
    String PAGEPARAMETER_SECOND = "Second" + SUFFIX_PAGE;

    String getCountSql(String sql);

    String getCountSqlBefore();

    String getCountSqlAfter();

    String getPageSql(String sql);

    String getPageSqlBefore();

    String getPageSqlAfter();

    void setPageParameter(Map paramMap,Page page);
}
