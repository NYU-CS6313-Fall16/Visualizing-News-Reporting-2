package com.newsvisualizer.visualization.predataoperations;

import com.mongodb.*;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a one time script which cleans the data with invalid entries and then saves it in MongoDb. This script is also responsible for extracting the name of source from the provided URL.
 * <p>
 * Created by rahulkhanna on 04/12/16.
 */
public class DataEnricher {

    private static final String SEPARATOR = ";";
    private static final Map<String, Integer> columnIndex;

    static {
        columnIndex = new HashMap<>();
        columnIndex.put("article_id", 0);
        columnIndex.put("story_id", 1);
        columnIndex.put("harvested_at", 2);
        columnIndex.put("entity_name", 3);
        columnIndex.put("entity_sector", 9);
        columnIndex.put(("story_name"), 5);
        columnIndex.put("story_volume", 33);
        columnIndex.put("article_url", 52);
        columnIndex.put("overall_source_rank", 37);
    }

    private final Reader source;

    public DataEnricher(Reader source) {
        this.source = source;
    }

    /**
     * This function is used to read the data from the provided CVS file.
     *
     * @return
     */
    private List<List<String>> readData() {
        List<List<String>> collect = null;
        try (BufferedReader reader = new BufferedReader(source)) {
            collect = reader.lines().skip(1).filter(line -> {
                List<String> data = Arrays.asList(line.split(","));
                if (data == null || data.size() == 0 || data.get(columnIndex.get("article_id")) == null ||
                        data.get(columnIndex.get("story_id")) == null ||
                        data.get(columnIndex.get("story_id")).isEmpty() ||
                        data.get(columnIndex.get("harvested_at")) == null ||
                        data.get(columnIndex.get("harvested_at")).isEmpty() ||
                        data.get(columnIndex.get("entity_name")) == null ||
                        data.get(columnIndex.get("entity_name")).isEmpty() ||
                        data.get(columnIndex.get("entity_sector")).isEmpty() ||
                        data.get(columnIndex.get("story_name")) == null ||
                        data.get(columnIndex.get("story_volume")) == null ||
                        data.get(columnIndex.get("story_volume")).isEmpty() ||
                        data.get(columnIndex.get("article_url")) == null ||
                        data.get(columnIndex.get("article_url")).isEmpty() ||
                        data.get(columnIndex.get("overall_source_rank")) == null ||
                        data.get(columnIndex.get("overall_source_rank")).isEmpty()) {
                    return false;
                } else {
                    return true;
                }
            }).map((line) -> {
                List<String> data = Arrays.asList(line.split(","));
                List<String> selectedData = new ArrayList<>();
                selectedData.add(data.get(columnIndex.get("article_id")));
                selectedData.add(data.get(columnIndex.get("story_id")));
                selectedData.add(data.get(columnIndex.get("harvested_at")));
                selectedData.add(data.get(columnIndex.get("entity_name")));
                selectedData.add(data.get(columnIndex.get("entity_sector")));
                selectedData.add(data.get(columnIndex.get("story_name")));
                selectedData.add(data.get(columnIndex.get("story_volume")));
                String article_url = data.get(columnIndex.get("article_url"));
                String article_hostname = extractSourceNameFromURL(article_url);
                selectedData.add(article_hostname);
                selectedData.add(data.get(columnIndex.get("overall_source_rank")));
                return selectedData;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return collect;
    }

    /**
     * This function is used to extract the name of the source from the article's URL.
     *
     * @param article_url url of the article.
     * @return SourceName
     */
    private String extractSourceNameFromURL(String article_url) {
        int firstSlashIndex = article_url.indexOf("//") + 2;
        article_url = article_url.substring(firstSlashIndex);
        int indexOf2ndSlash = article_url.indexOf("/");
        String hostName = article_url;
        if (indexOf2ndSlash > 0) {
            hostName = article_url.substring(0, indexOf2ndSlash);
        }
//        System.out.println("hostName = " + hostName);
        if (hostName.startsWith("www")) {
            hostName = hostName.substring(4);
        }
        if (hostName.endsWith("com") || hostName.endsWith("org") || hostName.endsWith("net")) {
//            System.out.println("ends with com");
            hostName = hostName.substring(0, hostName.length() - 4);
        } else if (hostName.contains("co") && hostName.indexOf("co") > 0) {
            hostName = hostName.substring(0, hostName.indexOf("co") - 1);
        }

        return hostName;
    }

    /**
     * This function is used to write the processed data into another CSV file.
     *
     * @param inputData
     * @param fileName
     */
    private void writeData(List<List<String>> inputData, String fileName) {
        try {
            FileWriter fileWriter = new FileWriter(fileName);
            String header = "article_id,story_id,harvested_at,entity_name,entity_ticker,entity_sector,article_sentiment,story_name,story_sentiment,story_volume,event_author_rank,article_url,overall_source_rank\n";
            fileWriter.append(header);
            for (List<String> data : inputData) {
                fileWriter.append(data.toString());
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This function is used to save the data in MongoDb.
     *
     * @param dataToSave
     */
    private void saveDataInDB(List<List<String>> dataToSave) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            DBCollection collection = getDbCollection();
            List<DBObject> dbObj = new ArrayList<>();
            dataToSave.stream().forEach(d -> {
                DBObject obj = new BasicDBObject();
                obj.put("article_id", d.get(0));
                obj.put("story_id", d.get(1));
                try {
                    obj.put("harvested_at", format.parse(d.get(2)));
                } catch (ParseException e) {
                    System.out.println(e.getMessage());
                }
                obj.put("entity_name", d.get(3));
                obj.put("entity_sector", d.get(4));
                obj.put("story_name", d.get(5));
                obj.put("story_volume", Integer.parseInt(d.get(6)));
                obj.put("source_name", d.get(7));
                obj.put("overall_source_rank", Integer.parseInt(d.get(8)));
                collection.insert(obj);
            });


        } catch (UnknownHostException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * This function is used get the connection to DB.
     *
     * @return connection to DB.
     * @throws UnknownHostException
     */
    private DBCollection getDbCollection() throws UnknownHostException {
        Mongo client = new MongoClient("127.0.0.1");
        DB db = client.getDB("iv");
        return db.getCollection("data");
    }

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("data.csv");
        Reader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"));
        DataEnricher enricher = new DataEnricher(reader);
        List<List<String>> cleanData = enricher.readData();
//        enricher.writeData(cleanData, "cleandata.csv");
        enricher.saveDataInDB(cleanData);
//        enricher.getDbCollection().drop();

        System.out.println(enricher.getDbCollection().count());

    }
}
