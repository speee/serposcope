/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.google;

import com.google.inject.Singleton;
import com.querydsl.core.QueryFlag;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLMergeClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.google.GoogleBest;
import com.serphacker.serposcope.models.google.GoogleRank;
import com.serphacker.serposcope.models.google.GoogleSearch;
import com.serphacker.serposcope.models.google.GoogleTarget;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleRankBest;
import com.serphacker.serposcope.querybuilder.QRun;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class GoogleRankDB extends AbstractDB {

    static QGoogleRank t_rank = QGoogleRank.googleRank;
    static QGoogleRankBest t_best = QGoogleRankBest.googleRankBest;
    static QRun t_run = QRun.run;

    public boolean insertBest(GoogleBest best){
        boolean inserted = false;
        try(Connection con = ds.getConnection()){
            inserted = new SQLMergeClause(con, dbTplConf, t_best)
                .set(t_best.groupId, best.getGroupId())
                .set(t_best.googleTargetId, best.getGoogleTargetId())
                .set(t_best.googleSearchId, best.getGoogleSearchId())
                .set(t_best.rank, best.getRank())
                .set(t_best.url, best.getUrl())
                .set(t_best.runDay, best.getRunDay() == null ? null : Timestamp.valueOf(best.getRunDay()))
                .execute() == 1;
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return inserted;
    }
    
    public GoogleBest createUnranked(int groupId, int googleTargetId, int googleSearchId) {
    	return new GoogleBest(groupId, googleTargetId, googleSearchId, (short)GoogleRank.UNRANKED, null, null);
    }
    public GoogleBest getBest(int groupId, int googleTargetId, int googleSearchId){
        GoogleBest best = null;
        
        try(Connection con = ds.getConnection()){
            Tuple tuple = new SQLQuery<Void>(con, dbTplConf)
                .select(t_best.all())
                .from(t_best)
                .where(t_best.groupId.eq(groupId))
                .where(t_best.googleTargetId.eq(googleTargetId))
                .where(t_best.googleSearchId.eq(googleSearchId))
                .fetchOne();
            
            if(tuple != null){
                best = new GoogleBest(
                    tuple.get(t_best.groupId),
                    tuple.get(t_best.googleTargetId),
                    tuple.get(t_best.googleSearchId),
                    tuple.get(t_best.rank),
                    tuple.get(t_best.runDay) != null ? tuple.get(t_best.runDay).toLocalDateTime() : null,
                    tuple.get(t_best.url)
                );
            } else {
                best = createUnranked(groupId, googleTargetId, googleSearchId);
            }
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return best;        
    }

    public Map<Integer, GoogleBest> getBestBySearch(int groupId, int googleTargetId, Collection<GoogleSearch> searches){
        List<Integer> searchIds = searches.stream().map(s -> s.getId()).collect(Collectors.toList());
        return getBestBySearch(groupId, googleTargetId, searchIds);
    }

    public Map<Integer, GoogleBest> getBestBySearch(int groupId, int googleTargetId, List<Integer> googleSearchIds){
        Map<Integer, GoogleBest> map = new LinkedHashMap<Integer, GoogleBest>();
    	try(Connection con = ds.getConnection()){
            List<Tuple> tuples = new SQLQuery<Void>(con, dbTplConf)
                .select(t_best.all())
                .from(t_best)
                .where(t_best.groupId.eq(groupId))
                .where(t_best.googleTargetId.eq(googleTargetId))
                .where(t_best.googleSearchId.in(googleSearchIds))
                .fetch();
            
            for (Tuple tuple : tuples) {
                map.put(tuple.get(t_best.googleSearchId), new GoogleBest(
                    tuple.get(t_best.groupId),
                    tuple.get(t_best.googleTargetId),
                    tuple.get(t_best.googleSearchId),
                    tuple.get(t_best.rank),
                    tuple.get(t_best.runDay) != null ? tuple.get(t_best.runDay).toLocalDateTime() : null,
                    tuple.get(t_best.url)));
            }
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    	return map;
    }

    public Map<Integer, GoogleBest> getBestByTarget(int groupId, Collection<GoogleTarget> targets, int googleSearchId){
        List<Integer> targetIds = targets.stream().map(t -> t.getId()).collect(Collectors.toList());
        return getBestByTarget(groupId, targetIds, googleSearchId);
    }

    public Map<Integer, GoogleBest> getBestByTarget(int groupId, List<Integer> googleTargetIds, int googleSearchId){
        Map<Integer, GoogleBest> map = new LinkedHashMap<Integer, GoogleBest>();
    	try(Connection con = ds.getConnection()){
            List<Tuple> tuples = new SQLQuery<Void>(con, dbTplConf)
                .select(t_best.all())
                .from(t_best)
                .where(t_best.groupId.eq(groupId))
                .where(t_best.googleTargetId.in(googleTargetIds))
                .where(t_best.googleSearchId.eq(googleSearchId))
                .fetch();
            
            for (Tuple tuple : tuples) {
                map.put(tuple.get(t_best.googleTargetId), new GoogleBest(
                    tuple.get(t_best.groupId),
                    tuple.get(t_best.googleTargetId),
                    tuple.get(t_best.googleSearchId),
                    tuple.get(t_best.rank),
                    tuple.get(t_best.runDay) != null ? tuple.get(t_best.runDay).toLocalDateTime() : null,
                    tuple.get(t_best.url)));
            }
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    	return map;
    }

    public boolean insert(GoogleRank rank) {
        if(dbTplConf.getTemplates().isNativeMerge()){
            return insertMerge(rank);
        } else {
            return insertOnDuplicateKey(rank);
        }
    }
    
    public boolean insert(Collection<GoogleRank> rank) {
        if(dbTplConf.getTemplates().isNativeMerge()){
            return insertMerge(rank);
        } else {
            return insertOnDuplicateKey(rank);
        }
    }    
    
    public boolean insertMerge(Collection<GoogleRank> ranks){
        try(Connection con = ds.getConnection()){
            SQLMergeClause clause = new SQLMergeClause(con, dbTplConf, t_rank);
            for (GoogleRank rank : ranks) {
                clause
                    .set(t_rank.runId, rank.runId)
                    .set(t_rank.groupId, rank.groupId)
                    .set(t_rank.googleTargetId, rank.googleTargetId)
                    .set(t_rank.googleSearchId, rank.googleSearchId)
                    .set(t_rank.rank, rank.rank)
                    .set(t_rank.previousRank, rank.previousRank)
                    .set(t_rank.diff, rank.diff)
                    .set(t_rank.url, rank.url)
                    .addBatch();
            }
            return clause.execute() > 0;
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return false;
    }
    
    public boolean insertOnDuplicateKey(Collection<GoogleRank> ranks){
        try(Connection con = ds.getConnection()){
            
            // waiting for patch https://github.com/querydsl/querydsl/issues/1921
            /*
            SQLInsertClause clause = new SQLInsertClause(con, dbTplConf, t_rank);
            clause.setBatchToBulk(true);
            for (GoogleRank rank : ranks) {
                clause
                    .set(t_rank.runId, rank.runId)
                    .set(t_rank.groupId, rank.groupId)
                    .set(t_rank.googleTargetId, rank.googleTargetId)
                    .set(t_rank.googleSearchId, rank.googleSearchId)
                    .set(t_rank.rank, rank.rank)
                    .set(t_rank.previousRank, rank.previousRank)
                    .set(t_rank.diff, rank.diff)
                    .set(t_rank.url, rank.url)
                    .addBatch();
            }
            clause.addFlag(QueryFlag.Position.END, 
                " on duplicate key update rank = values(rank) " +
                ", previous_rank = values(previous_rank)" + 
                ", diff = values(diff)" + 
                ", url = values(url)"
            );
            return clause.execute() > 0;
            */
            
            // 
            StringBuilder builder = new StringBuilder("INSERT INTO `GOOGLE_RANK` " + 
                "(`RUN_ID`, `GROUP_ID`, `GOOGLE_TARGET_ID`, `GOOGLE_SEARCH_ID`, `RANK`, `PREVIOUS_RANK`, `DIFF`, `URL`) " + 
                "VALUES ");
            for (GoogleRank rank : ranks) {
                builder.append("(");
                builder.append(rank.runId).append(',');
                builder.append(rank.groupId).append(',');
                builder.append(rank.googleTargetId).append(',');
                builder.append(rank.googleSearchId).append(',');
                builder.append(rank.rank).append(',');
                builder.append(rank.previousRank).append(',');
                builder.append(rank.diff).append(',');
                builder.append(dbTplConf.asLiteral(rank.url));
                builder.append("),");
            }
            builder.setCharAt(builder.length()-1, ' ');
            builder.append(" on duplicate key update rank = values(rank) " +
                ", previous_rank = values(previous_rank)" + 
                ", diff = values(diff)" + 
                ", url = values(url)"
            );
            try(Statement stmt = con.createStatement()){
                return stmt.executeUpdate(builder.toString()) > 0;
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return false;
    }    
    
    public boolean insertMerge(GoogleRank rank) {
        boolean inserted = false;
        try(Connection con = ds.getConnection()){
            inserted = new SQLMergeClause(con, dbTplConf, t_rank)
                .set(t_rank.runId, rank.runId)
                .set(t_rank.groupId, rank.groupId)
                .set(t_rank.googleTargetId, rank.googleTargetId)
                .set(t_rank.googleSearchId, rank.googleSearchId)
                .set(t_rank.rank, rank.rank)
                .set(t_rank.previousRank, rank.previousRank)
                .set(t_rank.diff, rank.diff)
                .set(t_rank.url, rank.url)
                .execute() == 1;
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return inserted;
    }    
    
    public boolean insertOnDuplicateKey(GoogleRank rank) {
        boolean inserted = false;
        try(Connection con = ds.getConnection()){
            inserted = new SQLInsertClause(con, dbTplConf, t_rank)
                .set(t_rank.runId, rank.runId)
                .set(t_rank.groupId, rank.groupId)
                .set(t_rank.googleTargetId, rank.googleTargetId)
                .set(t_rank.googleSearchId, rank.googleSearchId)
                .set(t_rank.rank, rank.rank)
                .set(t_rank.previousRank, rank.previousRank)
                .set(t_rank.diff, rank.diff)
                .set(t_rank.url, rank.url)
                .addFlag(QueryFlag.Position.END, 
                    " on duplicate key update rank = " + rank.rank + 
                    ", previous_rank = " + rank.previousRank + 
                    ", diff = " + rank.diff +
                    ", url = " + (rank.url == null ? "NULL" : dbTplConf.asLiteral(rank.url))
                )
                .execute() == 1;
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        return inserted;
    }        
    
    public int get(int runId, int groupId, int googleTargetId, int googleSearchId){
        Short rank = null;
        
        try(Connection con = ds.getConnection()){
            
            rank = new SQLQuery<Void>(con, dbTplConf)
                .select(t_rank.rank)
                .from(t_rank)
                .where(t_rank.runId.eq(runId))
                .where(t_rank.groupId.eq(groupId))
                .where(t_rank.googleTargetId.eq(googleTargetId))
                .where(t_rank.googleSearchId.eq(googleSearchId))
                .fetchOne();
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return rank != null ? rank : GoogleRank.UNRANKED;
    }

    public GoogleRank getFull(int runId, int groupId, int googleTargetId, int googleSearchId){
        GoogleRank rank = null;
        
        try(Connection con = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<Void>(con, dbTplConf)
                .select(t_rank.all())
                .from(t_rank)
                .where(t_rank.runId.eq(runId))
                .where(t_rank.groupId.eq(groupId))
                .where(t_rank.googleTargetId.eq(googleTargetId))
                .where(t_rank.googleSearchId.eq(googleSearchId))
                .fetchOne();
            
            rank = fromTuple(tuple);
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return rank;
    }
    
    public GoogleRank getDateRank(LocalDate date, int groupId, int googleTargetId, int googleSearchId){
        GoogleRank rank = null;
        
        try(Connection con = ds.getConnection()){
            
			SubQueryExpression<Integer> subQuery = SQLExpressions
					.select(t_rank.runId.max().as(t_rank.runId))
					.from(t_rank)					.innerJoin(t_run)
					.on(t_rank.groupId.eq(groupId).and(t_rank.googleTargetId.eq(googleTargetId))
							.and(t_rank.googleSearchId.eq(googleSearchId)).and(t_run.day.eq(Date.valueOf(date)))
							.and(t_rank.runId.eq(t_run.id)))
					.groupBy(t_run.day, t_rank.groupId, t_rank.googleTargetId, t_rank.googleSearchId);
            
			Tuple tuple = new SQLQuery<Void>(con, dbTplConf).select(t_rank.all()).from(t_rank)
					.where(t_rank.runId.in(subQuery)).where(t_rank.groupId.eq(groupId))
					.where(t_rank.googleTargetId.eq(googleTargetId)).where(t_rank.googleSearchId.eq(googleSearchId))
					.fetchOne();

			rank = fromTuple(tuple);
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return rank;
    }
    
    public List<GoogleRank> list0(int runStartId, int runEndId, int groupId, int targetId){
        List<GoogleRank> ranks = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
        	SQLQuery<Integer> query1 = new SQLQuery<Void>(con, dbTplConf)
					.select(t_run.id.max().as(t_run.id))
					.from(t_run)
					.innerJoin(t_rank)
					.on(t_run.id.between(runStartId, runEndId)
							.and(t_run.id.eq(t_rank.runId)).and(t_rank.googleTargetId.eq(targetId)))
					.groupBy(t_run.day);
        	List<Integer> runIds = query1.fetch();

        	SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_rank.all())
                .from(t_rank)
                .where(t_rank.runId.in(runIds))
                .where(t_rank.groupId.eq(groupId))
                .where(t_rank.googleTargetId.eq(targetId));
            List<Tuple> tuples = query.fetch();
            
            for (Tuple tuple : tuples) {
                ranks.add(fromTuple(tuple));
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return ranks;
    }    
    
    public List<GoogleRank> list(int runId, int groupId, int targetId){
        return list(Arrays.asList(runId), Arrays.asList(groupId), Arrays.asList(targetId), null);
    }
    
    public List<GoogleRank> list(
        Collection<Integer> runs, 
        Collection<Integer> groups, 
        Collection<Integer> targets,
        Collection<Integer> searches
    ){
        List<GoogleRank> ranks = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_rank.all())
                .from(t_rank);
            
            if(runs != null){
                query.where(t_rank.runId.in(runs));
            }
            
            if(groups != null){
                query.where(t_rank.groupId.in(groups));
            }
            
            if(targets != null){
                query.where(t_rank.googleTargetId.in(targets));
            }
            
            if(searches != null){
                query.where(t_rank.googleSearchId.in(searches));
            }            
            
            List<Tuple> tuples = query.fetch();
            
            for (Tuple tuple : tuples) {
                ranks.add(fromTuple(tuple));
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return ranks;
    }        
    
    public boolean deleteBySearch(int groupId, int googleSearchId){
        boolean deleted = false;
        
        try(Connection con = ds.getConnection()){
            
            deleted = new SQLDeleteClause(con, dbTplConf, t_best)
                .where(t_best.groupId.eq(groupId))
                .where(t_best.googleSearchId.eq(googleSearchId))
                .execute() > 0;             
            
            deleted &= new SQLDeleteClause(con, dbTplConf, t_rank)
                .where(t_rank.groupId.eq(groupId))
                .where(t_rank.googleSearchId.eq(googleSearchId))
                .execute() > 0;
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }        
        
        return deleted;
    }
    
    public boolean deleteByTarget(int groupId, int googleTargetId){
        boolean deleted = false;
        
        try(Connection con = ds.getConnection()){
            
            deleted = new SQLDeleteClause(con, dbTplConf, t_best)
                .where(t_best.groupId.eq(groupId))
                .where(t_best.googleTargetId.eq(googleTargetId))
                .execute() > 0;            
            
            deleted &= new SQLDeleteClause(con, dbTplConf, t_rank)
                .where(t_rank.groupId.eq(groupId))
                .where(t_rank.googleTargetId.eq(googleTargetId))
                .execute() > 0;
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }        
        
        return deleted;
    }    
    
    public void deleteByRunId(Integer runId){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_rank)
                .where(t_rank.runId.eq(runId))
                .execute();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    }        
    
    public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_best).execute();
            new SQLDeleteClause(con, dbTplConf, t_rank).execute();
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
    }    
    
    public static GoogleRank fromTuple(Tuple tuple){
        if(tuple == null){
            return null;
        }
        
        return new GoogleRank(
            tuple.get(t_rank.runId),
            tuple.get(t_rank.groupId),
            tuple.get(t_rank.googleTargetId),
            tuple.get(t_rank.googleSearchId),
            tuple.get(t_rank.rank),
            tuple.get(t_rank.previousRank),
            tuple.get(t_rank.url)
        );
    }
    
}
