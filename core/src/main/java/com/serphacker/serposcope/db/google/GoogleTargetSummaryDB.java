/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */
package com.serphacker.serposcope.db.google;

import com.google.inject.Singleton;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.SubQueryExpressionImpl;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLMergeClause;
import com.serphacker.serposcope.db.AbstractDB;
import com.serphacker.serposcope.models.google.GoogleTargetSummary;
import com.serphacker.serposcope.querybuilder.QGoogleRank;
import com.serphacker.serposcope.querybuilder.QGoogleTargetSummary;
import com.serphacker.serposcope.querybuilder.QRun;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class GoogleTargetSummaryDB extends AbstractDB {

    QGoogleTargetSummary t_summary = QGoogleTargetSummary.googleTargetSummary;
    QGoogleRank t_rank = QGoogleRank.googleRank;

    public int insert(Collection<GoogleTargetSummary> summaries){
        int inserted = 0;
        
        try(Connection con = ds.getConnection()){
            
            for (GoogleTargetSummary target : summaries) {
                inserted += new SQLMergeClause(con, dbTplConf, t_summary)
                    .set(t_summary.groupId, target.getGroupId())
                    .set(t_summary.googleTargetId, target.getTargetId())
                    .set(t_summary.runId, target.getRunId())
                    
                    .set(t_summary.totalTop3, target.getTotalTop3())
                    .set(t_summary.totalTop10, target.getTotalTop10())
                    .set(t_summary.totalTop100, target.getTotalTop100())
                    .set(t_summary.totalOut, target.getTotalOut())
                    
                    .set(t_summary.topRanks, target.getTopRanksSerialized())
                    .set(t_summary.topImprovements, target.getTopImprovementsSerialized())
                    .set(t_summary.topLosts, target.getTopLostsSerialized())
                    
                    .set(t_summary.scoreRaw, target.getScoreRaw())
                    .set(t_summary.scoreBasisPoint, target.getScoreBP())
                    .set(t_summary.previousScoreBasisPoint, target.getPreviousScoreBP())
                    
                    .execute();
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return inserted;
    }
    
    public boolean deleteByTarget(int targetId){
        boolean deleted = false;
        try(Connection con = ds.getConnection()){
            deleted = new SQLDeleteClause(con, dbTplConf, t_summary)
                .where(t_summary.googleTargetId.eq(targetId))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        return deleted;
    }
    
    public boolean deleteByRun(int runId){
        boolean deleted = false;
        try(Connection con = ds.getConnection()){
            deleted = new SQLDeleteClause(con, dbTplConf, t_summary)
                .where(t_summary.runId.eq(runId))
                .execute() == 1;
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        return deleted;
    }    
    
    public void wipe(){
        try(Connection con = ds.getConnection()){
            new SQLDeleteClause(con, dbTplConf, t_summary).execute();
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
    }
    
    public Map<Integer,Integer> getPreviousScore(int runId){
        Map<Integer,Integer> scores = new HashMap<>();
        
        try(Connection con = ds.getConnection()){
            List<Tuple> records = new SQLQuery<Void>(con, dbTplConf)
                .select(t_summary.googleTargetId, t_summary.scoreBasisPoint)
                .from(t_summary)
                .where(t_summary.runId.eq(runId))
                .fetch();
            
            for (Tuple record : records) {
                scores.put(record.get(t_summary.googleTargetId), record.get(t_summary.scoreBasisPoint));
            }
                
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        
        return scores;
    }
    
    public List<Integer> listScoreHistory(int groupId, int targetId, int history){
        List<Integer> scores = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            scores = new SQLQuery<Void>(con, dbTplConf)
                .select(t_summary.scoreBasisPoint)
                .from(t_summary)
                .where(t_summary.groupId.eq(groupId))
                .where(t_summary.googleTargetId.eq(targetId))
                .orderBy(t_summary.runId.desc())
                .limit(history)
                .fetch();
                
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        
        if(scores != null){
            Collections.reverse(scores);
        } else {
            scores = new ArrayList<>();
        }
        
        return scores;        
    }
    
    public Map<Integer, List<Integer>> listScoreHistories(int groupId, Collection<Integer> targetIds, int history){  
    	Map<Integer, List<Integer>> scores = new LinkedHashMap<>();
        try(Connection con = ds.getConnection()){
        	List<Tuple> tuples = new SQLQuery<Tuple>(con, dbTplConf)
                .select(t_summary.googleTargetId, t_summary.scoreBasisPoint)
                .from(t_summary)
                .where(t_summary.groupId.eq(groupId))
                .where(t_summary.googleTargetId.in(targetIds))
                .orderBy(t_summary.googleTargetId.asc(), t_summary.runId.asc())
                .fetch();
        	
        	for (Tuple tuple : tuples) {
        		Integer targetId = tuple.get(t_summary.googleTargetId);
        		if (!scores.containsKey(targetId)) {
        			scores.put(targetId, new LinkedList<>());
        		}
        		List<Integer> targetScores = scores.get(targetId);
        		if (targetScores.size() < history) {
        			targetScores.add(tuple.get(t_summary.scoreBasisPoint));        			
        		}
        	}

        	for (List<Integer> targetScores : scores.values()) {
        		for (int i = targetScores.size(); i < history; i++) {
        			targetScores.add(0,0);
        		}
        	}
        }catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        
        return scores;        
    }

    public List<GoogleTargetSummary> list(int runId){
        return list(runId, false);
    }
  
    public List<GoogleTargetSummary> list(int runId, boolean skipTop){
        List<GoogleTargetSummary> summaries = new ArrayList<>();
        Map<Integer, GoogleTargetSummary> summaryMap = new LinkedHashMap<>();
        try(Connection con = ds.getConnection()){
            
			SubQueryExpression<? extends Tuple> subQuery = SQLExpressions
					.select(t_summary.googleTargetId, t_summary.runId.max().as(t_summary.runId)).from(t_summary)
					.where(t_summary.runId.loe(runId)).groupBy(t_summary.googleTargetId);

			List<Tuple> summaryTuples = new SQLQuery<Void>(con, dbTplConf).select(t_summary.all()).from(t_summary)
					.where(Expressions.list(t_summary.googleTargetId, t_summary.runId).in(subQuery)).fetch();

            for (Tuple tuple : summaryTuples) {
                
                GoogleTargetSummary summary = new GoogleTargetSummary(
                    tuple.get(t_summary.groupId),
                    tuple.get(t_summary.googleTargetId),
                    tuple.get(t_summary.runId),
                    tuple.get(t_summary.previousScoreBasisPoint)
                );
                
                summary.setTotalTop3(tuple.get(t_summary.totalTop3));
                summary.setTotalTop10(tuple.get(t_summary.totalTop10));
                summary.setTotalTop100(tuple.get(t_summary.totalTop100));
                summary.setTotalOut(tuple.get(t_summary.totalOut));
                
                summary.setScoreRaw(tuple.get(t_summary.scoreRaw));
                summary.setScoreBP(tuple.get(t_summary.scoreBasisPoint));
                
                if(!skipTop){
                    List<Integer> topRanksIds = unserializeIds(tuple.get(t_summary.topRanks));
                    List<Integer> topImprovementsIds = unserializeIds(tuple.get(t_summary.topImprovements));
                    List<Integer> topLostsIds = unserializeIds(tuple.get(t_summary.topLosts));

                    Set<Integer> searchIds = new HashSet<>();
                    searchIds.addAll(topRanksIds);
                    searchIds.addAll(topImprovementsIds);
                    searchIds.addAll(topLostsIds);

                    List<Tuple> rankTuples = new SQLQuery<Void>(con, dbTplConf)
                        .select(t_rank.all())
                        .from(t_rank)
                        .where(t_rank.runId.eq(runId))
                        .where(t_rank.groupId.eq(summary.getGroupId()))
                        .where(t_rank.googleTargetId.eq(summary.getTargetId()))
                        .where(t_rank.googleSearchId.in(searchIds))
                        .fetch();


                    for (Tuple rankTuple : rankTuples) {
                        summary.addRankToTop(GoogleRankDB.fromTuple(rankTuple));
                    }
                }
                
                if (!summaryMap.containsKey(summary.getTargetId())) {
                	summaryMap.put(summary.getTargetId(), summary);
                }
            }
            summaries.addAll(summaryMap.values());
            
        } catch(Exception ex){
            LOG.error("SQLError", ex);
        }
        return summaries;
    }
    
    protected List<Integer> unserializeIds(String str){
        if(str == null || str.isEmpty()){
            return Collections.EMPTY_LIST;
        }
        
        List<Integer> ids = new ArrayList<>();
        
        String[] splits = str.split(",");
        for (String split : splits) {
            if(split.length() > 0){
                try {ids.add(Integer.parseInt(split));}catch(NumberFormatException ex){}
            }
        }
        
        return ids;
    }
    
    
    /*
    public List<GoogleTarget> list(Collection<Integer> groups){
        List<GoogleTarget> targets = new ArrayList<>();
        
        try(Connection con = ds.getConnection()){
            
            SQLQuery<Tuple> query = new SQLQuery<Void>(con, dbTplConf)
                .select(t_target.all())
                .from(t_target);
            
            if(groups != null){
                query.where(t_target.groupId.in(groups));
            }
            
            List<Tuple> tuples = query.fetch();
            
            if(tuples != null){
                for (Tuple tuple : tuples) {
                    targets.add(fromTuple(tuple));
                }
            }
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return targets;
    }
    
    public GoogleTarget get(int targetId){
        GoogleTarget target = null;
        
        try(Connection con = ds.getConnection()){
            
            Tuple tuple = new SQLQuery<Void>(con, dbTplConf)
                .select(t_target.all())
                .from(t_target)
                .where(t_target.id.eq(targetId))
                .fetchOne();
            
            target = fromTuple(tuple);
            
        } catch(Exception ex){
            LOG.error("SQL error", ex);
        }
        
        return target;
    }    
    
    GoogleTarget fromTuple(Tuple tuple) throws Exception{
        return new GoogleTarget(
            tuple.get(t_target.id),
            tuple.get(t_target.groupId),
            tuple.get(t_target.name),
            tuple.get(t_target.patternType) == null ? PatternType.REGEX : GoogleTarget.PatternType.values()[tuple.get(t_target.patternType)],
            tuple.get(t_target.pattern)
        );
    }
    */
}
