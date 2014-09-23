package com.github.pagehelper.pagesql;

import com.github.pagehelper.Page;
import com.github.pagehelper.pagesql.impl.HsqldbPageSql;
import com.github.pagehelper.pagesql.impl.MysqlPageSql;
import com.github.pagehelper.pagesql.impl.OraclePageSql;

import java.util.Map;

/**
 * @author liuzh
 */
public class PageSql implements DialectSql {
    private DialectSql delegate;

    public PageSql(Dialect dialect) {
        switch (dialect) {
            case mysql:
                delegate = new MysqlPageSql(dialect);
                break;
            case oracle:
                delegate = new OraclePageSql(dialect);
                break;
            case hsqldb:
            default:
                delegate = new HsqldbPageSql(dialect);

        }
    }

    /**
     * 获取总数sql - 如果要支持其他数据库，修改这里就可以
     *
     * @param sql 原查询sql
     * @return 返回count查询sql
     */
    @Override
    public String getCountSql(final String sql) {
        return delegate.getCountSql(sql);
    }

    /**
     * 获取count前置sql
     *
     * @return
     */
    @Override
    public String getCountSqlBefore() {
        return delegate.getCountSqlBefore();
    }

    /**
     * 获取count后置sql
     *
     * @return
     */
    @Override
    public String getCountSqlAfter() {
        return delegate.getCountSqlAfter();
    }

    /**
     * 获取分页sql - 如果要支持其他数据库，修改这里就可以
     *
     * @param sql 原查询sql
     * @return 返回分页sql
     */
    @Override
    public String getPageSql(String sql) {
        return delegate.getPageSql(sql);
    }

    /**
     * 获取分页前置sql
     *
     * @return
     */
    @Override
    public String getPageSqlBefore() {
        return delegate.getPageSqlBefore();
    }

    /**
     * 获取分页后置sql
     *
     * @return
     */
    @Override
    public String getPageSqlAfter() {
        return delegate.getPageSqlAfter();
    }

    /**
     * 设置分页参数
     *
     * @param paramMap
     * @param page
     */
    @Override
    public void setPageParameter(Map paramMap, Page page) {
        delegate.setPageParameter(paramMap, page);
    }
}
