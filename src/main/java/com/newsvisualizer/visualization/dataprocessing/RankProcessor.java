package com.newsvisualizer.visualization.dataprocessing;

import com.newsvisualizer.visualization.beans.AccernData;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents the Rank processing engine.
 * Created by rahulkhanna on 17/12/16.
 */
public class RankProcessor {

    /**
     * This function is used to bin the articles based on their reporting time. Also ranking for auxiliary view.
     *
     * @param data
     * @return
     */
    public static Map<String, Object> populateRank(List<AccernData> data) {
        Map<String, List<AccernData>> articleByStoriesId = new HashMap<>();
        data.stream().forEach(article -> {
            if (articleByStoriesId.containsKey(article.getStory_id())) {
                articleByStoriesId.get(article.getStory_id()).add(article);
            } else {
                List<AccernData> articles = new ArrayList<>();
                articles.add(article);
                articleByStoriesId.put(article.getStory_id(), articles);
            }
        });
        Map<String, Map<Integer, List<AccernData>>> articlesByRankForAStory = new HashMap<>();
        List<AccernData> processedData = new ArrayList<AccernData>();
        List<String> sourceNameWhoWereAtLeastFastestOnce = new ArrayList<>();
        int i = 0;
        articleByStoriesId.forEach((s, datas) -> {
            Collections.sort(datas);
            AccernData fastestArticle = datas.get(0);
            sourceNameWhoWereAtLeastFastestOnce.add(fastestArticle.getSource_name());
            fastestArticle.setSourceScore(100);

        });
        Map<String, List<AccernData>> filteredStories = new HashMap<>();
        articleByStoriesId.forEach((s, datas) -> {
            List<AccernData> collect = datas.stream().filter(article -> sourceNameWhoWereAtLeastFastestOnce.contains(article.getSource_name())).collect(Collectors.toList());
            Collections.sort(collect);
            int k = 0;
            for (AccernData article : collect) {
                if (k == 0) {
                    article.setShapeAssigned("Circle");
                } else if (k >= 1 && k < 6) {
                    article.setShapeAssigned("Triangle");
                } else {
                    article.setShapeAssigned("Diamond");
                }
                k++;
            }
            filteredStories.put(s, collect);
            processedData.addAll(collect);
        });
        filteredStories.forEach((s, datas) -> {
            AccernData fastestArticle = datas.get(0);
            System.out.println("datas = " + datas.size());
            long timeRangeForThisStory = datas.get(datas.size() - 1).getHarvested_at().getTime() - fastestArticle.getHarvested_at().getTime();
            Map<Integer, Map<String, Date>> timeRangePerRank = generateTimeCutOffPerRank(timeRangeForThisStory, fastestArticle.getHarvested_at().getTime());
            Map<Integer, List<AccernData>> articlesPerRankForAStory = new HashMap<>();
            timeRangePerRank.forEach((rank, range) -> {
                System.out.println("Filtering articles for rank = " + rank);
                List<AccernData> dataForGivenRank = datas.stream().filter((article) -> article.getHarvested_at().after(range.get("startTime")) && article.getHarvested_at().before(range.get("endTime"))
                ).map(article -> {
                    article.setSourceRank(rank);
                    return article;
                }).collect(Collectors.toList());
                articlesPerRankForAStory.put(rank, dataForGivenRank);
            });
            articlesByRankForAStory.put(s, articlesPerRankForAStory);
        });

        Map<String, Map<Integer, Double>> ranksByMonth = sourceRankByMonth(processedData);
        Map<String, Object> returnedData = new HashMap<>();
        returnedData.put("mainViewData", processedData);
        returnedData.put("auxilaryData", ranksByMonth);
        return returnedData;
    }

