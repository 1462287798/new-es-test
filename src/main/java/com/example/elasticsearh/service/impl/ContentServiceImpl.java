package com.example.elasticsearh.service.impl;


import com.example.elasticsearh.domain.Content;
import com.example.elasticsearh.service.ContentService;
import com.example.elasticsearh.util.HtmlParseUtil;
import com.google.gson.Gson;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ContentServiceImpl implements ContentService {
    @Autowired
    private RestHighLevelClient elasticsearchClient;

    //利用刚才写的工具将爬出来的内容写入ES
    public boolean parseContent(String keyword) throws IOException {
        List<Content> contents = new HtmlParseUtil().parseJD(keyword);

        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("2m");

        Gson gson = new Gson();
        for (Content content : contents) {
            String good = gson.toJson(content); //将集合中的每一个元素（对象）变成JSON字符串
            bulkRequest.add(new IndexRequest("jd_goods").source(good, XContentType.JSON));
        }

        System.out.println("============================" + elasticsearchClient);
        BulkResponse bulk = elasticsearchClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        return !bulk.hasFailures();
    }

    SearchRequest searchRequest = new SearchRequest("jd_goods");
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

    //2、获取这些数据实现搜索功能
    public List<Map<String, Object>> searchPage(String keyword, int pageNo, int pageSize) throws IOException {
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);

        QueryBuilder title = QueryBuilders.matchQuery("title", keyword);
        sourceBuilder.query(title);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse
                = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit searchHit : searchResponse.getHits().getHits())
            list.add(searchHit.getSourceAsMap());
        list.forEach(System.out::println);
        return list;
    }


    //3、获取这些数据实现高亮功能
    public List<Map<String, Object>> searchPageHighlightBuilder(String keyword, int pageNo, int pageSize) throws IOException {
        if (pageNo <= 1) {
            pageNo = 1;
        }
        sourceBuilder.from(pageNo);
        sourceBuilder.size(pageSize);
        QueryBuilder title
                = QueryBuilders.matchQuery("title", keyword);
        sourceBuilder.query(title);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //HighlightBuilder用于ES中高亮条件的构建
        highlightBuilder.field("title"); //让查询中title字段中的内容高亮
        highlightBuilder.requireFieldMatch(false); //多个字段高亮显示
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);

        //执行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse search = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);

        //解析结果
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        for (SearchHit hit : search.getHits().getHits()) {
            //解析高亮的字段
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            //highlightFields这个映射里放的是所有高亮的字段
            HighlightField title1 = highlightFields.get("title");


            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            //System.err.println("sourceAsMap：" + sourceAsMap);
            //解析高亮的字段
            if (title1 != null) {
                Text[] fragments = title1.fragments();
                String n_title = "";
                for (Text text : fragments) {
                    n_title += text;
                }
                sourceAsMap.put("title", n_title);
            }
            list.add(sourceAsMap);
        }
        return list;
    }

}
