package com.github.pagehelper;

import com.github.pagehelper.pagesql.Dialect;
import com.github.pagehelper.pagesql.DialectSql;
import com.github.pagehelper.pagesql.PageSql;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.TextSqlNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liuzh
 */
public class MappedStatementUtil {


    private String BOUND_SQL = "boundSql.sql";
    private String SQL_NODES = "sqlSource.rootSqlNode.contents";
    public List<ResultMapping> EMPTY_RESULTMAPPING = new ArrayList<ResultMapping>(0);

    private PageSql pageSql;

    private Dialect dialect;

    public MappedStatementUtil(Dialect dialect) {
        this.dialect = dialect;
        pageSql = new PageSql(dialect);
    }


    /**
     * 获取Count的MappedStatement
     *
     * @param ms
     * @param boundSql
     * @return
     */
    public MappedStatement getCountMappedStatement(MappedStatement ms, BoundSql boundSql) {
        return getMappedStatement(ms, boundSql, DialectSql.SUFFIX_COUNT);
    }

    /**
     * 获取分页的MappedStatement
     *
     * @param ms
     * @param boundSql
     * @return
     */
    public MappedStatement getPageMappedStatement(MappedStatement ms, BoundSql boundSql) {
        return getMappedStatement(ms, boundSql, DialectSql.SUFFIX_PAGE);
    }

    /**
     * 获取ms - 在这里对新建的ms做了缓存，第一次新增，后面都会使用缓存值
     *
     * @param ms
     * @param boundSql
     * @param suffix
     * @return
     */
    private MappedStatement getMappedStatement(MappedStatement ms, BoundSql boundSql, String suffix) {
        MappedStatement qs = null;
        try {
            qs = ms.getConfiguration().getMappedStatement(ms.getId() + suffix);
        } catch (Exception e) {
            //ignore
        }
        if (qs == null) {
            //创建一个新的MappedStatement
            qs = newMappedStatement(ms, getNewSqlSource(ms, new BoundSqlSqlSource(boundSql), suffix), suffix);
            try {
                ms.getConfiguration().addMappedStatement(qs);
            } catch (Exception e) {
                //ignore
            }
        }
        return qs;
    }

    /**
     * 新建count查询和分页查询的MappedStatement
     *
     * @param ms
     * @param newSqlSource
     * @param suffix
     * @return
     */
    private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource, String suffix) {
        String id = ms.getId() + suffix;
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), id, newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
            StringBuilder keyProperties = new StringBuilder();
            for (String keyProperty : ms.getKeyProperties()) {
                keyProperties.append(keyProperty).append(",");
            }
            keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
            builder.keyProperty(keyProperties.toString());
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        if (suffix == DialectSql.SUFFIX_PAGE) {
            builder.resultMaps(ms.getResultMaps());
        } else {
            //count查询返回值int
            List<ResultMap> resultMaps = new ArrayList<ResultMap>();
            ResultMap resultMap = new ResultMap.Builder(ms.getConfiguration(), id, int.class, EMPTY_RESULTMAPPING).build();
            resultMaps.add(resultMap);
            builder.resultMaps(resultMaps);
        }
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());

        return builder.build();
    }

    /**
     * 获取新的sqlSource
     *
     * @param ms
     * @param newSqlSource
     * @param suffix
     * @return
     */
    private SqlSource getNewSqlSource(MappedStatement ms, BoundSqlSqlSource newSqlSource, String suffix) {
        SqlSource sqlSource = ms.getSqlSource();
        //从XMLLanguageDriver.java和XMLScriptBuilder.java可以看出只有两种SqlSource
        if (sqlSource instanceof DynamicSqlSource) {
            MetaObject msObject = SysObject.forObject(ms);
            List<SqlNode> contents = (List<SqlNode>) msObject.getValue(SQL_NODES);
            List<SqlNode> newSqlNodes = new ArrayList<SqlNode>(contents.size() + 2);
            //这里用的等号
            if (suffix == DialectSql.SUFFIX_PAGE) {
                newSqlNodes.add(new TextSqlNode(pageSql.getPageSqlBefore()));
                newSqlNodes.addAll(contents);
                newSqlNodes.add(new TextSqlNode(pageSql.getPageSqlAfter()));
                return new DynamicPageSqlSource(ms.getConfiguration(), new MixedSqlNode(newSqlNodes));
            } else {
                newSqlNodes.add(new TextSqlNode(pageSql.getCountSqlBefore()));
                newSqlNodes.addAll(contents);
                newSqlNodes.add(new TextSqlNode(pageSql.getCountSqlAfter()));
                return new DynamicSqlSource(ms.getConfiguration(), new MixedSqlNode(newSqlNodes));
            }
        } else {
            //RawSqlSource
            //这里用的等号
            if (suffix == DialectSql.SUFFIX_PAGE) {
                //改为分页sql
                MetaObject sqlObject = SysObject.forObject(newSqlSource);
                sqlObject.setValue(BOUND_SQL, pageSql.getPageSql((String) sqlObject.getValue(BOUND_SQL)));
                //添加参数映射
                List<ParameterMapping> newParameterMappings = new ArrayList<ParameterMapping>();
                newParameterMappings.addAll(newSqlSource.getBoundSql().getParameterMappings());
                newParameterMappings.add(new ParameterMapping.Builder(ms.getConfiguration(), DialectSql.PAGEPARAMETER_FIRST, Integer.class).build());
                newParameterMappings.add(new ParameterMapping.Builder(ms.getConfiguration(), DialectSql.PAGEPARAMETER_SECOND, Integer.class).build());
                sqlObject.setValue("boundSql.parameterMappings", newParameterMappings);
            } else {
                //改为count sql
                MetaObject sqlObject = SysObject.forObject(newSqlSource);
                sqlObject.setValue(BOUND_SQL, pageSql.getCountSql((String) sqlObject.getValue(BOUND_SQL)));
            }
            return newSqlSource;
        }
    }

    /**
     * 处理参数对象，添加分页参数值
     *
     * @param parameterObject 参数对象
     * @param page            分页信息
     * @return 返回带有分页信息的参数对象
     */
    public Map setPageParameter(Object parameterObject, BoundSql boundSql, Page page) {
        Map paramMap = null;
        if (parameterObject == null) {
            paramMap = new HashMap();
        } else if (parameterObject instanceof Map) {
            paramMap = (Map) parameterObject;
        } else {
            paramMap = new MapperMethod.ParamMap<Object>();
            if (boundSql.getParameterMappings() != null && boundSql.getParameterMappings().size() > 0) {
                for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
                    if (!parameterMapping.getProperty().equals(DialectSql.PAGEPARAMETER_FIRST)
                            && !parameterMapping.getProperty().equals(DialectSql.PAGEPARAMETER_SECOND)) {
                        paramMap.put(parameterMapping.getProperty(), parameterObject);
                    }
                }
            }
        }
        pageSql.setPageParameter(paramMap,page);
        return paramMap;
    }
}
