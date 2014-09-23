package com.github.pagehelper.pagesql.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.UnParser;
import com.github.pagehelper.pagesql.Dialect;
import com.github.pagehelper.pagesql.DialectSql;

import java.util.Map;

/**
 * @author liuzh
 */
public class HsqldbPageSql implements DialectSql {
    //SQL反解析
    private UnParser UNPARSER;

    public HsqldbPageSql(Dialect dialect) {
        UNPARSER = new UnParser(dialect);
    }

    /**
     * 获取总数sql - 如果要支持其他数据库，修改这里就可以
     *
     * @param sql 原查询sql
     * @return 返回count查询sql
     */
    @Override
    public String getCountSql(final String sql) {
        try {
            if (sql.toUpperCase().contains("ORDER")) {
                return getCountSqlBefore() + UNPARSER.removeOrderBy(sql) + getCountSqlAfter();
            }
        } catch (Exception e) {
            //ignore
        }
        return getCountSqlBefore() + sql + getCountSqlAfter();
    }

    /**
     * 获取count前置sql
     *
     * @return
     */
    @Override
    public String getCountSqlBefore() {
        return "select count(0) from (";
    }

    /**
     * 获取count后置sql
     *
     * @return
     */
    @Override
    public String getCountSqlAfter() {
        return ") tmp_count";
    }

    /**
     * 获取分页sql - 如果要支持其他数据库，修改这里就可以
     *
     * @param sql 原查询sql
     * @return 返回分页sql
     */
    @Override
    public String getPageSql(String sql) {
        return getPageSqlBefore() + sql + getPageSqlAfter();
    }

    /**
     * 获取分页前置sql
     *
     * @return
     */
    @Override
    public String getPageSqlBefore() {
        return "";
    }

    /**
     * 获取分页后置sql
     *
     * @return
     */
    @Override
    public String getPageSqlAfter() {
        return " LIMIT ? OFFSET ?";
    }

    /**
     * 设置分页参数
     * @param paramMap
     * @param page
     */
    @Override
    public void setPageParameter(Map paramMap, Page page) {
        paramMap.put(PAGEPARAMETER_FIRST, page.getPageSize());
        paramMap.put(PAGEPARAMETER_SECOND, page.getStartRow());
    }
}