    /**
     * This function is used to calculate the rank of source for every month.
     *
     * @param articlesByRankForAStory
     * @return
     */
    private static Map<String, Map<Integer, Double>> sourceRankByMonth(List<AccernData> articlesByRankForAStory) {
        Map<String, Map<Integer, Double>> rankOfSourcesByMonth = new HashMap<>();
        Map<String, Map<Integer, List<AccernData>>> articlesByMonthAndSource = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            int monthNeeded = i;
            List<AccernData> articlesByMonth = articlesByRankForAStory.stream().filter(article -> {
                Calendar cal = Calendar.getInstance();
                cal.setTime(article.getHarvested_at());
                return cal.get(Calendar.MONTH) == monthNeeded;
            }).collect(Collectors.toList());
            segregateMonthsDataBySource(articlesByMonth, articlesByMonthAndSource, monthNeeded);

        }
        for (Map.Entry<String, Map<Integer, List<AccernData>>> data : articlesByMonthAndSource.entrySet()) {
            String sourceName = data.getKey();
            data.getValue().forEach((month, articles) -> {
                int totalRank = 0;
                for (AccernData article : articles) {
                    totalRank += article.getSourceRank();
                }
                double avgSourceRank = totalRank / articles.size();
                if (rankOfSourcesByMonth.containsKey(sourceName)) {
                    Map<Integer, Double> ranks = rankOfSourcesByMonth.get(sourceName);
                    ranks.put(month, avgSourceRank);
                } else {
                    Map<Integer, Double> ranks = new HashMap<Integer, Double>();
                    ranks.put(month, avgSourceRank);
                    rankOfSourcesByMonth.put(sourceName, ranks);
                }
            });
        }
        return rankOfSourcesByMonth;
    }

    /**
     * This function is used to segregate the data for a source by month.
     *
     * @param articlesByMonth
     * @param articlesByMonthAndSource
     * @param monthNeeded
     */
    private static void segregateMonthsDataBySource(List<AccernData> articlesByMonth, Map<String, Map<Integer, List<AccernData>>> articlesByMonthAndSource, int monthNeeded) {
        for (AccernData article : articlesByMonth) {
            if (articlesByMonthAndSource.containsKey(article.getSource_name())) {
                Map<Integer, List<AccernData>> articlesOfASource = articlesByMonthAndSource.get(article.getSource_name());
                if (articlesOfASource.containsKey(monthNeeded)) {
                    List<AccernData> accernDatas = articlesOfASource.get(monthNeeded);
                    accernDatas.add(article);
                } else {
                    List<AccernData> accernDatas = new ArrayList<>();
                    accernDatas.add(article);
                    articlesOfASource.put(monthNeeded, accernDatas);
                }
            } else {
                Map<Integer, List<AccernData>> articlesOfASource = new HashMap<>();
                List<AccernData> accernDatas = new ArrayList<>();
                accernDatas.add(article);
                articlesOfASource.put(monthNeeded, accernDatas);
                articlesByMonthAndSource.put(article.getSource_name(), articlesOfASource);
            }
        }
    }

    /**
     * This function is used to generate logarithmic factors for ranking sources in the auxiliary view.
     *
     * @param timeRangeForThisStory
     * @param startTime
     * @return
     */
    private static Map<Integer, Map<String, Date>> generateTimeCutOffPerRank(long timeRangeForThisStory, long startTime) {
        Map<Integer, Map<String, Date>> timeRangePerRank = new HashMap<>(8);
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(startTime);
        for (int i = 8; i >= 1; i--) {
            Map<String, Date> dateRange = new HashMap<>();
            long currentStartTime = startTime;
            dateRange.put("startTime", start.getTime());
            double factor = (float) (1 / Math.pow(2.0, i));
            long cutoffTime = (long) (startTime + timeRangeForThisStory * factor);
            Calendar endTime = Calendar.getInstance();
            endTime.setTimeInMillis(cutoffTime);
            dateRange.put("endTime", endTime.getTime());
            startTime = endTime.getTimeInMillis();
            timeRangePerRank.put(i, dateRange);
        }
        return timeRangePerRank;
    }

}
