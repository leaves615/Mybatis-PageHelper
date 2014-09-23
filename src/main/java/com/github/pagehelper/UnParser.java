package com.github.pagehelper;

import com.foundationdb.sql.StandardException;
import com.foundationdb.sql.parser.FromBaseTable;
import com.foundationdb.sql.parser.OrderByList;
import com.foundationdb.sql.parser.SQLParser;
import com.foundationdb.sql.parser.StatementNode;
import com.foundationdb.sql.unparser.NodeToString;
import com.github.pagehelper.pagesql.Dialect;

/**
 * @author liuzh
 */
public class UnParser  extends NodeToString {
    private static final SQLParser PARSER = new SQLParser();

    private Dialect dialect;

    public UnParser(Dialect dialect) {
        this.dialect = dialect;
    }

    public String removeOrderBy(String sql) throws StandardException {
        StatementNode stmt = PARSER.parseStatement(sql);
        String result = toString(stmt);
        if (result.indexOf('$') > -1) {
            result = result.replaceAll("\\$\\d+", "?");
        }
        return result;
    }

    @Override
    protected String orderByList(OrderByList node) throws StandardException {
        //order by中如果包含参数就原样返回
        // 这里建议order by使用${param}这样的参数
        // 这种形式的order by可以正确的被过滤掉，并且支持大部分的数据库
        String sql = nodeList(node);
        if (sql.indexOf('$') > -1) {
            return sql;
        }
        return "";
    }

    @Override
    protected String fromBaseTable(FromBaseTable node) throws StandardException {
        String tn = toString(node.getOrigTableName());
        String n = node.getCorrelationName();
        if (n == null) {
            return tn;
        } else if (dialect == Dialect.oracle) {
            //Oracle表不支持AS
            return tn + " " + n;
        } else {
            return tn + " AS " + n;
        }
    }
}